package com.ahmad.quran

import android.os.Looper
import android.provider.Settings
import androidx.lifecycle.SavedStateHandle
import com.ahmad.quran.data.*
import com.ahmad.quran.mushaf.svg.QuranSvgEdition
import com.ahmad.quran.ui.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.CancellationException

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[23,36])
class OfflineSearchTest {
    private val app get()=RuntimeEnvironment.getApplication()
    private fun await(condition:()->Boolean) {
        val end=System.nanoTime()+15_000_000_000L
        while(!condition() && System.nanoTime()<end) { shadowOf(Looper.getMainLooper()).idle();Thread.sleep(10) }
        assertTrue("Local search/navigation did not complete",condition())
    }
    @Test fun hamdAndCommonUthmaniSpellingsMatchOfflineButReturnedTextIsVerbatim() {
        Settings.Global.putInt(app.contentResolver,Settings.Global.AIRPLANE_MODE_ON,1)
        QuranRepository.open(app).use { repo ->
            val results=repo.searchAyahs("الحمد لله")
            assertTrue(results.any { it.id==AyahId(1,2) })
            for(row in results) assertEquals(repo.getAyah(row.surahNumber,row.ayahNumber)!!.text,row.text)
            assertTrue(repo.searchAyahs("العالمين").any { it.id==AyahId(1,2) })
            assertTrue(repo.searchAyahs("الصلاة").isNotEmpty())
            assertTrue(repo.searchAyahs("إنه").isNotEmpty())
            assertTrue(repo.isReadOnly)
        }
    }
    @Test fun paginationIsOrderedCompleteAndQueriesAreLiteralRatherThanSqlPatterns() {
        QuranRepository.open(app).use { repo ->
            val first=repo.searchAyahs("الله",limit=50)
            val next=repo.searchAyahs("الله",offset=50,limit=50)
            assertEquals(50,first.size);assertEquals(50,next.size)
            assertEquals(repo.searchAyahs("الله",limit=100),first+next)
            assertEquals(100,(first+next).map { it.id }.distinct().size)
            for(q in listOf("", "   ","%","_","' OR 1=1 --","xyz-no-match")) assertTrue(repo.searchAyahs(q).isEmpty())
            assertThrows(IllegalArgumentException::class.java) { repo.searchAyahs("الله",offset=-1) }
            assertEquals(6236,repo.getSurahs().sumOf { it.ayahCount })
        }
    }
    @Test fun cancelledCursorClosesAndTheCanonicalRepositoryRemainsReadable() {
        QuranRepository.open(app).use { repo ->
            var checked=0
            assertThrows(CancellationException::class.java) {
                repo.searchAyahs("xyz-no-match") { if(++checked==5) throw CancellationException() }
            }
            assertEquals(7,repo.getSurah(1).size)
            assertEquals("الحمد لله",QuranSearch.key("  ٱلْحَمْدُ   لِلَّهِ  "))
        }
    }
    @Test fun directAyahTargetsValidateBoundsAndUseOnlyTheCurrentArtworkEdition() {
        val model=MainViewModel(app,SavedStateHandle());await { !model.state.value.loading }
        assertEquals(Screen.HOME,model.state.value.screen)
        val prefs=ReadingPreferences(app)
        for((id,page) in listOf(AyahId(1,1) to 1,AyahId(2,282) to 48,AyahId(5,78) to 121,AyahId(114,6) to 604)) {
            val previous=prefs.lastLocation
            assertTrue(model.openAyah(id));await { !model.state.value.loading }
            assertEquals(page,model.state.value.pageNumber);assertEquals(id,model.state.value.targetAyah)
            assertEquals(previous,prefs.lastLocation)
            model.recordPageDisplayed(page);assertEquals(page,prefs.lastLocation!!.pageNumber)
            model.consumeTarget(id);assertNull(model.state.value.targetAyah)
        }
        val before=model.state.value
        for(id in listOf(AyahId(0,1),AyahId(115,1),AyahId(1,0),AyahId(1,8),AyahId(2,287),AyahId(112,5),AyahId(114,7))) {
            assertFalse(model.openAyah(id));assertEquals(before,model.state.value)
        }
        model.toggleBookmark(604,AyahId(114,6));model.openPage(1);await { !model.state.value.loading }
        assertEquals(6,prefs.bookmarks.single().ayahNumber)
        assertEquals(QuranSvgEdition.ID,prefs.bookmarks.single().editionId)
    }
    @Test fun rapidQueryReplacementPublishesOnlyLatestResultsAndLoadMoreDoesNotRepeatRows() {
        val model=MainViewModel(app,SavedStateHandle());await { !model.state.value.loading }
        model.search("الله");model.search("الحمد لله")
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(300))
        await { !model.state.value.searchLoading }
        assertTrue(model.state.value.searchResults.any { it.id==AyahId(1,2) })
        assertEquals("الحمد لله",model.state.value.searchQuery)
        model.search("الله");shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(300))
        await { !model.state.value.searchLoading };assertEquals(50,model.state.value.searchResults.size)
        model.loadMoreSearchResults();model.loadMoreSearchResults()
        await { !model.state.value.searchLoading };assertEquals(100,model.state.value.searchResults.size)
        assertEquals(100,model.state.value.searchResults.map { it.id }.distinct().size)
    }
}
