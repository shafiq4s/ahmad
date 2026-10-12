package com.ahmad.quran

import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import com.ahmad.quran.data.AyahId
import com.ahmad.quran.mushaf.svg.QuranSvgEdition
import com.ahmad.quran.ui.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23, 36])
class ReadingHistoryTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private fun ready(model: MainViewModel) {
        val end = System.nanoTime() + 20_000_000_000L
        while (model.state.value.loading && System.nanoTime() < end) {
            shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(10)
        }
        assertFalse(model.state.value.loading)
    }

    @Test fun historyKeepsTenNewestPagesAndRevisitUpdatesSelectionWithoutDuplicates() {
        val prefs = ReadingPreferences(app)
        val map = QuranSvgEdition(app)
        assertTrue(prefs.history.isEmpty())
        for (page in 1..12) {
            val id = map.firstAyah(page)
            prefs.recordHistory(ReadingHistoryEntry(page, id.surahNumber, readAtMillis = page.toLong()))
        }
        assertEquals((12 downTo 3).toList(), ReadingPreferences(app).history.map { it.pageNumber })
        prefs.recordHistory(ReadingHistoryEntry(3, 2, 6, readAtMillis = 20))
        assertEquals(10, prefs.history.size)
        assertEquals(3, prefs.history.first().pageNumber)
        assertEquals(6, prefs.history.first().ayahNumber)
        assertEquals(1, prefs.history.count { it.pageNumber == 3 })
    }

    @Test fun invalidOrCorruptHistoryCannotCreateCanonicalTargets() {
        val prefs = ReadingPreferences(app)
        for (entry in listOf(ReadingHistoryEntry(605, 1), ReadingHistoryEntry(604, 114, 7),
            ReadingHistoryEntry(604, 112, 5), ReadingHistoryEntry(1, 1, editionId = "other"))) {
            assertThrows(IllegalArgumentException::class.java) { prefs.recordHistory(entry) }
        }
        app.getSharedPreferences("reading", 0).edit().putString("history", """[
            {"page":604,"surah":114,"ayah":7,"edition":"${QuranSvgEdition.ID}","readAtMillis":3},
            {"page":121,"surah":5,"ayah":78,"edition":"${QuranSvgEdition.ID}","readAtMillis":2},
            {"page":1,"surah":1,"ayah":null,"edition":"${QuranSvgEdition.ID}","readAtMillis":1},
            {"broken":true}]""").commit()
        assertEquals(listOf(121, 1), prefs.history.map { it.pageNumber })
        app.getSharedPreferences("reading", 0).edit().putString("history", "invalid JSON").commit()
        assertTrue(prefs.history.isEmpty())
    }

    @Test fun onlyDisplayedPagesAreRecordedAndResumeRestoresSelectedIdentity() {
        val model = MainViewModel(app, SavedStateHandle()); ready(model)
        val prefs = ReadingPreferences(app)
        assertTrue(model.openAyah(AyahId(114, 6))); ready(model)
        assertTrue(prefs.history.isEmpty())
        model.recordPageDisplayed(603)
        assertTrue(prefs.history.isEmpty())
        model.recordPageDisplayed(604)
        assertNull(prefs.history.single().ayahNumber)
        model.recordPageDisplayed(604, AyahId(114, 6))
        assertEquals(ReadingHistoryEntry(604, 114, 6).copy(readAtMillis = prefs.history.single().readAtMillis), prefs.history.single())
        model.navigateTo(Screen.HOME)
        model.recordPageDisplayed(1)
        assertEquals(604, prefs.history.single().pageNumber)
        model.resumeReading(); ready(model)
        assertEquals(604, model.state.value.pageNumber)
        assertEquals(AyahId(114, 6), model.state.value.targetAyah)
        model.navigateTo(Screen.HOME)
        model.openHistory(prefs.history.single()); ready(model)
        assertEquals(AyahId(114, 6), model.state.value.targetAyah)
    }

    @Test fun cacheClearPreservesHistoryBookmarksLastPositionAndEverySetting() {
        val prefs = ReadingPreferences(app)
        prefs.recordHistory(ReadingHistoryEntry(121, 5, 78))
        prefs.saveBookmark(ReadingBookmark(121, 5, 78))
        prefs.saveLocation(ReadingLocation(ReaderMode.PAGE, 5, 78, 121, QuranSvgEdition.ID))
        prefs.setKeepScreenAwake(true); prefs.setAutoResumeReading(true); prefs.setShowReaderBarsOnOpen(false)
        val before = app.getSharedPreferences("reading", 0).all
        val dir = java.io.File(app.cacheDir, "mushaf-pages/${QuranSvgEdition.ID}").apply { mkdirs() }
        java.io.File(dir, "121.svg").writeText("test cache")
        assertTrue(DownloadedPageCache(app).clear())
        assertEquals(before, app.getSharedPreferences("reading", 0).all)
        assertEquals(1, ReadingPreferences(app).history.size)
        assertTrue(app.assets.open("mushaf-svg/001.svg").use { it.read() >= 0 })
        assertTrue(app.assets.open("mushaf-svg/121.json").use { it.read() >= 0 })
    }
}
