package com.ahmad.quran

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import com.ahmad.quran.data.Ayah
import com.ahmad.quran.data.AyahId
import com.ahmad.quran.data.CorpusInfo
import com.ahmad.quran.data.QuranRepository
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23, 36])
class QuranRepositoryTest {
    private val sourceDirectory get() = File(checkNotNull(System.getProperty("quran.sourceDir")))
    private fun sourceAyahs(): List<Ayah> = File(sourceDirectory, "quran-uthmani.txt").readText(Charsets.UTF_8)
        .split('\n').map { it.removeSuffix("\r") }.filter { it.isNotEmpty() && !it.startsWith('#') }
        .map { line -> val parts = line.split('|', limit = 3); Ayah(parts[0].toInt(), parts[1].toInt(), parts[2]) }
    private fun boundaries(section: String): List<AyahId> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(sourceDirectory, "quran-data.xml"))
        val nodes = document.getElementsByTagName(section).item(0).childNodes
        return buildList {
            for (i in 0 until nodes.length) {
                val attributes = nodes.item(i).attributes ?: continue
                add(AyahId(attributes.getNamedItem("sura").nodeValue.toInt(), attributes.getNamedItem("aya").nodeValue.toInt()))
            }
        }
    }

    @Test fun everyStoredVerseAndSurahMatchesPinnedOfficialSourcesExactly() {
        QuranRepository.open(RuntimeEnvironment.getApplication()).use { repo ->
            val surahs = repo.getSurahs()
            assertEquals((1..114).toList(), surahs.map { it.surahNumber })
            assertEquals(6236, surahs.sumOf { it.ayahCount })
            val required = mapOf(1 to 7, 2 to 286, 3 to 200, 112 to 4, 114 to 6)
            required.forEach { (number, count) -> assertEquals(count, surahs[number - 1].ayahCount) }
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(sourceDirectory, "quran-data.xml"))
            val metadata = document.getElementsByTagName("sura")
            surahs.forEachIndexed { index, surah ->
                val attributes = metadata.item(index).attributes
                assertEquals(attributes.getNamedItem("name").nodeValue, surah.name)
                assertEquals(attributes.getNamedItem("ayas").nodeValue.toInt(), surah.ayahCount)
            }
            val expected = sourceAyahs()
            val actual = surahs.flatMap { repo.getSurah(it.surahNumber) }
            assertEquals(6236, actual.size)
            assertEquals(expected, actual)
            expected.zip(actual).forEach { (source, stored) -> assertArrayEquals(source.text.toByteArray(Charsets.UTF_8), stored.text.toByteArray(Charsets.UTF_8)) }
            assertEquals(expected.first(), repo.getAyah(1, 1))
            assertEquals(expected.last(), repo.getAyah(114, 6))
            assertNull(repo.getAyah(114, 7))
            assertEquals("6933e133dd56db778c801bf738848454e43648105a151e8d84d86a7cae39ec5f", QuranRepository.sha256(File(sourceDirectory, "quran-uthmani.txt")))
            assertEquals("8867c1d88191472adec9db694b3cd9f135b1a2ef580574d32cf888dcb22c5c7a", QuranRepository.sha256(File(sourceDirectory, "quran-data.xml")))
        }
    }

    @Test fun all604PagesAnd30JuzMatchMetadataAndCoverTheWholeQuranOnce() {
        QuranRepository.open(RuntimeEnvironment.getApplication()).use { repo ->
            val edition = repo.getMushafEditions().single()
            val textVersion = repo.getTextVersions().single()
            assertEquals(604, edition.pageCount)
            assertEquals(textVersion.textVersionId, edition.textVersionId)
            assertEquals("1.1", textVersion.version)
            assertEquals(CorpusInfo.TEXT_VERSION_ID, textVersion.textVersionId)
            val pageStarts = boundaries("pages")
            val allPages = (1..604).flatMap { number ->
                val page = checkNotNull(repo.getPage(edition.editionId, number))
                assertEquals(number, page.pageNumber)
                assertEquals(edition.editionId, page.editionId)
                assertEquals(pageStarts[number - 1], page.ayahs.first().id)
                page.ayahs.forEach { assertEquals(number, repo.getPageForAyah(edition.editionId, it.surahNumber, it.ayahNumber)) }
                page.ayahs
            }
            assertEquals(sourceAyahs(), allPages)
            val samples = mapOf(1 to AyahId(1, 1), 2 to AyahId(2, 1), 3 to AyahId(2, 6), 22 to AyahId(2, 142), 50 to AyahId(3, 1), 200 to AyahId(9, 80), 400 to AyahId(29, 31), 603 to AyahId(109, 1), 604 to AyahId(112, 1))
            samples.forEach { (page, id) -> assertEquals(id, repo.getPage(edition.editionId, page)!!.ayahs.first().id) }
            assertEquals(AyahId(114, 6), allPages.last().id)
            val juzStarts = boundaries("juzs")
            val allJuz = (1..30).flatMap { number -> repo.getJuz(edition.editionId, number).also { assertEquals(juzStarts[number - 1], it.first().id) } }
            assertEquals(sourceAyahs(), allJuz)
            for (juz in 1..30) {
                val verses = repo.getJuz(edition.editionId, juz)
                for (ayah in listOf(verses.first(), verses.last())) assertEquals(juz, repo.getJuzForAyah(edition.editionId, ayah.surahNumber, ayah.ayahNumber))
            }
            assertNull(repo.getJuzForAyah("unknown", 1, 1))
            assertNull(repo.getPage(edition.editionId, 605))
            assertNull(repo.getPage("unknown", 1))
            assertNull(repo.getPageForAyah("unknown", 1, 1))
            assertTrue(repo.getJuz("unknown", 1).isEmpty())
        }
    }

    @Test fun prebuiltFileIsCopiedVerbatimOpenedReadOnlyAndCannotBeWrittenThroughSql() {
        val app = RuntimeEnvironment.getApplication()
        val destination = File(app.noBackupFilesDir, CorpusInfo.FILE_NAME)
        QuranRepository.open(app).use { repo ->
            assertTrue(repo.isReadOnly)
            assertEquals(2_170_880L, destination.length())
            assertEquals(CorpusInfo.SHA256, QuranRepository.sha256(destination))
            assertTrue(repo.getNotice().contains("Tanzil Project"))
            assertTrue(repo.getNotice().contains("2007-2026"))
            assertTrue(repo.getNotice().contains("Creative Commons Attribution 3.0"))
        }
        SQLiteDatabase.openDatabase(destination.path, null, SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS).use { db ->
            assertTrue(db.isReadOnly)
            try {
                db.execSQL("DELETE FROM ayah_texts")
                fail("Read-only corpus accepted a write")
            } catch (_: SQLiteException) { }
            val names = db.rawQuery("PRAGMA table_info(ayahs)", null).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(1)) } }
            assertEquals(listOf("surahNumber", "ayahNumber", "ordinal"), names)
        }
        QuranRepository.open(app).use { assertEquals(6236, it.getSurahs().sumOf { surah -> surah.ayahCount }) }
        assertEquals(CorpusInfo.SHA256, QuranRepository.sha256(destination))
    }

    @Test fun damagedLocalCopyIsReplacedFromTheBundledVerifiedAsset() {
        val app = RuntimeEnvironment.getApplication()
        QuranRepository.open(app).close()
        val destination = File(app.noBackupFilesDir, CorpusInfo.FILE_NAME)
        destination.setWritable(true)
        destination.writeBytes(byteArrayOf(1, 2, 3))
        QuranRepository.open(app).use { assertNotNull(it.getAyah(114, 6)) }
        assertEquals(CorpusInfo.SHA256, QuranRepository.sha256(destination))
        assertFalse(app.noBackupFilesDir.listFiles()!!.any { it.name.startsWith("quran-corpus-") })
    }
}
