package com.ahmad.quran.ui

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import com.ahmad.quran.data.QuranRepository
import com.ahmad.quran.mushaf.svg.QuranSvgEdition

data class ReadingLocation(
    val mode: ReaderMode,
    val surahNumber: Int,
    val ayahNumber: Int,
    val pageNumber: Int,
    val editionId: String = QuranRepository.DEFAULT_EDITION,
)

data class ReadingBookmark(
    val pageNumber: Int,
    val surahNumber: Int,
    val ayahNumber: Int? = null,
    val editionId: String = QuranSvgEdition.ID,
    val createdAtMillis: Long = System.currentTimeMillis(),
)

data class ReadingHistoryEntry(
    val pageNumber: Int,
    val surahNumber: Int,
    val ayahNumber: Int? = null,
    val editionId: String = QuranSvgEdition.ID,
    val readAtMillis: Long = System.currentTimeMillis(),
)

class ReadingPreferences(context: Context) {
    private val ayahCounts by lazy { com.ahmad.quran.data.ReadingMetadata.getSurahAyahCounts(context.applicationContext) }
    private val preferences = context.applicationContext.getSharedPreferences("reading", Context.MODE_PRIVATE)
    val textSizeSp: Int get() = preferences.getInt("textSizeSp", 28).coerceIn(20, 40)
    // Hidden until real recitation playback exists; taps currently only select an ayah.
    val recitationOnAyahTap: Boolean get() = preferences.getBoolean("recitationOnAyahTap", false)
    val darkMode: Boolean get() = preferences.getBoolean("darkMode", false)
    val lastReadAtMillis: Long get() = preferences.getLong("lastReadAtMillis", 0)
    val keepScreenAwake: Boolean get() = preferences.getBoolean("keepScreenAwake", false)
    val autoResumeReading: Boolean get() = preferences.getBoolean("autoResumeReading", false)
    val showReaderBarsOnOpen: Boolean get() = preferences.getBoolean("showReaderBarsOnOpen", true)
    val bookmarks: List<ReadingBookmark> get() = runCatching {
        val rows = JSONArray(preferences.getString("bookmarks", "[]"))
        (0 until rows.length()).mapNotNull { i ->
            runCatching {
                val r = rows.getJSONObject(i)
                ReadingBookmark(r.getInt("page"), r.getInt("surah"), if(r.isNull("ayah")) null else r.getInt("ayah"),
                    r.getString("edition"), r.getLong("createdAtMillis")).takeIf {
                    it.editionId == QuranSvgEdition.ID && it.pageNumber in 1..604 && it.surahNumber in 1..114 &&
                        (it.ayahNumber == null || it.ayahNumber in 1..(ayahCounts[it.surahNumber] ?: 0)) && it.createdAtMillis >= 0
                }
            }.getOrNull()
        }.distinctBy { it.editionId to it.pageNumber }.sortedByDescending { it.createdAtMillis }
    }.getOrDefault(emptyList())
    val history: List<ReadingHistoryEntry> get() = runCatching {
        val rows = JSONArray(preferences.getString("history", "[]"))
        (0 until rows.length()).mapNotNull { i ->
            runCatching {
                val r = rows.getJSONObject(i)
                ReadingHistoryEntry(r.getInt("page"), r.getInt("surah"),
                    if (r.isNull("ayah")) null else r.getInt("ayah"), r.getString("edition"), r.getLong("readAtMillis"))
                    .takeIf { it.editionId == QuranSvgEdition.ID && it.pageNumber in 1..604 &&
                        it.surahNumber in 1..114 && it.readAtMillis >= 0 &&
                        (it.ayahNumber == null || it.ayahNumber in 1..(ayahCounts[it.surahNumber] ?: 0)) }
            }.getOrNull()
        }.sortedByDescending { it.readAtMillis }.distinctBy { it.editionId to it.pageNumber }.take(10)
    }.getOrDefault(emptyList())

