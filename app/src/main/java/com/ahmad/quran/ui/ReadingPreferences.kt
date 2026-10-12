package com.ahmad.quran.ui

import android.content.Context
import androidx.core.content.edit
import com.ahmad.quran.data.QuranRepository
import com.ahmad.quran.mushaf.svg.QuranSvgEdition

data class ReadingLocation(
    val mode: ReaderMode,
    val surahNumber: Int,
    val ayahNumber: Int,
    val pageNumber: Int,
    val editionId: String = QuranRepository.DEFAULT_EDITION,
)

class ReadingPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("reading", Context.MODE_PRIVATE)
    val textSizeSp: Int get() = preferences.getInt("textSizeSp", 28).coerceIn(20, 40)
    val darkMode: Boolean get() = preferences.getBoolean("darkMode", false)
    val lastLocation: ReadingLocation? get() {
        val mode = ReaderMode.entries.firstOrNull { it.name == preferences.getString("mode", null) } ?: return null
        val surah = preferences.getInt("surah", 0)
        val ayah = preferences.getInt("ayah", 0)
        val page = preferences.getInt("page", 0)
        val edition = preferences.getString("edition", null) ?: return null
        if (surah !in 1..114 || ayah !in 1..286 || page !in 1..604 ||
            edition !in listOf(QuranRepository.DEFAULT_EDITION,QuranSvgEdition.ID)) return null
        return ReadingLocation(mode, surah, ayah, page, edition)
    }
    fun saveLocation(location: ReadingLocation) {
        if (lastLocation == location) return
        preferences.edit {
            putString("mode", location.mode.name)
            putInt("surah", location.surahNumber)
            putInt("ayah", location.ayahNumber)
            putInt("page", location.pageNumber)
            putString("edition", location.editionId)
        }
    }
    fun setTextSize(size: Int) { preferences.edit { putInt("textSizeSp", size.coerceIn(20, 40)) } }
    fun setDarkMode(enabled: Boolean) { preferences.edit { putBoolean("darkMode", enabled) } }
}
