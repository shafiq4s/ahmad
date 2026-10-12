package com.ahmad.quran.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ahmad.quran.data.AyahId
import com.ahmad.quran.data.Ayah
import com.ahmad.quran.data.MushafEdition
import com.ahmad.quran.data.QuranRepository
import com.ahmad.quran.data.ReadingMetadata
import com.ahmad.quran.data.Surah
import com.ahmad.quran.mushaf.svg.QuranSvgEdition
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

enum class Screen { HOME, QURAN, READER, SETTINGS, BOOKMARKS, SEARCH, HISTORY, ABOUT, SUPPORT }
enum class ReaderMode { SURAH, PAGE }

data class AyahDetails(val ayah: Ayah, val surahName: String, val juzNumber: Int?)

data class QuranUiState(
    val screen: Screen = Screen.HOME,
    val surahs: List<Surah> = emptyList(),
    val revelationTypes: Map<Int, String> = emptyMap(),
    val edition: MushafEdition? = null,
    val mode: ReaderMode = ReaderMode.SURAH,
    val surahNumber: Int = 1,
    val pageNumber: Int = 1,
    val juzNumber: Int? = null,
    val anchor: AyahId = AyahId(1, 1),
    val lastLocation: ReadingLocation? = null,
    val darkMode: Boolean = false,
    val notice: String = "",
    val loading: Boolean = true,
    val error: Boolean = false,
    val bookmarks: List<ReadingBookmark> = emptyList(),
    val history: List<ReadingHistoryEntry> = emptyList(),
    val juzPages: Map<Int, Int> = emptyMap(),
    val keepScreenAwake: Boolean = false,
    val autoResumeReading: Boolean = false,
    val showReaderBarsOnOpen: Boolean = true,
    val targetAyah: AyahId? = null,
    val searchQuery: String = "",
    val searchResults: List<Ayah> = emptyList(),
    val searchLoading: Boolean = false,
    val searchHasMore: Boolean = false,
    val searchError: Boolean = false,
)

class MainViewModel(application: Application, private val savedState: SavedStateHandle) : AndroidViewModel(application) {
    private val preferences = ReadingPreferences(application)
    private val repository = lazy { QuranRepository.open(application) }
    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private var svgEdition: QuranSvgEdition? = null
    private val mutableState = MutableStateFlow(QuranUiState(
        screen = Screen.entries.firstOrNull { it.name == savedState.get<String>("readingScreen") } ?: Screen.HOME,
        mode = ReaderMode.entries.firstOrNull { it.name == savedState.get<String>("readerMode") } ?: ReaderMode.SURAH,
        surahNumber = (savedState.get<Int>("surahNumber") ?: 1).coerceIn(1, 114),
        pageNumber = (savedState.get<Int>("pageNumber") ?: 1).coerceIn(1, 604),
        anchor = AyahId(savedState.get<Int>("anchorSurah") ?: 1, savedState.get<Int>("anchorAyah") ?: 1),
        lastLocation = preferences.lastLocation,
        darkMode = preferences.darkMode,
        bookmarks = preferences.bookmarks,
        history = preferences.history,
        keepScreenAwake = preferences.keepScreenAwake,
        autoResumeReading = preferences.autoResumeReading,
        showReaderBarsOnOpen = preferences.showReaderBarsOnOpen,
        targetAyah = savedState.get<Int>("targetSurah")?.let { s -> savedState.get<Int>("targetAyah")?.let { a -> AyahId(s,a) } },
        searchQuery = savedState.get<String>("searchQuery").orEmpty(),
    ))
    val state = mutableState.asStateFlow()

    private var autoResumePending = savedState.get<String>("readingScreen") == null && preferences.autoResumeReading

    init {
        load()
        if(mutableState.value.searchQuery.isNotBlank()) search(mutableState.value.searchQuery)
    }

    fun navigateTo(screen: Screen) {
        val current = mutableState.value.screen
        if (screen == Screen.SETTINGS && current !in listOf(Screen.SETTINGS, Screen.ABOUT, Screen.SUPPORT))
            savedState["settingsReturnScreen"] = current.name
        if (screen != Screen.READER) clearTarget()
        savedState["readingScreen"] = screen.name
        mutableState.value = mutableState.value.copy(screen = screen)
    }

