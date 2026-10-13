package com.ahmad.quran

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.ahmad.quran.data.AyahId
import com.ahmad.quran.mushaf.svg.NativeSvgPageView
import com.ahmad.quran.mushaf.svg.PagePoint
import com.ahmad.quran.ui.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.io.File
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[23,36],qualifiers="ar-w360dp-h640dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class ReaderPolishUiTest {
    private fun idle(ms:Long)=shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
    private fun await(check:()->Boolean) {
        val end=System.nanoTime()+20_000_000_000L
        while(!check() && System.nanoTime()<end) { shadowOf(Looper.getMainLooper()).idle();Thread.sleep(10) }
        assertTrue("Reader did not become ready",check())
    }
    private fun layout(a:MainActivity,w:Int=720,h:Int=1280) {
        a.window.decorView.apply {
            measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));layout(0,0,w,h)
        }
    }
    private fun page(a:MainActivity,n:Int,w:Int=720,h:Int=1280):NativeSvgPageView {
        ViewModelProvider(a)[MainViewModel::class.java].openPage(n)
        val v=a.findViewById<NativeSvgPageView>(R.id.mushaf_page)
        await { v.page?.geometry?.pageNumber==n };layout(a,w,h)
        val b=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888);v.draw(Canvas(b));b.recycle();idle(250);layout(a,w,h)
        await { a.reader.displaying };return v
    }
    private fun point(a:MainActivity,v:NativeSvgPageView):PagePoint {
        val r=JSONArray(a.assets.open("mushaf-svg/%03d.json".format(java.util.Locale.ROOT,v.page!!.geometry.pageNumber)).bufferedReader().use { it.readText() }).getJSONObject(0)
        return v.screenPoint(PagePoint(r.getDouble("x").toFloat(),r.getDouble("y").toFloat()))
    }
    private fun event(v:NativeSvgPageView,p:PagePoint,action:Int) {
        val now=SystemClock.uptimeMillis();val e=MotionEvent.obtain(now,now,action,p.x,p.y,0);v.dispatchTouchEvent(e);e.recycle()
    }
    private fun tap(v:NativeSvgPageView,p:PagePoint) { event(v,p,MotionEvent.ACTION_DOWN);idle(60);event(v,p,MotionEvent.ACTION_UP);idle(250) }
    private fun capture(view:View,name:String) {
        if(android.os.Build.VERSION.SDK_INT!=36)return
        val dir=File(System.getProperty("quran.sourceDir"),"../../reader-polish/screenshots/simulated-api36").apply { mkdirs() }
        val b=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888);view.draw(Canvas(b))
        File(dir,"$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG,100,it) };b.recycle()
    }
    @Test fun tapHoldModalDismissAndLeavingReaderHaveConsistentChromeAndCanonicalIdentity() {
        SourcePageFixtures.install(RuntimeEnvironment.getApplication())
        val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get()
        try {
            await { !ViewModelProvider(a)[MainViewModel::class.java].state.value.loading };layout(a)
            capture(a.window.decorView,"home")
            val v=page(a,48);val id=AyahId(2,282)
            capture(a.window.decorView,"reader-bars-visible")
            tap(v,point(a,v));layout(a)
            assertNull(v.selectedAyah);assertFalse(a.ayahDialog.isOpen)
            assertEquals(View.GONE,a.findViewById<View>(R.id.top_bar).visibility)
            assertEquals(View.GONE,a.findViewById<View>(R.id.bottom_bar).visibility)
            capture(a.window.decorView,"reader-bars-hidden")
            tap(v,point(a,v));layout(a);assertEquals(View.VISIBLE,a.findViewById<View>(R.id.top_bar).visibility)
            var p=point(a,v);event(v,p,MotionEvent.ACTION_DOWN);idle(500)
            assertEquals(id,v.selectedAyah);assertFalse(a.ayahDialog.isOpen)
            event(v,p,MotionEvent.ACTION_UP);idle(250);layout(a)
            assertEquals(View.GONE,a.findViewById<View>(R.id.top_bar).visibility)
            assertEquals(View.GONE,a.findViewById<View>(R.id.bottom_bar).visibility)
            capture(a.window.decorView,"ayah-highlighted")
            idle(300);assertFalse(a.ayahDialog.isOpen);assertEquals(id,v.selectedAyah)
            p=point(a,v);event(v,p,MotionEvent.ACTION_DOWN);idle(899);assertFalse(a.ayahDialog.isOpen)
            idle(1);assertTrue(a.ayahDialog.isOpen);event(v,p,MotionEvent.ACTION_UP)
            await { a.ayahDialog.dialog.findViewById<View>(R.id.ayah_dialog_copy)!!.isEnabled }
            assertEquals(id,a.ayahDialog.ayahId);idle(200)
            val dialog=a.ayahDialog.dialog.window!!.decorView
            dialog.measure(View.MeasureSpec.makeMeasureSpec(a.ayahDialog.dialog.window!!.attributes.width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1024,View.MeasureSpec.AT_MOST));dialog.layout(0,0,dialog.measuredWidth,dialog.measuredHeight)
            capture(dialog,"ayah-modal")
            a.ayahDialog.dialog.onBackPressedDispatcher.onBackPressed();idle(200);layout(a)
            assertNull(v.selectedAyah);assertFalse(a.ayahDialog.isOpen)
            assertEquals(View.GONE,a.findViewById<View>(R.id.top_bar).visibility)
            tap(v,point(a,v));layout(a);assertEquals(View.VISIBLE,a.findViewById<View>(R.id.top_bar).visibility)
            a.findViewById<View>(R.id.nav_settings).performClick();idle(250);layout(a)
            capture(a.window.decorView,"settings")
            assertEquals(View.VISIBLE,a.findViewById<View>(R.id.top_bar).visibility)
            assertTrue(a.reader.retainedDocumentCount<=3)
        } finally { c.pause().stop().destroy() }
    }
    @Test fun everyLocaleKeepsIdenticalGesturesAndNativeDirection() {
        SourcePageFixtures.install(RuntimeEnvironment.getApplication())
        val c=Robolectric.buildActivity(MainActivity::class.java).setup();var a=c.get()
        try {
            for(tag in InterfaceLocales.tags) {
                InterfaceLocales.select(a,tag);c.recreate();a=c.get()
                await { !ViewModelProvider(a)[MainViewModel::class.java].state.value.loading }
                val v=page(a,1)
                assertEquals(InterfaceLocales.direction(tag),a.findViewById<View>(R.id.app_root).layoutDirection)
                tap(v,point(a,v));layout(a);assertNull(v.selectedAyah)
                assertEquals(View.GONE,a.findViewById<View>(R.id.top_bar).visibility)
                val p=point(a,v);event(v,p,MotionEvent.ACTION_DOWN);idle(500)
                assertEquals(AyahId(1,1),v.selectedAyah);assertFalse(a.ayahDialog.isOpen)
                idle(400);assertEquals(AyahId(1,1),a.ayahDialog.ayahId)
                event(v,p,MotionEvent.ACTION_UP);a.ayahDialog.dismiss();idle(200)
                ViewModelProvider(a)[MainViewModel::class.java].navigateTo(Screen.HOME);idle(200)
            }
        } finally { InterfaceLocales.select(a,InterfaceLocales.SYSTEM);c.pause().stop().destroy() }
    }
    @Test fun smallMediumLargeFittedPagesNeverCropOrOverlapChromeAndTouchMapsStayExact() {
        SourcePageFixtures.install(RuntimeEnvironment.getApplication())
        val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get()
        val samples=JSONArray()
        try {
            await { !ViewModelProvider(a)[MainViewModel::class.java].state.value.loading }
            for((w,h) in listOf(640 to 960,720 to 1280,1200 to 2000)) for(n in listOf(1,2,48,121,604)) {
                val v=page(a,n,w,h);val g=v.page!!.geometry
                for(p in listOf(PagePoint(g.renderBox.left,g.renderBox.top),PagePoint(g.renderBox.right,g.renderBox.bottom))) {
                    val screen=v.screenPoint(p);assertTrue(screen.x>=-0.01f && screen.x<=v.width+0.01f);assertTrue(screen.y>=-0.01f && screen.y<=v.height+0.01f)
                }
                val at=point(a,v);assertNotNull(v.hitTest(at.x,at.y))
                val vp=IntArray(2);val top=IntArray(2);val bottom=IntArray(2)
                v.getLocationInWindow(vp);a.findViewById<View>(R.id.top_bar).getLocationInWindow(top);a.findViewById<View>(R.id.bottom_bar).getLocationInWindow(bottom)
                assertTrue(vp[1]>=top[1]+a.findViewById<View>(R.id.top_bar).height)
                assertTrue(vp[1]+v.height<=bottom[1]);assertTrue(a.reader.retainedDocumentCount<=3)
                samples.put(JSONObject().put("page",n).put("width",w).put("height",h).put("drawMs",v.lastRenderNanos/1e6).put("hitTestUs",v.lastHitTestNanos/1e3).put("retainedDocuments",a.reader.retainedDocumentCount))
            }
            val dir=File(System.getProperty("quran.sourceDir"),"../../reader-polish").apply { mkdirs() }
            File(dir,"desktop-api-${android.os.Build.VERSION.SDK_INT}.json").writeText(JSONObject().put("platform","Robolectric native Canvas on Linux desktop; not Android device timings").put("samples",samples).toString(2)+"\n")
        } finally { c.pause().stop().destroy() }
    }
}
