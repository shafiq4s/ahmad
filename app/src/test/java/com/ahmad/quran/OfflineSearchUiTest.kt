package com.ahmad.quran

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.lifecycle.ViewModelProvider
import com.ahmad.quran.data.AyahId
import com.ahmad.quran.data.QuranRepository
import com.ahmad.quran.mushaf.svg.NativeSvgPageView
import com.ahmad.quran.ui.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[23,36],qualifiers="ar-w360dp-h640dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OfflineSearchUiTest {
    private fun await(condition:()->Boolean) {
        val end=System.nanoTime()+20_000_000_000L
        while(!condition() && System.nanoTime()<end) { shadowOf(Looper.getMainLooper()).idle();Thread.sleep(10) }
        assertTrue("Search UI did not become ready",condition())
    }
    private fun layout(a:MainActivity) {
        a.window.decorView.apply { measure(View.MeasureSpec.makeMeasureSpec(720,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1280,View.MeasureSpec.EXACTLY));layout(0,0,720,1280) }
    }
    private fun settle(a:MainActivity) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300));layout(a) }
    private fun ready(a:MainActivity) { await { !ViewModelProvider(a)[MainViewModel::class.java].state.value.loading };settle(a) }
    private fun renderPage(a:MainActivity,n:Int) {
        await { !ViewModelProvider(a)[MainViewModel::class.java].state.value.loading }
        val view=a.findViewById<NativeSvgPageView>(R.id.mushaf_page)
        await { view.page?.geometry?.pageNumber==n };layout(a)
        val b=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888);view.draw(Canvas(b));b.recycle();settle(a)
    }
    private fun search(a:MainActivity,q:String) {
        a.findViewById<EditText>(R.id.quran_search_query).setText(q)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300))
        await { !ViewModelProvider(a)[MainViewModel::class.java].state.value.searchLoading };settle(a)
    }
    @Test fun homeSearchResultAndBackFlowShowOriginalTextAndSelectTheSameCanonicalId() {
        SourcePageFixtures.install(RuntimeEnvironment.getApplication())
        val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get()
        try {
            ready(a)
            assertEquals(View.VISIBLE,a.findViewById<View>(R.id.home_panel).visibility)
            assertEquals(View.GONE,a.findViewById<View>(R.id.launch_panel).visibility)
            assertEquals(0,a.reader.parsedDocumentCount)
            assertEquals(a.getString(R.string.last_read_empty),a.findViewById<TextView>(R.id.last_location).text)
            assertFalse(a.findViewById<View>(R.id.continue_reading).isEnabled)
            capture(a.window.decorView,"01-home")
            a.findViewById<View>(R.id.open_search).performClick();settle(a);search(a,"الحمد لله")
            val rows=a.findViewById<ListView>(R.id.search_results);assertTrue(rows.adapter.count>0)
            val ayah=rows.adapter.getItem(0) as com.ahmad.quran.data.Ayah
            assertEquals(AyahId(1,2),ayah.id)
            val row=rows.adapter.getView(0,null,rows)
            QuranRepository.open(a).use { assertEquals(it.getAyah(1,2)!!.text,row.findViewById<TextView>(R.id.search_result_text).text.toString()) }
            capture(a.window.decorView,"02-search")
            rows.performItemClick(row,0,1002);renderPage(a,1)
            val view=a.findViewById<NativeSvgPageView>(R.id.mushaf_page)
            assertEquals(AyahId(1,2),view.selectedAyah);assertFalse(a.ayahDialog.isOpen)
            assertNull(ViewModelProvider(a)[MainViewModel::class.java].state.value.targetAyah)
            assertTrue(a.reader.retainedDocumentCount<=3)
            a.onBackPressedDispatcher.onBackPressed();settle(a)
            assertEquals(Screen.SEARCH,ViewModelProvider(a)[MainViewModel::class.java].state.value.screen)
            assertEquals("الحمد لله",a.findViewById<EditText>(R.id.quran_search_query).text.toString())
            search(a,"عبارة غير موجودة");assertEquals(0,rows.adapter.count)
            assertEquals(a.getString(R.string.quran_search_empty),a.findViewById<TextView>(R.id.search_status).text)
            capture(a.window.decorView,"03-empty-search")
            a.onBackPressedDispatcher.onBackPressed();settle(a)
            assertEquals(Screen.HOME,ViewModelProvider(a)[MainViewModel::class.java].state.value.screen)
            assertTrue(a.findViewById<View>(R.id.continue_reading).isEnabled)
            capture(a.window.decorView,"01-home")
        } finally { c.pause().stop().destroy() }
    }
    @Test fun directJumpDialogRejectsInvalidAyahsAndSelectsAllFourReferenceTargets() {
        SourcePageFixtures.install(RuntimeEnvironment.getApplication())
        val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get()
        try {
            ready(a);a.findViewById<View>(R.id.read_pages).performClick();renderPage(a,1)
            val model=ViewModelProvider(a)[MainViewModel::class.java]
            for((id,page) in listOf(AyahId(1,1) to 1,AyahId(2,282) to 48,AyahId(5,78) to 121,AyahId(114,6) to 604)) {
                a.findViewById<View>(R.id.go_page).performClick()
                val d=checkNotNull(a.pageJumpDialog)
                d.findViewById<RadioGroup>(R.id.jump_mode)!!.check(R.id.jump_mode_surah)
                d.findViewById<Spinner>(R.id.jump_selection)!!.setSelection(id.surahNumber-1);settle(a)
                val input=d.findViewById<EditText>(R.id.jump_ayah_number)!!
                val count=model.state.value.surahs[id.surahNumber-1].ayahCount
                input.setText((count+1).toString());d.findViewById<View>(R.id.jump_confirm)!!.performClick()
                assertTrue(d.isShowing);assertNotNull(input.error)
                input.setText(id.ayahNumber.toString())
                if(id==AyahId(2,282)) capture(d.window!!.decorView,"04-ayah-jump")
                d.findViewById<View>(R.id.jump_confirm)!!.performClick();renderPage(a,page)
                assertEquals(id,a.findViewById<NativeSvgPageView>(R.id.mushaf_page).selectedAyah)
                assertFalse(a.ayahDialog.isOpen);assertEquals(page,ReadingPreferences(a).lastLocation!!.pageNumber)
                if(id==AyahId(2,282)) capture(a.window.decorView,"05-long-ayah-selected")
                assertTrue(a.reader.retainedDocumentCount<=3)
            }
            assertTrue(model.openAyah(AyahId(114,1)));renderPage(a,604)
            assertEquals(AyahId(114,1),a.findViewById<NativeSvgPageView>(R.id.mushaf_page).selectedAyah)
            model.toggleBookmark(604,AyahId(114,1))
            a.findViewById<View>(R.id.nav_quran).performClick();settle(a)
            assertEquals(114,a.findViewById<ListView>(R.id.surah_list).adapter.count)
            model.navigateTo(Screen.HOME);settle(a);a.findViewById<View>(R.id.open_bookmarks).performClick();settle(a)
            assertEquals(1,a.findViewById<ListView>(R.id.bookmarks_list).adapter.count)
        } finally { c.pause().stop().destroy() }
    }
    @Test fun unavailableTargetSurvivesRetryAndRepeatedRetryWhileLoadingCannotDuplicateTheRequest() {
        val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get()
        try {
            ready(a);val model=ViewModelProvider(a)[MainViewModel::class.java]
            assertTrue(model.openAyah(AyahId(5,78)));await { a.reader.failed };settle(a)
            assertEquals(AyahId(5,78),model.state.value.targetAyah)
            assertNull(ReadingPreferences(a).lastLocation)
            assertEquals(a.getString(R.string.page_asset_unavailable),a.findViewById<TextView>(R.id.page_asset_message).text)
            capture(a.window.decorView,"06-unavailable-page")
            val retry=a.findViewById<View>(R.id.page_asset_retry)
            retry.performClick();assertFalse(retry.isEnabled);retry.performClick()
            await { a.reader.failed }
            val dir=File(a.cacheDir,"mushaf-pages/${com.ahmad.quran.mushaf.svg.QuranSvgEdition.ID}").apply { mkdirs() }
            SourcePageFixtures.original("121.svg").copyTo(File(dir,"121.svg"))
            retry.performClick();renderPage(a,121)
            assertEquals(AyahId(5,78),a.findViewById<NativeSvgPageView>(R.id.mushaf_page).selectedAyah)
            assertNull(model.state.value.targetAyah);assertEquals(121,ReadingPreferences(a).lastLocation!!.pageNumber)
        } finally { c.pause().stop().destroy() }
    }
    private fun capture(view:View,name:String) {
        if(android.os.Build.VERSION.SDK_INT!=36)return
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
        val b=Bitmap.createBitmap(view.width.coerceAtLeast(1),view.height.coerceAtLeast(1),Bitmap.Config.ARGB_8888)
        view.draw(Canvas(b))
        val folder=File(requireNotNull(System.getProperty("quran.sourceDir")),"../../offline-search/screenshots/simulated-api36").apply { mkdirs() }
        File(folder,"$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG,100,it) };b.recycle()
    }
}