    // One recent position per page. Revisiting moves it to the front and retains the latest selection.
    fun recordHistory(entry: ReadingHistoryEntry) {
        require(entry.editionId == QuranSvgEdition.ID && entry.pageNumber in 1..604 && entry.surahNumber in 1..114)
        require(entry.readAtMillis >= 0 && (entry.ayahNumber == null || entry.ayahNumber in 1..(ayahCounts[entry.surahNumber] ?: 0)))
        val items = (listOf(entry) + history.filterNot { it.editionId == entry.editionId && it.pageNumber == entry.pageNumber }).take(10)
        val rows = JSONArray()
        items.forEach { rows.put(JSONObject().put("edition", it.editionId).put("page", it.pageNumber)
            .put("surah", it.surahNumber).put("ayah", it.ayahNumber ?: JSONObject.NULL).put("readAtMillis", it.readAtMillis)) }
        preferences.edit { putString("history", rows.toString()) }
    }
    val lastLocation: ReadingLocation? get() {
        val mode = ReaderMode.entries.firstOrNull { it.name == preferences.getString("mode", null) } ?: return null
        val surah = preferences.getInt("surah", 0)
        val ayah = preferences.getInt("ayah", 0)
        val page = preferences.getInt("page", 0)
        val edition = preferences.getString("edition", null) ?: return null
        if (surah !in 1..114 || ayah !in 1..(ayahCounts[surah] ?: 0) || page !in 1..604 ||
            edition !in listOf(QuranRepository.DEFAULT_EDITION,QuranSvgEdition.ID)) return null
        return ReadingLocation(mode, surah, ayah, page, edition)
    }
    fun saveLocation(location: ReadingLocation) {
        preferences.edit {
            putLong("lastReadAtMillis", System.currentTimeMillis())
            putString("mode", location.mode.name)
            putInt("surah", location.surahNumber)
            putInt("ayah", location.ayahNumber)
            putInt("page", location.pageNumber)
            putString("edition", location.editionId)
        }
    }
    fun setKeepScreenAwake(enabled: Boolean) { preferences.edit { putBoolean("keepScreenAwake", enabled) } }
    fun setAutoResumeReading(enabled: Boolean) { preferences.edit { putBoolean("autoResumeReading", enabled) } }
    fun setShowReaderBarsOnOpen(enabled: Boolean) { preferences.edit { putBoolean("showReaderBarsOnOpen", enabled) } }
    fun saveBookmark(bookmark: ReadingBookmark) {
        require(bookmark.pageNumber in 1..604 && bookmark.surahNumber in 1..114 && bookmark.editionId == QuranSvgEdition.ID)
        require(bookmark.createdAtMillis >= 0 && (bookmark.ayahNumber == null || bookmark.ayahNumber in 1..(ayahCounts[bookmark.surahNumber] ?: 0)))
        writeBookmarks(bookmarks.filterNot { it.editionId == bookmark.editionId && it.pageNumber == bookmark.pageNumber } + bookmark)
    }
    fun removeBookmark(editionId: String, pageNumber: Int) {
        writeBookmarks(bookmarks.filterNot { it.editionId == editionId && it.pageNumber == pageNumber })
    }
    private fun writeBookmarks(items: List<ReadingBookmark>) {
        val rows = JSONArray()
        items.forEach { rows.put(JSONObject().put("edition", it.editionId).put("page", it.pageNumber)
            .put("surah", it.surahNumber).put("ayah", it.ayahNumber ?: JSONObject.NULL).put("createdAtMillis", it.createdAtMillis)) }
        preferences.edit { putString("bookmarks", rows.toString()) }
    }
    fun setTextSize(size: Int) { preferences.edit { putInt("textSizeSp", size.coerceIn(20, 40)) } }
    fun setRecitationOnAyahTap(enabled: Boolean) { preferences.edit { putBoolean("recitationOnAyahTap", enabled) } }
    fun setDarkMode(enabled: Boolean) { preferences.edit { putBoolean("darkMode", enabled) } }
}