    fun navigateBack() {
        val destination = when (mutableState.value.screen) {
            Screen.READER -> Screen.entries.firstOrNull { it.name == savedState.get<String>("readerReturnScreen") && it != Screen.READER } ?: Screen.HOME
            Screen.SETTINGS -> Screen.entries.firstOrNull { it.name == savedState.get<String>("settingsReturnScreen") && it !in listOf(Screen.SETTINGS, Screen.ABOUT, Screen.SUPPORT) } ?: Screen.HOME
            Screen.ABOUT, Screen.SUPPORT -> Screen.SETTINGS
            else -> Screen.HOME
        }
        navigateTo(destination)
    }

    fun openAyah(id: AyahId): Boolean {
        val surah = mutableState.value.surahs.firstOrNull { it.surahNumber == id.surahNumber } ?: return false
        if (id.ayahNumber !in 1..surah.ayahCount) return false
        val map = svgEdition ?: return false
        val page = map.pageForAyah(id) ?: return false
        open(ReaderMode.PAGE, id.surahNumber, page, map.firstAyah(page), id)
        return true
    }
    fun consumeTarget(id: AyahId) {
        if(mutableState.value.targetAyah == id) clearTarget()
    }
    private fun clearTarget() {
        savedState.remove<Int>("targetSurah");savedState.remove<Int>("targetAyah")
        mutableState.value = mutableState.value.copy(targetAyah = null)
    }

    fun search(query: String) {
        searchJob?.cancel()
        savedState["searchQuery"] = query
        mutableState.value = mutableState.value.copy(searchQuery=query,searchResults=emptyList(),searchLoading=query.isNotBlank(),searchHasMore=false,searchError=false)
        if(query.isNotBlank()) runSearch(query,0,true)
    }
    fun loadMoreSearchResults() {
        val current=mutableState.value
        if(!current.searchLoading && (current.searchHasMore || current.searchError)) {
            mutableState.value=current.copy(searchLoading=true,searchError=false)
            runSearch(current.searchQuery,current.searchResults.size,false)
        }
    }
    private fun runSearch(query: String, offset: Int, debounce: Boolean) {
        searchJob=viewModelScope.launch {
            try {
                if(debounce) delay(250)
                val rows=withContext(Dispatchers.IO) {
                    val request=currentCoroutineContext()
                    repository.value.searchAyahs(query,offset,51) { request.ensureActive() }
                }
                mutableState.value=mutableState.value.copy(searchResults=mutableState.value.searchResults+rows.take(50),
                    searchLoading=false,searchHasMore=rows.size>50,searchError=false)
            } catch(error: Exception) {
                if(error is CancellationException) throw error
                mutableState.value=mutableState.value.copy(searchLoading=false,searchError=true)
            }
        }
    }

    fun firstPageForSurah(surahNumber: Int): Int? = svgEdition?.firstPage(surahNumber)

    fun openSurah(surahNumber: Int) {
        require(surahNumber in 1..114)
        val map = svgEdition ?: return
        open(ReaderMode.PAGE, surahNumber, map.firstPage(surahNumber), AyahId(surahNumber, 1))
    }

    fun openPage(pageNumber: Int) {
        val edition = mutableState.value.edition ?: return
        if (pageNumber !in 1..edition.pageCount) return
        open(ReaderMode.PAGE, mutableState.value.surahNumber, pageNumber, checkNotNull(svgEdition).firstAyah(pageNumber))
    }

    fun resumeReading() {
        val location = preferences.lastLocation ?: return
        val map = svgEdition ?: return
        val id = AyahId(location.surahNumber, location.ayahNumber)
        // Older text-reader locations belong to Tanzil pagination; migrate by canonical identity.
        val page = if (location.editionId == QuranSvgEdition.ID) location.pageNumber else map.pageForAyah(id) ?: return
        val recent = preferences.history.firstOrNull { it.pageNumber == page && it.editionId == location.editionId }
        openSavedPosition(page, location.surahNumber, if (location.editionId == QuranSvgEdition.ID) recent?.ayahNumber else location.ayahNumber)
    }

