package com.ahmad.quran.data

data class Surah(val surahNumber: Int, val name: String, val ayahCount: Int)
data class AyahId(val surahNumber: Int, val ayahNumber: Int)
data class Ayah(val surahNumber: Int, val ayahNumber: Int, val text: String) {
    val id: AyahId get() = AyahId(surahNumber, ayahNumber)
}
data class TextVersion(val textVersionId: String, val name: String, val version: String, val sourceUrl: String, val sourceSha256: String)
data class MushafEdition(val editionId: String, val name: String, val textVersionId: String, val pageCount: Int, val metadataVersion: String)
data class QuranPage(val editionId: String, val pageNumber: Int, val ayahs: List<Ayah>)
