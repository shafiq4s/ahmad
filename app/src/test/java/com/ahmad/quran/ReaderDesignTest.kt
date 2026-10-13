package com.ahmad.quran

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.widget.ListView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.ahmad.quran.data.AyahId
import com.ahmad.quran.mushaf.svg.NativeSvgPageView
import com.ahmad.quran.mushaf.svg.PagePoint
import com.ahmad.quran.mushaf.svg.QuranSvgEdition
import com.ahmad.quran.ui.MainActivity
import com.ahmad.quran.ui.MainViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[23,36],qualifiers="ar-w360dp-h640dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReaderDesignTest {
    @org.junit.Before fun installLocalRenderingFixtures() {
        com.ahmad.quran.SourcePageFixtures.install(org.robolectric.RuntimeEnvironment.getApplication())
    }
    private val width=720
    private val height=1280
    private fun await(condition:()->Boolean) {
        val deadline=System.nanoTime()+20_000_000_000L
        while(!condition() && System.nanoTime()<deadline) { shadowOf(Looper.getMainLooper()).idle();Thread.sleep(10) }
        assertTrue("Local reading state did not arrive",condition())
    }
    private fun layout(a:MainActivity) {
        a.window.decorView.apply {
            measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY))
            layout(0,0,width,height)
        }
    }
    private fun settle(a:MainActivity) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(400));layout(a) }
    private fun ready(a:MainActivity) { await { !ViewModelProvider(a)[MainViewModel::class.java].state.value.loading };settle(a) }
    private fun page(a:MainActivity,n:Int):NativeSvgPageView {
        ViewModelProvider(a)[MainViewModel::class.java].openPage(n)
        val v=a.findViewById<NativeSvgPageView>(R.id.mushaf_page)
        await { v.page?.geometry?.pageNumber==n };layout(a)
        val b=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888);v.draw(Canvas(b));b.recycle();settle(a)
        return v
    }
    private fun assertNoCover(a:MainActivity,v:NativeSvgPageView) {
        val top=a.findViewById<View>(R.id.top_bar);val bottom=a.findViewById<View>(R.id.bottom_bar)
        val p=IntArray(2);val t=IntArray(2);val b=IntArray(2)
        v.getLocationInWindow(p);top.getLocationInWindow(t);bottom.getLocationInWindow(b)
        assertTrue(p[1]>=t[1]+top.height)
        assertTrue(p[1]+v.height<=b[1])
    }
    @Test fun indexHeaderSettingsAndTouchTargetsUseCurrentArtworkEdition() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup();val a=controller.get()
        try {
            ready(a);assertEquals(0,a.reader.parsedDocumentCount)
            a.findViewById<View>(R.id.open_surahs).performClick();settle(a)
            val list=a.findViewById<ListView>(R.id.surah_list);val map=QuranSvgEdition(a)
            for(i in 0 until list.adapter.count) {
                val row=list.adapter.getView(i,null,list)
                assertEquals(a.getString(R.string.surah_first_page,map.firstPage(i+1)),row.findViewById<TextView>(R.id.surah_first_page).text.toString())
            }
            assertTrue(list.childCount>0)
            for(i in 0 until list.childCount) assertEquals(View.LAYOUT_DIRECTION_RTL,list.getChildAt(i).layoutDirection)
            capture(a,"surah-index")
            val v=page(a,121)
            assertEquals(a.getString(R.string.ayah_surah,"المائدة"),a.findViewById<TextView>(R.id.screen_title).text.toString())
            assertEquals(a.getString(R.string.ayah_juz,6),a.findViewById<TextView>(R.id.reader_location).text.toString())
            assertEquals(a.getString(R.string.page_fraction,121,604),a.findViewById<TextView>(R.id.page_indicator).text.toString())
            assertNoCover(a,v)
            for(id in listOf(R.id.nav_quran,R.id.nav_settings,R.id.previous_page,R.id.next_page,R.id.go_page)) {
                val button=a.findViewById<View>(id)
                assertTrue("48dp touch height",button.height>=48*a.resources.displayMetrics.density)
                assertTrue("48dp touch width",button.width>=48*a.resources.displayMetrics.density)
            }
            for(id in listOf("zoom_1","zoom_2","zoom_3","zoom_4","zoom_controls")) {
                @Suppress("DiscouragedApi") val removed=a.resources.getIdentifier(id,"id",a.packageName)
                assertEquals("Removed debug control $id",0,removed)
            }
            capture(a,"reader-121")
            a.findViewById<View>(R.id.nav_settings).performClick();settle(a)
            assertEquals(a.getString(R.string.app_version,BuildConfig.VERSION_NAME),a.findViewById<TextView>(R.id.app_version).text.toString())
            capture(a,"settings")
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun barsCollapseSmoothlyExpandPageAndCanReverseWithoutChangingTouchGeometry() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup();val a=controller.get()
        try {
            ready(a);val v=page(a,48);val box=v.page!!.geometry.renderBox;val identity=AyahId(2,282)
            val source=PagePoint(120f,250f);val before=v.screenPoint(source)
            assertEquals(identity,v.hitTest(before.x,before.y))
            val originalHeight=v.height;val topHeight=a.findViewById<View>(R.id.top_bar).height
            await { a.reader.retainedDocumentCount==3 }
            val parsed=a.reader.parsedDocumentCount
            v.onReaderTap!!.invoke()
            val field=MainActivity::class.java.getDeclaredField("chromeAnimation").apply { isAccessible=true }
            var animation=field.get(a) as ValueAnimator;assertEquals(190L,animation.duration)
            animation.currentPlayTime=95;layout(a)
            assertTrue(a.findViewById<View>(R.id.top_bar).height in 1 until topHeight)
            assertTrue(v.height>originalHeight)
            val mid=v.screenPoint(source);assertEquals(identity,v.hitTest(mid.x,mid.y))
            // Reverse from the current fraction, without jumping to a fully open/closed endpoint.
            val midHeight=a.findViewById<View>(R.id.top_bar).height
            v.onReaderTap!!.invoke();animation=field.get(a) as ValueAnimator;layout(a)
            assertEquals(midHeight,a.findViewById<View>(R.id.top_bar).height)
            animation.end();layout(a);assertEquals(originalHeight,v.height);assertNoCover(a,v)
            v.onReaderTap!!.invoke();(field.get(a) as ValueAnimator).end();layout(a)
            assertEquals(View.GONE,a.findViewById<View>(R.id.top_bar).visibility)
            assertEquals(View.GONE,a.findViewById<View>(R.id.bottom_bar).visibility)
            assertTrue(v.height>originalHeight)
            val x0=v.screenPoint(PagePoint(box.left,box.top));val x1=v.screenPoint(PagePoint(box.right,box.bottom))
            assertEquals(box.width()/box.height(),(x1.x-x0.x)/(x1.y-x0.y),.0001f)
            val enlarged=v.screenPoint(source);assertEquals(identity,v.hitTest(enlarged.x,enlarged.y))
            assertEquals(parsed,a.reader.parsedDocumentCount)
            capture(a,"reader-48-immersive")
            v.onReaderTap!!.invoke();(field.get(a) as ValueAnimator).end();layout(a);assertNoCover(a,v)
            assertEquals(originalHeight,v.height)
        } finally { controller.pause().stop().destroy() }
    }
    private fun capture(a:MainActivity,name:String) {
        if(android.os.Build.VERSION.SDK_INT!=36)return
        settle(a)
        val decor=a.window.decorView;val b=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);decor.draw(Canvas(b))
        val folder=File(requireNotNull(System.getProperty("quran.sourceDir")),"../../reader-design/screenshots/simulated-api36").apply { mkdirs() }
        File(folder,"$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG,100,it) };b.recycle()
    }
}
