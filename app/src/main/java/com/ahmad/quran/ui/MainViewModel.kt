package com.ahmad.quran.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ahmad.quran.data.AyahId
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

enum class Screen { QURAN, READER, SETTINGS }
enum class ReaderMode { SURAH, PAGE }

data class QuranUiState(
    val screen: Screen = Screen.QURAN,
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
)

class MainViewModel(application: Application, private val savedState: SavedStateHandle) : AndroidViewModel(application) {
    private val preferences = ReadingPreferences(application)
    private val repository = lazy { QuranRepository.open(application) }
    private var loadJob: Job? = null
    private var svgEdition: QuranSvgEdition? = null
    private val mutableState = MutableStateFlow(QuranUiState(
        screen = Screen.entries.firstOrNull { it.name == savedState.get<String>("readingScreen") } ?: Screen.QURAN,
        mode = ReaderMode.entries.firstOrNull { it.name == savedState.get<String>("readerMode") } ?: ReaderMode.SURAH,
        surahNumber = (savedState.get<Int>("surahNumber") ?: 1).coerceIn(1, 114),
        pageNumber = (savedState.get<Int>("pageNumber") ?: 1).coerceIn(1, 604),
        anchor = AyahId(savedState.get<Int>("anchorSurah") ?: 1, savedState.get<Int>("anchorAyah") ?: 1),
        lastLocation = preferences.lastLocation,
        darkMode = preferences.darkMode,
    ))
    val state = mutableState.asStateFlow()

    init { load() }

    fun navigateTo(screen: Screen) {
        savedState["readingScreen"] = screen.name
        mutableState.value = mutableState.value.copy(screen = screen)
    }

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
        open(ReaderMode.PAGE, location.surahNumber, page, id)
    }

    private fun open(mode: ReaderMode, surah: Int, page: Int, anchor: AyahId) {
        savedState["readerMode"] = mode.name
        savedState["surahNumber"] = surah
        savedState["pageNumber"] = page
        saveAnchor(anchor)
        mutableState.value = mutableState.value.copy(mode = mode, surahNumber = surah, pageNumber = page, anchor = anchor)
        navigateTo(Screen.READER)
        load()
    }

    fun recordPageDisplayed(pageNumber: Int) {
        val current=mutableState.value
        if (current.screen!=Screen.READER || current.loading || current.error || current.pageNumber!=pageNumber) return
        val id=checkNotNull(svgEdition).firstAyah(pageNumber)
        val location=ReadingLocation(ReaderMode.PAGE,id.surahNumber,id.ayahNumber,pageNumber,QuranSvgEdition.ID)
        preferences.saveLocation(location)
        mutableState.value=current.copy(lastLocation=location)
    }

    fun setDarkMode(enabled: Boolean) {
        preferences.setDarkMode(enabled)
        mutableState.value = mutableState.value.copy(darkMode = enabled)
    }
    fun retry() = load()
    private fun saveAnchor(anchor: AyahId) {
        savedState["anchorSurah"] = anchor.surahNumber
        savedState["anchorAyah"] = anchor.ayahNumber
    }

    override fun onCleared() {
        loadJob?.cancel()
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
                        selection.copy(surahs = surahs, revelationTypes = types, edition = edition, anchor = anchor,
                            mode=ReaderMode.PAGE, pageNumber = page, juzNumber = juz, notice = notice, loading = false)
                    }
                }
                savedState["pageNumber"] = loaded.pageNumber
                saveAnchor(loaded.anchor)
                mutableState.value = mutableState.value.copy(
                    surahs = loaded.surahs, revelationTypes = loaded.revelationTypes, edition = loaded.edition,
                    anchor = loaded.anchor, pageNumber = loaded.pageNumber, juzNumber = loaded.juzNumber,
                    mode = ReaderMode.PAGE,
                    notice = loaded.notice, loading = false, error = false,
                )
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableState.value = mutableState.value.copy(loading = false, error = true)
            }
        }
    }
}
