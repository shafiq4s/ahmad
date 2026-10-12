package com.ahmad.quran

import androidx.lifecycle.SavedStateHandle
import com.ahmad.quran.data.AyahId
import com.ahmad.quran.data.QuranRepository
import com.ahmad.quran.data.ReadingMetadata
import com.ahmad.quran.mushaf.svg.QuranSvgEdition
import com.ahmad.quran.ui.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import android.os.Looper
import java.io.File

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk=[23,36])
class ReadingExperienceStateTest {
    private val app get()=RuntimeEnvironment.getApplication()
    private fun await(condition:()->Boolean) {
        val end=System.nanoTime()+20_000_000_000L
        while(!condition() && System.nanoTime()<end) { shadowOf(Looper.getMainLooper()).idle();Thread.sleep(10) }
        assertTrue(condition())
    }
    @Test fun localOptionsTimestampAndUniquePageBookmarksRoundTripWithoutCloud() {
        val p=ReadingPreferences(app)
        assertFalse(p.keepScreenAwake);assertFalse(p.autoResumeReading);assertTrue(p.showReaderBarsOnOpen)
        p.setKeepScreenAwake(true);p.setAutoResumeReading(true);p.setShowReaderBarsOnOpen(false)
        val last=ReadingLocation(ReaderMode.PAGE,5,78,121,QuranSvgEdition.ID)
        p.saveLocation(last);assertTrue(p.lastReadAtMillis>0);assertEquals(last,p.lastLocation)
        p.saveBookmark(ReadingBookmark(121,5,78,createdAtMillis=100))
        p.saveBookmark(ReadingBookmark(604,114,6,createdAtMillis=200))
        p.saveBookmark(ReadingBookmark(121,5,79,createdAtMillis=300))
        val restored=ReadingPreferences(app)
        assertTrue(restored.keepScreenAwake);assertTrue(restored.autoResumeReading);assertFalse(restored.showReaderBarsOnOpen)
        assertEquals(listOf(121,604),restored.bookmarks.map { it.pageNumber })
        assertEquals(79,restored.bookmarks.first().ayahNumber)
        restored.removeBookmark(QuranSvgEdition.ID,121)
        assertEquals(604,restored.bookmarks.single().pageNumber);assertEquals(last,restored.lastLocation)
    }
    @Test fun invalidBookmarkIdsAndCorruptEntriesCannotBecomeReadingTargets() {
        val p=ReadingPreferences(app)
        assertThrows(IllegalArgumentException::class.java) { p.saveBookmark(ReadingBookmark(605,1)) }
        assertThrows(IllegalArgumentException::class.java) { p.saveBookmark(ReadingBookmark(604,114,7)) }
        assertThrows(IllegalArgumentException::class.java) { p.saveBookmark(ReadingBookmark(604,112,5)) }
        assertThrows(IllegalArgumentException::class.java) { p.saveBookmark(ReadingBookmark(1,1,editionId="other-edition")) }
        app.getSharedPreferences("reading",0).edit().putString("bookmarks","""[{"page":604,"surah":114,"ayah":7,"edition":"${QuranSvgEdition.ID}","createdAtMillis":1},{"page":1,"surah":1,"ayah":1,"edition":"${QuranSvgEdition.ID}","createdAtMillis":2}]""").commit()
        assertEquals(listOf(1),p.bookmarks.map { it.pageNumber })
    }
    @Test fun allThirtyJuzTargetsUseCanonicalStartIdentityAndCurrentArtworkPageMap() {
        val starts=ReadingMetadata.getJuzStarts(app);val map=QuranSvgEdition(app)
        val model=MainViewModel(app,SavedStateHandle())
        await { !model.state.value.loading }
        QuranRepository.open(app).use { repo ->
            for(n in 1..30) {
                val first=repo.getJuz(QuranRepository.DEFAULT_EDITION,n).first().id
                assertEquals(first,starts[n]);assertEquals(map.pageForAyah(first),model.state.value.juzPages[n])
            }
        }
        assertEquals(AyahId(5,82),starts[7]);assertEquals(121,model.state.value.juzPages[7])
        model.openJuz(7);await { !model.state.value.loading };assertEquals(121,model.state.value.pageNumber)
    }
    @Test fun diskClearTouchesOnlyDownloadedEditionArtwork() {
        val p=ReadingPreferences(app);p.saveLocation(ReadingLocation(ReaderMode.PAGE,1,1,1,QuranSvgEdition.ID));p.saveBookmark(ReadingBookmark(1,1,1))
        val cache=File(app.cacheDir,"mushaf-pages/${QuranSvgEdition.ID}").apply { mkdirs() }
        File(cache,"121.svg").writeBytes(ByteArray(123));File(cache,".part").writeBytes(ByteArray(10))
        val other=File(app.cacheDir,"unrelated-cache").apply { writeText("retain") }
        QuranRepository.open(app).use { assertEquals(6236,it.getSurahs().sumOf { s -> s.ayahCount }) }
        val bytes=app.assets.open("mushaf-svg/001.svg").use { it.readBytes() }
        val storage=DownloadedPageCache(app);assertEquals(133L,storage.sizeBytes());assertTrue(storage.clear());assertEquals(0L,storage.sizeBytes())
        assertTrue(other.exists());assertEquals(1,p.bookmarks.single().pageNumber);assertEquals(1,p.lastLocation!!.pageNumber)
        assertArrayEquals(bytes,app.assets.open("mushaf-svg/001.svg").use { it.readBytes() })
        QuranRepository.open(app).use { assertEquals(7,it.getSurah(1).size) }
    }
}
