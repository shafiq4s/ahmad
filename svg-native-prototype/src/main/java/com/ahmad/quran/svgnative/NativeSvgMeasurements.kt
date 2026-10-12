package com.ahmad.quran.svgnative

import com.ahmad.quran.mushaf.svg.NativeSvgPageView

import com.ahmad.quran.mushaf.svg.NativeSvgPageLoader

import com.ahmad.quran.mushaf.svg.NativeSvgPage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.os.Build
import android.os.Debug
import android.view.View
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/** Trial-only CPU/heap measurements. Results always identify the execution environment. */
object NativeSvgMeasurements {
    fun run(context: Context, environment: String, ui: (() -> Unit) -> Unit = { it() }): JSONObject {
        lateinit var view: NativeSvgPageView
        ui {
            view = NativeSvgPageView(context)
            view.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1722,View.MeasureSpec.EXACTLY))
            view.layout(0,0,1080,1722)
        }
        val bitmap = Bitmap.createBitmap(1080,1722,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val result = JSONObject().put("environment",environment).put("api",Build.VERSION.SDK_INT)
            .put("viewportPixels",JSONArray(listOf(1080,1722))).put("renderSurfaceBytes",bitmap.allocationByteCount)
            .put("nativeGraphics",environment.startsWith("Android emulator") || Build.VERSION.SDK_INT>=26)
        val coldStart = System.nanoTime()
        NativeSvgPageLoader(context, 1, "native-svg").use { loader ->
            val load = System.nanoTime()
            val page = loader.load(1)
            val loadNanos = System.nanoTime()-load
            ui { view.display(page);bitmap.eraseColor(Color.WHITE);view.draw(canvas) }
            result.put("firstContentColdMs",ms(System.nanoTime()-coldStart))
                .put("firstPageLoadMs",ms(loadNanos)).put("firstParseMs",ms(page.parseNanos))
                .put("firstRenderMs",ms(view.lastRenderNanos))
        }
        ui { view.release() }
        val modes = JSONArray()
        for (capacity in listOf(1,3)) {
            val loads = mutableListOf<Double>()
            val renders = mutableListOf<Double>()
            val switches = mutableListOf<Double>()
            val rows = JSONArray()
            NativeSvgPageLoader(context, capacity, "native-svg").use { loader ->
                // Warm process/JIT first; explicit cache misses and hits remain separately labelled.
                for(number in loader.pages) {
                    val loaded = loader.load(number)
                    ui { view.display(loaded);bitmap.eraseColor(Color.WHITE);view.draw(canvas) }
                }
                loader.cache.evictAll()
                for(number in listOf(1,2,48,121,604,121,48,2,1,2,48,121,604,121,48)) {
                    ui { view.release() }
                    val hit = loader.cache.snapshot().containsKey(number)
                    val start = System.nanoTime()
                    val loaded = loader.load(number)
                    val loadMs = ms(System.nanoTime()-start)
                    ui { view.display(loaded);bitmap.eraseColor(Color.WHITE);view.draw(canvas) }
                    val switchMs = ms(System.nanoTime()-start)
                    loads.add(loadMs);renders.add(ms(view.lastRenderNanos));switches.add(switchMs)
                    rows.put(JSONObject().put("page",number).put("cacheHit",hit).put("loadMs",loadMs)
                        .put("parseMs",if(hit)0.0 else ms(loaded.parseNanos)).put("renderMs",ms(view.lastRenderNanos))
                        .put("contentReadyMs",switchMs))
                    if(capacity==3)loader.prefetchNeighbours(number)
                }
            }
            ui { view.release() }
            modes.put(JSONObject().put("cacheCapacity",capacity).put("loadMedianMs",median(loads))
                .put("renderMedianMs",median(renders)).put("switchMedianMs",median(switches))
                .put("switchP95Ms",percentile(switches,.95)).put("samples",rows)
                .put("memory",measureMemory(context,capacity,view,canvas,ui)))
        }
        result.put("cacheModes",modes)
        val parses = JSONArray()
        NativeSvgPageLoader(context, 1, "native-svg").use { loader ->
            for(number in loader.pages) {
                val page = loader.load(number)
                ui { view.display(page);bitmap.eraseColor(Color.WHITE);view.draw(canvas) }
                parses.put(JSONObject().put("page",number).put("parseMs",ms(page.parseNanos)).put("renderMs",ms(view.lastRenderNanos)))
            }
            val hits = mutableListOf<Double>()
            ui {
                val id = view.page!!.geometry.regions.last().ayahId
                val marker = JSONArray(context.assets.open("native-svg/604.json").bufferedReader().use { it.readText() })
                    .let { it.getJSONObject(it.length()-1) }
                val point = view.screenPoint(com.ahmad.quran.mushaf.svg.PagePoint(marker.getDouble("x").toFloat(),marker.getDouble("y").toFloat()))
                repeat(1000) { check(view.hitTest(point.x,point.y)==id);hits.add(ms(view.lastHitTestNanos)) }
            }
            result.put("hitTestingMedianMs",median(hits)).put("hitTestingP95Ms",percentile(hits,.95))
            // PNG is an in-memory measurement baseline only: no WebP or raster app assets.
            val encoded = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }.toByteArray()
            ui { view.release() }
            loader.cache.evictAll()
            val decodes = mutableListOf<Double>()
            val draws = mutableListOf<Double>()
            repeat(15) {
                val start = System.nanoTime()
                val raster = checkNotNull(BitmapFactory.decodeByteArray(encoded,0,encoded.size))
                decodes.add(ms(System.nanoTime()-start))
                ui {
                    val drawStart = System.nanoTime()
                    canvas.drawBitmap(raster,null,RectF(0f,0f,1080f,1722f),null)
                    draws.add(ms(System.nanoTime()-drawStart))
                }
                raster.recycle()
            }
            result.put("rasterBaseline",JSONObject().put("format","PNG, temporary in-memory native capture")
                .put("decodeMedianMs",median(decodes)).put("drawMedianMs",median(draws))
                .put("decodedBitmapBytes",1080*1722*4).put("encodedBytes",encoded.size))
        }
        ui { view.release() }
        bitmap.recycle()
        result.put("pageParseAndRender",parses)
        return result
    }

    private fun measureMemory(context: Context,capacity: Int,view: NativeSvgPageView,canvas: Canvas,
                              ui: (() -> Unit) -> Unit): JSONObject {
        gc()
        val baseline = memory()
        val peak = AtomicLong(baseline)
        val sampling = AtomicBoolean(true)
        val sampler = Thread {
            while(sampling.get()) {
                val value = memory()
                // Single sampler writer; updateAndGet is unavailable on Android 6.
                peak.set(maxOf(peak.get(),value))
                Thread.sleep(5)
            }
        }.apply { isDaemon=true;start() }
        var retained = 0L
        try {
            NativeSvgPageLoader(context, capacity, "native-svg").use { loader ->
                val loaded = loader.load(48)
                ui { view.display(loaded);view.draw(canvas) }
                if(capacity==3)loader.prefetchNeighbours(48)
                gc()
                retained = memory()
                ui { view.release() }
            }
        } finally { sampling.set(false);sampler.join() }
        val observedPeak = maxOf(peak.get(),retained)
        return JSONObject().put("baselineManagedPlusNativeBytes",baseline)
            .put("retainedManagedPlusNativeBytes",retained)
            .put("retainedDeltaBytes",retained-baseline).put("observedPeakBytes",observedPeak)
            .put("observedPeakDeltaBytes",observedPeak-baseline).put("sampleIntervalMs",5)
            .put("scope","Runtime managed heap + Debug native malloc heap; rendering surface already in baseline; not PSS")
    }
    private fun memory(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory()-runtime.freeMemory()+Debug.getNativeHeapAllocatedSize()
    }
    private fun gc() { repeat(3) { System.gc();System.runFinalization();Thread.sleep(30) } }
    private fun ms(nanos: Long) = nanos/1_000_000.0
    private fun median(values: List<Double>) = percentile(values,.5)
    private fun percentile(values: List<Double>,fraction: Double) = values.sorted()[
        kotlin.math.ceil(values.size*fraction).toInt().coerceIn(1,values.size)-1]
}
