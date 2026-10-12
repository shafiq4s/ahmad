package com.ahmad.quran

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.widget.ListView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.ahmad.quran.data.AyahId
import com.ahmad.quran.mushaf.svg.NativeSvgPageView
import com.ahmad.quran.ui.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23, 36], qualifiers = "ar-w360dp-h640dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReadingMaturityUiTest {
    private fun await(condition: () -> Boolean) {
        val end = System.nanoTime() + 20_000_000_000L
        while (!condition() && System.nanoTime() < end) { shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(10) }
        assertTrue("Reading maturity UI did not become ready", condition())
    }
    private fun settle(a: MainActivity) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300))
        a.window.decorView.apply {
            measure(View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(1280, View.MeasureSpec.EXACTLY))
            layout(0, 0, 720, 1280)
        }
    }
    private fun ready(a: MainActivity) { await { !ViewModelProvider(a)[MainViewModel::class.java].state.value.loading }; settle(a) }
    private fun page(a: MainActivity, number: Int) {
        ready(a)
        val view = a.findViewById<NativeSvgPageView>(R.id.mushaf_page)
        await { view.page?.geometry?.pageNumber == number }; settle(a)
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap)); bitmap.recycle(); settle(a)
    }
    @Test fun newestBookmarksAndHistoryOpenExactSelectedAyahAndDeleteCleanly() {
        val app = RuntimeEnvironment.getApplication()
        SourcePageFixtures.install(app)
        ReadingPreferences(app).apply {
            saveBookmark(ReadingBookmark(121, 5, 78, createdAtMillis = 100))
            saveBookmark(ReadingBookmark(604, 114, 6, createdAtMillis = 200))
            recordHistory(ReadingHistoryEntry(48, 2, 282, readAtMillis = System.currentTimeMillis() - 1000))
        }
        val c = Robolectric.buildActivity(MainActivity::class.java).setup(); val a = c.get()
        try {
            ready(a); assertEquals(0, a.reader.parsedDocumentCount)
            a.findViewById<View>(R.id.open_bookmarks).performClick(); settle(a)
            val bookmarks = a.findViewById<ListView>(R.id.bookmarks_list)
            assertEquals(604L, bookmarks.adapter.getItemId(0))
            val row = checkNotNull(bookmarks.getChildAt(0))
            assertEquals(View.LAYOUT_DIRECTION_RTL, row.layoutDirection)
            assertTrue(row.findViewById<TextView>(R.id.bookmark_surah).text.contains("الناس"))
            assertEquals(a.getString(R.string.bookmark_ayah_location, 604, 6), row.findViewById<TextView>(R.id.bookmark_location).text.toString())
            capture(a, "02-bookmarks")
            bookmarks.performItemClick(row, 0, 604); page(a, 604)
            assertEquals(AyahId(114, 6), a.findViewById<NativeSvgPageView>(R.id.mushaf_page).selectedAyah)
            assertFalse(a.ayahDialog.isOpen)
            a.onBackPressedDispatcher.onBackPressed(); settle(a)
            assertEquals(Screen.BOOKMARKS, ViewModelProvider(a)[MainViewModel::class.java].state.value.screen)
            for (i in 0..1) bookmarks.adapter.getView(0, null, bookmarks).findViewById<View>(R.id.bookmark_remove).performClick().also { settle(a) }
            assertEquals(0, bookmarks.adapter.count)
            assertEquals(View.VISIBLE, a.findViewById<View>(R.id.bookmarks_empty).visibility)
            a.onBackPressedDispatcher.onBackPressed(); settle(a)
            assertTrue(a.findViewById<View>(R.id.continue_reading).isEnabled)
            capture(a, "01-home")
            a.findViewById<View>(R.id.open_history).performClick(); settle(a)
            val history = a.findViewById<ListView>(R.id.history_list)
            assertEquals(2, history.adapter.count)
            assertEquals(604L, history.adapter.getItemId(0))
            val historyRow = history.adapter.getView(0, null, history)
            val time = historyRow.findViewById<TextView>(R.id.history_time).text.toString()
            assertTrue(time.any { it in '\u0621'..'\u064A' })
            assertFalse(time.contains("PM") || time.contains("Oct"))
            capture(a, "03-history")
            history.performItemClick(history.adapter.getView(1, null, history), 1, 48); page(a, 48)
            assertEquals(AyahId(2, 282), a.findViewById<NativeSvgPageView>(R.id.mushaf_page).selectedAyah)
            assertTrue(a.reader.retainedDocumentCount <= 3)
            capture(a, "08-reader")
            a.onBackPressedDispatcher.onBackPressed(); settle(a)
            assertEquals(Screen.HISTORY, ViewModelProvider(a)[MainViewModel::class.java].state.value.screen)
            assertEquals(48L, history.adapter.getItemId(0))
        } finally { c.pause().stop().destroy() }
    }
    @Test fun aboutAndSupportAreSeparateAccessibleRtlPagesWithNaturalBack() {
        val c = Robolectric.buildActivity(MainActivity::class.java).setup(); val a = c.get()
        try {
            ready(a); a.findViewById<View>(R.id.nav_settings).performClick(); settle(a)
            assertEquals(View.GONE, a.findViewById<View>(R.id.about_panel).visibility)
            assertEquals(View.GONE, a.findViewById<View>(R.id.support_panel).visibility)
            capture(a, "04-settings")
            a.findViewById<View>(R.id.open_about).performClick(); settle(a)
            assertEquals(Screen.ABOUT, ViewModelProvider(a)[MainViewModel::class.java].state.value.screen)
            assertEquals(View.LAYOUT_DIRECTION_RTL, a.findViewById<View>(R.id.about_panel).layoutDirection)
            assertTrue(a.findViewById<TextView>(R.id.app_version).text.contains(BuildConfig.VERSION_NAME))
            assertTrue(a.findViewById<TextView>(R.id.quran_svg_attribution).text.contains("مشروع مستقل"))
            assertTrue(a.findViewById<TextView>(R.id.quran_svg_attribution).text.contains("مجمع الملك فهد"))
            capture(a, "05-about")
            a.findViewById<View>(R.id.show_license).performClick(); settle(a)
            assertEquals(View.VISIBLE, a.findViewById<View>(R.id.license_notice).visibility)
            assertTrue(a.findViewById<TextView>(R.id.license_notice).text.contains("Tanzil"))
            a.onBackPressedDispatcher.onBackPressed(); settle(a)
            assertEquals(Screen.SETTINGS, ViewModelProvider(a)[MainViewModel::class.java].state.value.screen)
            a.findViewById<View>(R.id.open_support).performClick(); settle(a)
            assertEquals(Screen.SUPPORT, ViewModelProvider(a)[MainViewModel::class.java].state.value.screen)
            capture(a, "06-support")
            a.onBackPressedDispatcher.onBackPressed(); settle(a)
            assertEquals(Screen.SETTINGS, ViewModelProvider(a)[MainViewModel::class.java].state.value.screen)
            a.onBackPressedDispatcher.onBackPressed(); settle(a)
            assertEquals(Screen.HOME, ViewModelProvider(a)[MainViewModel::class.java].state.value.screen)
            a.findViewById<View>(R.id.open_surahs).performClick(); settle(a)
            assertEquals(114, a.findViewById<ListView>(R.id.surah_list).adapter.count)
            capture(a, "07-index")
        } finally { c.pause().stop().destroy() }
    }
    @Test fun emptyHistoryAndBookmarksRemainClearAndRestoredReaderHistorySurvivesRecreation() {
        val c = Robolectric.buildActivity(MainActivity::class.java).setup(); var a = c.get()
        try {
            ready(a); a.findViewById<View>(R.id.open_history).performClick(); settle(a)
            assertEquals(View.VISIBLE, a.findViewById<View>(R.id.history_empty).visibility)
            assertEquals(View.GONE, a.findViewById<View>(R.id.history_list).visibility)
            a.onBackPressedDispatcher.onBackPressed(); settle(a)
            a.findViewById<View>(R.id.read_pages).performClick(); page(a, 1)
            assertTrue(ViewModelProvider(a)[MainViewModel::class.java].openAyah(AyahId(1, 7))); page(a, 1)
            assertEquals(7, ReadingPreferences(a).history.single().ayahNumber)
            a.findViewById<View>(R.id.nav_settings).performClick(); settle(a)
            a.onBackPressedDispatcher.onBackPressed(); page(a, 1)
            assertEquals(Screen.READER, ViewModelProvider(a)[MainViewModel::class.java].state.value.screen)
            ViewModelProvider(a)[MainViewModel::class.java].navigateTo(Screen.HISTORY); settle(a)
            c.recreate(); a = c.get(); ready(a)
            assertEquals(Screen.HISTORY, ViewModelProvider(a)[MainViewModel::class.java].state.value.screen)
            assertEquals(1, a.findViewById<ListView>(R.id.history_list).adapter.count)
        } finally { c.pause().stop().destroy() }
    }
    private fun capture(a: MainActivity, name: String) {
        if (android.os.Build.VERSION.SDK_INT != 36) return
        val view = a.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val dir = File(requireNotNull(System.getProperty("quran.sourceDir")), "../../reading-maturity/screenshots/simulated-api36").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
