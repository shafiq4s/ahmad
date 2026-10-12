package com.ahmad.quran.mushaf.svg

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import com.ahmad.quran.data.AyahId
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[23,36],qualifiers="xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AyahHighlightTest {
    @org.junit.Before fun installLocalRenderingFixtures() {
        com.ahmad.quran.SourcePageFixtures.install(org.robolectric.RuntimeEnvironment.getApplication())
    }
    private val app get()=RuntimeEnvironment.getApplication()

    @Test fun everySourceAyahHasUniformLineStripsWithoutMutatingItsPolygons() {
        val catalog=org.json.JSONObject(app.assets.open("mushaf-svg/pages.json").bufferedReader().use { it.readText() })
        val adapter=QuranSvgPageAdapter { true }
        val overrides=OpeningPageHitOverrides(app.assets.open("mushaf-svg/opening-hit-overrides.json").bufferedReader().use { it.readText() })
        fun box(a:JSONArray)=RectF(a.getDouble(0).toFloat(),a.getDouble(1).toFloat(),(a.getDouble(0)+a.getDouble(2)).toFloat(),(a.getDouble(1)+a.getDouble(3)).toFloat())
        val pages=catalog.getJSONArray("pages")
        for(index in 0 until pages.length()) {
            val row=pages.getJSONObject(index);val n=row.getInt("pageNumber")
            val page=overrides.apply(adapter.parse(catalog.getString("editionId"),n,box(row.getJSONArray("viewBox")),box(row.getJSONArray("renderBox")),
                app.assets.open("mushaf-svg/%03d.json".format(java.util.Locale.ROOT,n)).bufferedReader().use { it.readText() }))
            val expectedHeight=if(n<=2)28.33f*.80f else 36f*.80f
            for(region in page.regions) {
                val before=region.polygons.map { it.points.toList() }
                val lines=AyahHighlight.prepare(page,region).lines
                assertTrue("Missing line highlight: page $n ${region.ayahId}",lines.isNotEmpty())
                assertEquals(lines.size,lines.map { it.centerY() }.distinct().size)
                for(line in lines) {
                    assertEquals(expectedHeight,line.height(),0.001f)
                    assertTrue(line.width()>0);assertTrue(page.viewBox.contains(line))
                }
                for(pair in lines.zipWithNext()) assertTrue("Distinct row gap required",pair.second.top-pair.first.bottom>0)
                assertEquals(before,region.polygons.map { it.points })
            }
            if(n==48) assertEquals(15,AyahHighlight.prepare(page,page.regions.single()).lines.size)
            if(n==121) assertEquals(3,AyahHighlight.prepare(page,page.regions.first()).lines.size)
        }
    }

    @Test fun disconnectedPartsOnOneLineBecomeOneStripButNeverOneTouchRegion() {
        fun rect(l:Float,t:Float,r:Float,b:Float)=PagePolygon(listOf(PagePoint(l,t),PagePoint(r,t),PagePoint(r,b),PagePoint(l,b)))
        val region=AyahPolygonRegion("test",121,5,78,listOf(rect(0f,0f,30f,40f),rect(70f,0f,100f,40f),rect(20f,40f,100f,76f)))
        val page=PolygonMushafPage("test",121,RectF(0f,0f,345f,550f),RectF(0f,0f,345f,550f),listOf(region))
        val lines=AyahHighlight.prepare(page,region).lines
        assertEquals(2,lines.size);assertEquals(0f,lines.first().left,0f);assertEquals(100f,lines.first().right,0f)
        assertEquals(7.2f,lines[1].top-lines[0].bottom,0.001f)
        assertNull("Visual whitespace must not become touchable",page.hitTest(50f,25.25f))
        assertEquals(AyahId(5,78),page.hitTest(20f,25.25f))
    }

    @Test @Config(sdk=[36]) fun greenBackdropFadesSwitchesAndClearsWithoutTintingOpaqueLetters() {
        NativeSvgPageLoader(app,1).use { loader ->
            val controller=org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).setup()
            val activity=controller.get();val view=NativeSvgPageView(activity)
            activity.setContentView(view,android.view.ViewGroup.LayoutParams(600,960))
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
            view.layout(0,0,600,960);view.display(loader.load(1))
            val rows=JSONArray(app.assets.open("mushaf-svg/001.json").bufferedReader().use { it.readText() })
            val plain=capture(view);val originalInk=Bitmap.createBitmap(600,960,Bitmap.Config.ARGB_8888);view.draw(Canvas(originalInk))
            fun select(index:Int) {
                val row=rows.getJSONObject(index);val p=view.screenPoint(PagePoint(row.getDouble("x").toFloat(),row.getDouble("y").toFloat()))
                val now=SystemClock.uptimeMillis();val e=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,p.x,p.y,0)
                view.dispatchTouchEvent(e);e.recycle();shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(650))
            }
            val fadeField=NativeSvgPageView::class.java.getDeclaredField("fade").apply { isAccessible=true }
            fun animation()=fadeField.get(view) as android.animation.ValueAnimator
            select(0);assertEquals(AyahId(1,1),view.selectedAyah);assertEquals(140L,animation().duration);animation().end()
            val selected=capture(view);var green=0;var opaqueInk=0
            for(y in 0 until plain.height) for(x in 0 until plain.width) {
                val a=plain.getPixel(x,y);val b=selected.getPixel(x,y);val source=originalInk.getPixel(x,y)
                if(Color.alpha(source)==255 && Color.red(source)<80 && Color.green(source)<80 && Color.blue(source)<80) {
                    assertEquals("Opaque Quran ink tinted",a,b);opaqueInk++
                }
                if(a==Color.WHITE && b!=a) {
                    assertTrue("Unexpected backdrop: ${Integer.toHexString(b)}",Color.green(b)>=Color.blue(b) && Color.blue(b)>=Color.red(b))
                    assertTrue(Color.red(b)>225);if(Color.green(b)-Color.red(b)>1)green++
                }
            }
            assertTrue(green>100);assertTrue(opaqueInk>100)
            select(1);assertEquals(AyahId(1,2),view.selectedAyah);animation().end()
            view.clearAyahSelection();assertNull(view.selectedAyah);assertEquals(140L,animation().duration)
            animation().currentPlayTime=70;val fading=capture(view);assertFalse(equal(plain,fading))
            animation().end();val cleared=capture(view);assertTrue(equal(plain,cleared))
            plain.recycle();originalInk.recycle();selected.recycle();fading.recycle();cleared.recycle();view.release();controller.pause().stop().destroy()
        }
    }
    private fun capture(view:NativeSvgPageView)=Bitmap.createBitmap(600,960,Bitmap.Config.ARGB_8888).also { it.eraseColor(Color.WHITE);view.draw(Canvas(it)) }
    private fun equal(a:Bitmap,b:Bitmap):Boolean { val x=IntArray(a.width*a.height);val y=IntArray(x.size);a.getPixels(x,0,a.width,0,0,a.width,a.height);b.getPixels(y,0,b.width,0,0,b.width,b.height);return x.contentEquals(y) }
}
