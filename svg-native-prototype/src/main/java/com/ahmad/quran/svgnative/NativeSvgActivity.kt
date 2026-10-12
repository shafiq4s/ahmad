package com.ahmad.quran.svgnative

import com.ahmad.quran.mushaf.svg.NativeSvgPageView

import com.ahmad.quran.mushaf.svg.NativeSvgPageLoader

import com.ahmad.quran.mushaf.svg.NativeSvgPage

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.Build
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.concurrent.Executors
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class NativeSvgActivity : Activity() {
    lateinit var pageView: NativeSvgPageView
        private set
    lateinit var root: LinearLayout
        private set
    private lateinit var loader: NativeSvgPageLoader
    private val worker = Executors.newSingleThreadExecutor()
    private var requestedPage = 1
    @Volatile private var generation = 0
    @Volatile private var destroyed = false
    private lateinit var label: TextView
    private lateinit var statistics: TextView
    private var capacity = 1
    @Volatile private var modeSession = 0
    private var selectedId = "—"
    private val createdAt = System.nanoTime()
    private val samples = JSONArray()
    private val presses = JSONArray()
    private val pss = JSONArray()
    private val handler = Handler(Looper.getMainLooper())
    private val memorySample = object : Runnable {
        override fun run() {
            worker.execute {
                if (!destroyed) {
                    val info = Debug.MemoryInfo()
                    val sampleMode = capacity
                    val sampleSession = modeSession
                    Debug.getMemoryInfo(info)
                    runOnUiThread {
                        if (!destroyed && capacity == sampleMode && modeSession == sampleSession) {
                            pss.put(JSONObject().put("mode", sampleMode).put("session", sampleSession)
                                .put("elapsedMs", (System.nanoTime()-createdAt)/1e6)
                                .put("pssKiB", info.totalPss))
                            updateStatistics()
                        }
                    }
                }
            }
            handler.postDelayed(this, 3000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loader = NativeSvgPageLoader(this, 1, "native-svg")
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(Color.WHITE)
            setPadding(12, 10, 12, 10)
        }
        label = TextView(this).apply { textSize = 16f; gravity = Gravity.CENTER }
        root.addView(label, LinearLayout.LayoutParams(-1, 48))
        statistics = TextView(this).apply {
            textSize = 11f
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setTextIsSelectable(true)
        }
        root.addView(statistics, LinearLayout.LayoutParams(-1, -2))
        pageView = NativeSvgPageView(this).apply {
            onAyahLongPress = {
                label.text = getString(R.string.ayah_label, it.surahNumber, it.ayahNumber)
                selectedId = "${it.surahNumber}:${it.ayahNumber}"
                presses.put(JSONObject().put("surahNumber", it.surahNumber).put("ayahNumber", it.ayahNumber)
                    .put("page", requestedPage).put("mode", capacity).put("session", modeSession)
                    .put("dispatchMsExcludingOsTimeout", lastLongPressDispatchNanos/1e6).put("hitTestMs",lastHitTestNanos/1e6))
                updateStatistics()
            }
        }
        root.addView(pageView, LinearLayout.LayoutParams(-1, 0, 1f))
        val navigation = LinearLayout(this)
        listOf(R.string.previous_page to -1, R.string.next_page to 1).forEach { (title, step) ->
            navigation.addView(Button(this).apply {
                text = getString(title)
                setOnClickListener { loader.pages.getOrNull(loader.pages.indexOf(requestedPage)+step)?.let { requestPage(it) } }
            }, LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(navigation)
        val controls = LinearLayout(this)
        controls.addView(Button(this).apply {
            text = getString(R.string.cache_mode, if(capacity==1) "A" else "B", capacity)
            setOnClickListener {
                capacity = if (capacity == 1) 3 else 1
                modeSession++
                text = getString(R.string.cache_mode, if(capacity==1) "A" else "B", capacity)
                requestPage(requestedPage, resetCache = true)
            }
        }, LinearLayout.LayoutParams(0, -2, 2f))
        listOf(1f, 2f, 4f).forEach { zoom ->
            controls.addView(Button(this).apply {
                text = getString(R.string.zoom_value, zoom.toInt())
                setOnClickListener { pageView.setZoom(zoom) }
            }, LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(controls)
        setContentView(root)
        requestPage(savedInstanceState?.getInt("page", 1) ?: 1)
    }

    private fun requestPage(number: Int, resetCache: Boolean = false) {
        requestedPage = number
        val request = ++generation
        val mode = capacity
        val session = modeSession
        selectedId = "—"
        val started = System.nanoTime()
        label.text = getString(R.string.loading_prompt, number)
        // Clear the previous document before parsing; one-page mode never retains a UI cache.
        pageView.release()
        worker.execute {
            if (request != generation || destroyed) return@execute
            try {
                if (resetCache) loader.cache.evictAll()
                loader.prepareCurrent(number, mode)
                val cached = loader.cache.snapshot().containsKey(number)
                val page = loader.load(number)
                runOnUiThread {
                    if (!destroyed && generation == request) {
                        pageView.display(page)
                        pageView.onNextRender = {
                            samples.put(JSONObject().put("page", number).put("mode", mode).put("session", session).put("cacheHit", cached)
                                .put("coldActivityContentMs", if(samples.length()==0)(System.nanoTime()-createdAt)/1e6 else JSONObject.NULL)
                                .put("contentMs",(System.nanoTime()-started)/1e6)
                                .put("parseMs",if(cached)0.0 else page.parseNanos/1e6)
                                .put("renderMs",pageView.lastRenderNanos/1e6).put("cacheSize",loader.cache.size())
                                .put("hardwareAccelerated",pageView.lastCanvasHardware)
                                .put("windowHardwareAccelerated",pageView.isHardwareAccelerated)
                                .put("viewLayerType",pageView.layerType))
                            updateStatistics()
                            if (mode == 3) worker.execute {
                                if (!destroyed && request == generation) {
                                    loader.prefetchNeighbours(number)
                                    runOnUiThread { if (!destroyed && request == generation) updateStatistics() }
                                }
                            }
                        }
                        label.text = getString(R.string.page_prompt, number)
                    }
                }
            } catch (_: Exception) {
                runOnUiThread { if (!destroyed && generation == request) label.setText(R.string.error_prompt) }
            }
        }
    }

    private fun updateStatistics() {
        val rows = (0 until samples.length()).map { samples.getJSONObject(it) }.filter { it.getInt("session")==modeSession }
        val warm = rows.drop(1).map { it.getDouble("contentMs") }.sorted()
        fun percentile(fraction: Double) = if(warm.isEmpty()) "—" else
            "%.1f".format(java.util.Locale.ROOT, warm[(kotlin.math.ceil(warm.size*fraction).toInt()-1).coerceAtLeast(0)])
        val memory = (0 until pss.length()).map { pss.getJSONObject(it) }.filter { it.getInt("session")==modeSession }
        val peak = memory.maxOfOrNull { it.getInt("pssKiB") }?.toString() ?: "—"
        val currentPss = memory.lastOrNull()?.getInt("pssKiB")?.toString() ?: "—"
        val row = rows.lastOrNull()
        val cold = if(samples.length()==0)0.0 else samples.getJSONObject(0).getDouble("coldActivityContentMs")
        statistics.text = getString(R.string.validation_stats, Build.VERSION.SDK_INT,
            if(pageView.lastCanvasHardware) "hardware" else "software", capacity, percentile(0.5),percentile(0.95),
            row?.getDouble("parseMs") ?: 0.0, pageView.lastRenderNanos/1e6, peak,
            rows.count { it.getBoolean("cacheHit") }, rows.count { !it.getBoolean("cacheHit") },cold,
            pageView.lastLongPressDispatchNanos/1e6, Build.MODEL, loader.cache.size(),
            if(rows.size>1)"%.1f".format(java.util.Locale.ROOT,row!!.getDouble("contentMs")) else "—",
            currentPss,selectedId)
    }

    private fun saveMeasurements() {
        val report = JSONObject().put("deviceModel",Build.MODEL).put("manufacturer",Build.MANUFACTURER)
            .put("android",Build.VERSION.RELEASE).put("api",Build.VERSION.SDK_INT)
            .put("transitions",samples).put("longPresses",presses).put("pssSamples",pss)
            .put("pssSamplingPolicy","Process PSS every 3 seconds; maximum sampled PSS is not an instantaneous peak.")
            .put("contentTimingPolicy","From activity/request to first View draw completion; excludes compositor presentation and OS process-launch time.")
        File(filesDir,"device-validation.json").writeText(report.toString(2))
    }
    override fun onResume() {
        super.onResume()
        handler.postDelayed(memorySample,3000)
    }
    override fun onPause() {
        handler.removeCallbacks(memorySample)
        saveMeasurements()
        super.onPause()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("page", requestedPage)
        super.onSaveInstanceState(outState)
    }
    override fun onDestroy() {
        destroyed = true
        handler.removeCallbacks(memorySample)
        pageView.release()
        worker.execute { loader.close() }
        worker.shutdown()
        super.onDestroy()
    }
}
