package com.ahmad.quran

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[23,36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PageAssetUiTest {
    private fun await(condition:()->Boolean) {
        val end=System.nanoTime()+20_000_000_000L
        while(!condition() && System.nanoTime()<end) { shadowOf(Looper.getMainLooper()).idle();Thread.sleep(10) }
        assertTrue("Page UI state did not complete",condition())
    }
    private fun draw(a:MainActivity) {
        val root=a.window.decorView
        root.measure(View.MeasureSpec.makeMeasureSpec(720,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1280,View.MeasureSpec.EXACTLY))
        root.layout(0,0,720,1280)
        val v=a.findViewById<NativeSvgPageView>(R.id.mushaf_page)
        val bitmap=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888);v.draw(Canvas(bitmap));bitmap.recycle()
        shadowOf(Looper.getMainLooper()).idle()
    }
    @Test fun missingPageShowsArabicErrorAndRetryRecoversFromVerifiedDiskWithoutNetwork() {
        val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();val model=ViewModelProvider(a)[MainViewModel::class.java]
        try {
            await { !model.state.value.loading };model.openPage(121)
            await { a.reader.failed }
            assertEquals(View.VISIBLE,a.findViewById<View>(R.id.page_asset_status).visibility)
            assertEquals(View.GONE,a.findViewById<View>(R.id.page_asset_progress).visibility)
            assertEquals(a.getString(R.string.page_asset_test_unconfigured),a.findViewById<TextView>(R.id.page_asset_message).text.toString())
            a.findViewById<View>(R.id.page_asset_retry).performClick()
            assertEquals(View.VISIBLE,a.findViewById<View>(R.id.page_asset_progress).visibility)
            await { a.reader.failed }
            val cache=File(a.cacheDir,"mushaf-pages/${QuranSvgEdition.ID}").apply { mkdirs() }
            SourcePageFixtures.original("121.svg").copyTo(File(cache,"121.svg"))
            a.findViewById<View>(R.id.page_asset_retry).performClick()
            val v=a.findViewById<NativeSvgPageView>(R.id.mushaf_page)
            await { v.page?.geometry?.pageNumber==121 };draw(a)
            assertTrue(a.reader.displaying);assertEquals(View.GONE,a.findViewById<View>(R.id.page_asset_status).visibility)
            val p=v.screenPoint(PagePoint(120f,18f));assertEquals(AyahId(5,78),v.hitTest(p.x,p.y))
        } finally { c.pause().stop().destroy() }
    }
    @Test fun actualBundledSeedsRenderWithNoDiskDownloadAndNoExtraPages() {
        val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get();val model=ViewModelProvider(a)[MainViewModel::class.java]
        try {
            await { !model.state.value.loading }
            val svg=a.assets.list("mushaf-svg")!!.filter { it.endsWith(".svg") }.sorted()
            assertEquals(listOf("001.svg","002.svg","003.svg","004.svg","005.svg","300.svg","600.svg","601.svg","602.svg","603.svg","604.svg"),svg)
            for(n in listOf(1,300,604)) {
                model.openPage(n);val v=a.findViewById<NativeSvgPageView>(R.id.mushaf_page)
                await { v.page?.geometry?.pageNumber==n };draw(a)
                assertTrue(a.reader.displaying);assertFalse(a.reader.failed);assertTrue(a.reader.retainedDocumentCount<=3)
                assertEquals(View.GONE,a.findViewById<View>(R.id.page_asset_status).visibility)
            }
            assertFalse(File(a.cacheDir,"mushaf-pages/${QuranSvgEdition.ID}").exists())
        } finally { c.pause().stop().destroy() }
    }
}
