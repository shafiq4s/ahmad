package com.ahmad.quran.mushaf.svg

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import com.ahmad.quran.data.AyahId
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[23,36],qualifiers="xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AyahHighlightTest {
    private val app get()=RuntimeEnvironment.getApplication()
    @Test @Config(sdk=[36]) fun visualInsetProtectsActualInkAndNeverChangesTouchGeometry() {
        val report=JSONArray()
        NativeSvgPageLoader(app,1).use { loader ->
            for(number in listOf(1,2,48,121,604)) {
                val page=loader.load(number)
                val rows=JSONArray(app.assets.open("mushaf-svg/%03d.json".format(java.util.Locale.ROOT,number)).bufferedReader().use { it.readText() })
                // Test every opening ayah, both components of 5:78, long 2:282 and final 114:6.
                val regions=if(number<=2)page.geometry.regions else if(number==604)listOf(page.geometry.regions.last()) else listOf(page.geometry.regions.first())
                for(region in regions) {
                    val beforePoints=region.polygons.map { it.points.toList() }
                    val plan=AyahHighlight.prepare(page,region)
                    for(zoom in listOf(1f,2f,4f)) {
                        val row=(0 until rows.length()).map { rows.getJSONObject(it) }.first { it.getInt("surahNumber")==region.surahNumber && it.getInt("ayahNumber")==region.ayahNumber }
                        val point=PagePoint(row.getDouble("x").toFloat(),row.getDouble("y").toFloat())
                        val view=NativeSvgPageView(app);view.layout(0,0,600,960);view.display(page)
                        val focus=view.screenPoint(point);view.setZoom(zoom,focus.x,focus.y)
                        val p=view.screenPoint(point);assertEquals(region.ayahId,view.hitTest(p.x,p.y))
                        val matrix=Matrix();matrix.setRectToRect(page.geometry.renderBox,RectF(0f,0f,600f,960f),Matrix.ScaleToFit.CENTER)
                        matrix.postScale(zoom,zoom,focus.x,focus.y)
                        val values=FloatArray(9);matrix.getValues(values)
                        val visual=plan.path(1f/values[Matrix.MSCALE_X],app.resources.displayMetrics.density)
                        val outside=Path(visual);assertTrue(outside.op(region.path(),Path.Op.DIFFERENCE));assertTrue(outside.isEmpty)
                        val artwork=Bitmap.createBitmap(600,960,Bitmap.Config.ARGB_8888)
                        val mask=Bitmap.createBitmap(600,960,Bitmap.Config.ARGB_8888)
                        val originalMask=Bitmap.createBitmap(600,960,Bitmap.Config.ARGB_8888)
                        val c=Canvas(artwork);c.concat(matrix);c.clipPath(region.path());page.artwork.renderToCanvas(c,page.geometry.viewBox)
                        val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.BLACK }
                        Canvas(mask).apply { concat(matrix);drawPath(visual,paint) }
                        Canvas(originalMask).apply { concat(matrix);drawPath(region.path(),paint) }
                        val pixels=IntArray(600*960);val highlighted=IntArray(pixels.size);val original=IntArray(pixels.size)
                        artwork.getPixels(pixels,0,600,0,0,600,960);mask.getPixels(highlighted,0,600,0,0,600,960);originalMask.getPixels(original,0,600,0,0,600,960)
                        var ink=0;var lost=0;var area=0;var originalArea=0
                        for(i in pixels.indices) {
                            if(Color.alpha(highlighted[i])>0)area++
                            if(Color.alpha(original[i])>0)originalArea++
                            if(Color.alpha(pixels[i])>20 && Color.red(pixels[i])<230) { ink++;if(Color.alpha(highlighted[i])==0)lost++ }
                        }
                        assertTrue(ink>0);assertEquals("Lost selected ink: $number ${region.ayahId} ${zoom}x",0,lost)
                        assertTrue(area<=originalArea)
                        assertEquals(beforePoints,region.polygons.map { it.points })
                        assertEquals(region.ayahId,view.hitTest(p.x,p.y))
                        report.put(JSONObject().put("page",number).put("ayah","${region.surahNumber}:${region.ayahNumber}").put("zoom",zoom)
                            .put("inkPixels",ink).put("lostInkPixels",lost).put("visualArea",area).put("originalArea",originalArea)
                            .put("inkPatches",plan.patchCount).put("insetReduced",plan.insetReduced))
                        artwork.recycle();mask.recycle();originalMask.recycle();view.release()
                    }
                }
            }
        }
        val tools=File(requireNotNull(System.getProperty("quran.sourceDir")),"../..")
        File(tools,"ayah-highlight/ink-safety-api-${android.os.Build.VERSION.SDK_INT}.json").writeText(report.toString(2))
    }
    @Test @Config(sdk=[36]) fun honeyBackdropFadesSwitchesAndClearsWithoutTintingOpaqueLetters() {
        NativeSvgPageLoader(app,1).use { loader ->
            val activityController=org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).setup()
            val activity=activityController.get()
            val view=NativeSvgPageView(activity)
            activity.setContentView(view,android.view.ViewGroup.LayoutParams(600,960))
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
            view.layout(0,0,600,960);view.display(loader.load(1))
            val rows=JSONArray(app.assets.open("mushaf-svg/001.json").bufferedReader().use { it.readText() })
            val plain=capture(view)
            val originalInk=Bitmap.createBitmap(600,960,Bitmap.Config.ARGB_8888)
            view.draw(Canvas(originalInk))
            fun select(index:Int) {
                val row=rows.getJSONObject(index);val p=view.screenPoint(PagePoint(row.getDouble("x").toFloat(),row.getDouble("y").toFloat()))
                val now=SystemClock.uptimeMillis();val e=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,p.x,p.y,0)
                view.dispatchTouchEvent(e);e.recycle();shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(650))
            }
            val fadeField=NativeSvgPageView::class.java.getDeclaredField("fade").apply { isAccessible=true }
            fun animation()=fadeField.get(view) as android.animation.ValueAnimator
            select(0)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
            assertEquals(AyahId(1,1),view.selectedAyah)
            assertEquals(140L,animation().duration)
            animation().end()
            val selected=capture(view);var honey=0;var opaqueInk=0
            for(y in 0 until plain.height) for(x in 0 until plain.width) {
                val a=plain.getPixel(x,y);val b=selected.getPixel(x,y)
                val source=originalInk.getPixel(x,y)
                if(Color.alpha(source)==255 && Color.red(source)<80 && Color.green(source)<80 && Color.blue(source)<80) {
                    assertEquals("Opaque Quran ink tinted",a,b);opaqueInk++
                }
                if(a==Color.WHITE && b!=a) {
                    assertTrue("Unexpected backdrop at $x,$y: ${Integer.toHexString(b)}",Color.red(b)>=Color.green(b) && Color.green(b)>=Color.blue(b))
                    assertTrue(Color.red(b)>=244);honey++
                }
            }
            assertTrue(honey>100);assertTrue("Opaque source ink assertion must not be vacuous",opaqueInk>100)
            select(1);shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200));assertEquals(AyahId(1,2),view.selectedAyah);animation().end()
            val clearField=NativeSvgPageView::class.java.getDeclaredField("clearSelection").apply { isAccessible=true }
            (clearField.get(view) as Runnable).run();assertNull(view.selectedAyah)
            // Drive the actual platform animator explicitly; offscreen tests have no display vsync.
            assertEquals(140L,animation().duration)
            animation().currentPlayTime=70
            val fading=capture(view);assertFalse(equal(plain,fading))
            val alphaField=NativeSvgPageView::class.java.getDeclaredField("previousAlpha").apply { isAccessible=true }
            assertEquals(0.5f,alphaField.getFloat(view),0.05f)
            animation().end();val cleared=capture(view);assertTrue(equal(plain,cleared))
            select(2);animation().end()
            val now=SystemClock.uptimeMillis();val up=MotionEvent.obtain(now,now,MotionEvent.ACTION_UP,0f,0f,0);view.dispatchTouchEvent(up);up.recycle()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2000));assertNull(view.selectedAyah)
            plain.recycle();originalInk.recycle();selected.recycle();fading.recycle();cleared.recycle();view.release();activityController.pause().stop().destroy()
        }
    }
    private fun capture(view:NativeSvgPageView)=Bitmap.createBitmap(600,960,Bitmap.Config.ARGB_8888).also { it.eraseColor(Color.WHITE);view.draw(Canvas(it)) }
    private fun equal(a:Bitmap,b:Bitmap):Boolean { val x=IntArray(a.width*a.height);val y=IntArray(x.size);a.getPixels(x,0,a.width,0,0,a.width,a.height);b.getPixels(y,0,b.width,0,0,b.width,b.height);return x.contentEquals(y) }
}