    fun openBookmark(bookmark: ReadingBookmark) = openSavedPosition(bookmark.pageNumber, bookmark.surahNumber, bookmark.ayahNumber)
    fun openHistory(entry: ReadingHistoryEntry) = openSavedPosition(entry.pageNumber, entry.surahNumber, entry.ayahNumber)

    private fun openSavedPosition(page: Int, surah: Int, ayah: Int?) {
        val map = svgEdition ?: return
        if (page !in 1..map.edition.pageCount) return
        val id = ayah?.let { AyahId(surah, it) }
        // The saved edition page takes precedence for an ayah spanning multiple pages.
        val target = id?.takeIf { candidate ->
            val first = map.firstAyah(page); val last = map.lastAyah(page)
            val key = candidate.surahNumber * 1000 + candidate.ayahNumber
            mutableState.value.surahs.any { it.surahNumber == candidate.surahNumber && candidate.ayahNumber in 1..it.ayahCount } &&
                key in (first.surahNumber * 1000 + first.ayahNumber)..(last.surahNumber * 1000 + last.ayahNumber)
        }
        if (page in 1..map.edition.pageCount) open(ReaderMode.PAGE, surah, page, map.firstAyah(page), target)
    }

    fun openJuz(juzNumber: Int) {
        mutableState.value.juzPages[juzNumber]?.let(::openPage)
    }
    fun toggleBookmark(pageNumber: Int, id: AyahId? = null) {
        require(pageNumber in 1..604)
        if (mutableState.value.bookmarks.any { it.editionId == QuranSvgEdition.ID && it.pageNumber == pageNumber }) {
            removeBookmark(QuranSvgEdition.ID, pageNumber)
        } else {
            val contextId = id ?: checkNotNull(svgEdition).firstAyah(pageNumber)
            val surah = mutableState.value.surahs.first { it.surahNumber == contextId.surahNumber }
            require(contextId.ayahNumber in 1..surah.ayahCount)
            preferences.saveBookmark(ReadingBookmark(pageNumber, contextId.surahNumber, id?.ayahNumber))
            mutableState.value = mutableState.value.copy(bookmarks = preferences.bookmarks)
        }
    }
    fun removeBookmark(editionId: String, pageNumber: Int) {
        preferences.removeBookmark(editionId, pageNumber)
        mutableState.value = mutableState.value.copy(bookmarks = preferences.bookmarks)
    }
    fun setKeepScreenAwake(enabled: Boolean) {
        preferences.setKeepScreenAwake(enabled);mutableState.value=mutableState.value.copy(keepScreenAwake=enabled)
    }
    fun setAutoResumeReading(enabled: Boolean) {
        preferences.setAutoResumeReading(enabled);mutableState.value=mutableState.value.copy(autoResumeReading=enabled)
    }
    fun setShowReaderBarsOnOpen(enabled: Boolean) {
        preferences.setShowReaderBarsOnOpen(enabled);mutableState.value=mutableState.value.copy(showReaderBarsOnOpen=enabled)
    }
    private fun open(mode: ReaderMode, surah: Int, page: Int, anchor: AyahId, target: AyahId? = null) {
        if(mutableState.value.screen != Screen.READER) savedState["readerReturnScreen"] = mutableState.value.screen.name
        savedState["targetSurah"] = target?.surahNumber
        savedState["targetAyah"] = target?.ayahNumber
        savedState["readerMode"] = mode.name
        savedState["surahNumber"] = surah
        savedState["pageNumber"] = page
        saveAnchor(anchor)
        mutableState.value = mutableState.value.copy(mode = mode, surahNumber = surah, pageNumber = page, anchor = anchor, targetAyah=target)
        navigateTo(Screen.READER)
        load()
    }

    fun recordPageDisplayed(pageNumber: Int, selectedAyah: AyahId? = null) {
        val current=mutableState.value
        if (current.screen!=Screen.READER || current.loading || current.error || current.pageNumber!=pageNumber) return
        if (selectedAyah != null && current.surahs.none { it.surahNumber == selectedAyah.surahNumber && selectedAyah.ayahNumber in 1..it.ayahCount }) return
        val recent = preferences.history.firstOrNull { it.pageNumber == pageNumber }
        val selection = selectedAyah ?: recent?.ayahNumber?.let { AyahId(recent.surahNumber, it) }
        val id=selection ?: checkNotNull(svgEdition).firstAyah(pageNumber)
        val location=ReadingLocation(ReaderMode.PAGE,id.surahNumber,id.ayahNumber,pageNumber,QuranSvgEdition.ID)
        preferences.saveLocation(location)
        preferences.recordHistory(ReadingHistoryEntry(pageNumber, id.surahNumber, selection?.ayahNumber))
        mutableState.value=current.copy(lastLocation=location, history=preferences.history)
    }

    fun setDarkMode(enabled: Boolean) {
        preferences.setDarkMode(enabled)
        mutableState.value = mutableState.value.copy(darkMode = enabled)
    }
    fun retry() = load()

    suspend fun getAyahDetails(id: AyahId): AyahDetails = withContext(Dispatchers.IO) {
        val ayah = checkNotNull(repository.value.getAyah(id.surahNumber, id.ayahNumber))
        val surah = mutableState.value.surahs.first { it.surahNumber == id.surahNumber }
        // Canonical juz membership, never a Tanzil page lookup.
        AyahDetails(ayah, surah.name, repository.value.getJuzForAyah(
            QuranRepository.DEFAULT_EDITION, id.surahNumber, id.ayahNumber,
        ))
    }
    private fun saveAnchor(anchor: AyahId) {
        savedState["anchorSurah"] = anchor.surahNumber
        savedState["anchorAyah"] = anchor.ayahNumber
    }

    override fun onCleared() {
        loadJob?.cancel()
        searchJob?.cancel()
        if (repository.isInitialized()) repository.value.close()
        super.onCleared()
    }

    private fun load() {
        loadJob?.cancel()
        val selection = mutableState.value
        mutableState.value = selection.copy(loading = true, error = false)
        loadJob = viewModelScope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) {
                    with(repository.value) {
                        val repo=this
                        val surahs = selection.surahs.ifEmpty { repo.getSurahs() }
                        val map = svgEdition ?: QuranSvgEdition(getApplication()).also { svgEdition=it }
                        val edition = map.edition
                        val juzPages = selection.juzPages.ifEmpty { ReadingMetadata.getJuzStarts(getApplication()).mapValues { (_, id) ->
                            checkNotNull(map.pageForAyah(id))
                        } }
                        val types = selection.revelationTypes.ifEmpty { ReadingMetadata.getRevelationTypes(getApplication()) }
                        val page = if (selection.mode==ReaderMode.SURAH && selection.screen==Screen.READER)
                            map.pageForAyah(selection.anchor) ?: map.firstPage(selection.surahNumber)
                            else selection.pageNumber.coerceIn(1,604)
                        val anchor = map.firstAyah(page)
                        // Juz is canonical metadata; this does not consult Tanzil PageStart for layout.
                        val juz = repo.getJuzForAyah(QuranRepository.DEFAULT_EDITION, anchor.surahNumber, anchor.ayahNumber)
                        val notice=selection.notice.ifEmpty {
                            repo.getNotice()+"\n\n"+getApplication<Application>().assets.open("mushaf-svg/NOTICE.md").bufferedReader().use { it.readText() }
                        }
                        selection.copy(surahs = surahs, revelationTypes = types, juzPages = juzPages, edition = edition, anchor = anchor,
                            mode=ReaderMode.PAGE, pageNumber = page, juzNumber = juz, notice = notice, loading = false)
                    }
                }
                savedState["pageNumber"] = loaded.pageNumber
                saveAnchor(loaded.anchor)
                mutableState.value = mutableState.value.copy(
                    surahs = loaded.surahs, revelationTypes = loaded.revelationTypes, juzPages = loaded.juzPages, edition = loaded.edition,
                    anchor = loaded.anchor, pageNumber = loaded.pageNumber, juzNumber = loaded.juzNumber,
                    mode = ReaderMode.PAGE,
                    notice = loaded.notice, loading = false, error = false,
                )
                if (autoResumePending) {
                    autoResumePending = false
                    resumeReading()
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableState.value = mutableState.value.copy(loading = false, error = true)
            }
        }
    }
}
