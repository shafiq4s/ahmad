package com.ahmad.quran.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale

class QuranRepository private constructor(private val database: SQLiteDatabase) : Closeable {
    val isReadOnly: Boolean get() = database.isReadOnly

    /** Read-only cursor scan; comparison folding never changes canonical storage or results. */
    fun searchAyahs(query: String, offset: Int = 0, limit: Int = 51, checkCancelled: () -> Unit = {}): List<Ayah> {
        require(offset >= 0 && limit in 1..101)
        val key = QuranSearch.key(query)
        if (key.isEmpty()) return emptyList()
        // ponytail: bounded 6236-row scan; add a separate search index only if measured latency warrants it.
        return database.rawQuery(
            "SELECT surahNumber,ayahNumber,text FROM ayah_texts WHERE textVersionId=? ORDER BY surahNumber,ayahNumber",
            arrayOf(DEFAULT_TEXT_VERSION),
        ).use { cursor ->
            buildList {
                var matched = 0
                while (cursor.moveToNext()) {
                    checkCancelled()
                    val text = cursor.getString(2)
                    if (QuranSearch.matches(text,key) && matched++ >= offset) {
                        add(Ayah(cursor.getInt(0), cursor.getInt(1), text))
                        if (size == limit) break
                    }
                }
            }
        }
    }

    fun getSurahs(): List<Surah> = database.rawQuery(
        "SELECT surahNumber,name,ayahCount FROM surahs ORDER BY surahNumber", null,
    ).use { cursor -> buildList { while (cursor.moveToNext()) add(Surah(cursor.getInt(0), cursor.getString(1), cursor.getInt(2))) } }

    fun getSurah(surahNumber: Int, textVersionId: String = DEFAULT_TEXT_VERSION): List<Ayah> {
        require(surahNumber in 1..114)
        return queryAyahs(
            "SELECT surahNumber,ayahNumber,text FROM ayah_texts WHERE textVersionId=? AND surahNumber=? ORDER BY ayahNumber",
            arrayOf(textVersionId, surahNumber.toString()),
        )
    }

    fun getAyah(surahNumber: Int, ayahNumber: Int, textVersionId: String = DEFAULT_TEXT_VERSION): Ayah? {
        require(surahNumber in 1..114 && ayahNumber > 0)
        return queryAyahs(
            "SELECT surahNumber,ayahNumber,text FROM ayah_texts WHERE textVersionId=? AND surahNumber=? AND ayahNumber=?",
            arrayOf(textVersionId, surahNumber.toString(), ayahNumber.toString()),
        ).firstOrNull()
    }

    fun getTextVersions(): List<TextVersion> = database.rawQuery(
        "SELECT textVersionId,name,version,sourceUrl,sourceSha256 FROM text_versions ORDER BY textVersionId", null,
    ).use { cursor -> buildList { while (cursor.moveToNext()) add(TextVersion(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4))) } }

    fun getMushafEditions(): List<MushafEdition> = database.rawQuery(
        "SELECT editionId,name,textVersionId,pageCount,metadataVersion FROM mushaf_editions ORDER BY editionId", null,
    ).use { cursor -> buildList { while (cursor.moveToNext()) add(MushafEdition(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getInt(3), cursor.getString(4))) } }

    fun getPage(editionId: String, pageNumber: Int): QuranPage? {
        require(pageNumber > 0)
        val range = database.rawQuery(
            "SELECT p.startOrdinal,p.endOrdinal,e.textVersionId FROM edition_pages p JOIN mushaf_editions e ON e.editionId=p.editionId WHERE p.editionId=? AND p.pageNumber=?",
            arrayOf(editionId, pageNumber.toString()),
        ).use { cursor -> if (cursor.moveToFirst()) Triple(cursor.getInt(0), cursor.getInt(1), cursor.getString(2)) else null } ?: return null
        return QuranPage(editionId, pageNumber, getRange(range.third, range.first, range.second))
    }

    fun getPageForAyah(editionId: String, surahNumber: Int, ayahNumber: Int): Int? = database.rawQuery(
        "SELECT p.pageNumber FROM edition_pages p JOIN ayahs a ON a.ordinal>=p.startOrdinal AND a.ordinal<p.endOrdinal WHERE p.editionId=? AND a.surahNumber=? AND a.ayahNumber=?",
        arrayOf(editionId, surahNumber.toString(), ayahNumber.toString()),
    ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else null }

    fun getJuzForAyah(editionId: String, surahNumber: Int, ayahNumber: Int): Int? = database.rawQuery(
        "SELECT b.number FROM edition_boundaries b JOIN ayahs a ON a.ordinal>=b.startOrdinal AND a.ordinal<b.endOrdinal WHERE b.editionId=? AND b.kind='JUZ' AND a.surahNumber=? AND a.ayahNumber=?",
        arrayOf(editionId, surahNumber.toString(), ayahNumber.toString()),
    ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else null }

    fun getJuz(editionId: String, juzNumber: Int): List<Ayah> {
        require(juzNumber in 1..30)
        val range = database.rawQuery(
            "SELECT b.startOrdinal,b.endOrdinal,e.textVersionId FROM edition_boundaries b JOIN mushaf_editions e ON e.editionId=b.editionId WHERE b.editionId=? AND b.kind='JUZ' AND b.number=?",
            arrayOf(editionId, juzNumber.toString()),
        ).use { cursor -> if (cursor.moveToFirst()) Triple(cursor.getInt(0), cursor.getInt(1), cursor.getString(2)) else null } ?: return emptyList()
        return getRange(range.third, range.first, range.second)
    }

    fun getNotice(): String = database.rawQuery("SELECT content FROM notices ORDER BY name DESC", null).use { cursor ->
        buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }.joinToString("\n\n")
    }

    private fun getRange(textVersionId: String, start: Int, end: Int): List<Ayah> = queryAyahs(
        "SELECT t.surahNumber,t.ayahNumber,t.text FROM ayahs a JOIN ayah_texts t ON t.surahNumber=a.surahNumber AND t.ayahNumber=a.ayahNumber WHERE t.textVersionId=? AND a.ordinal>=? AND a.ordinal<? ORDER BY a.ordinal",
        arrayOf(textVersionId, start.toString(), end.toString()),
    )

    private fun queryAyahs(sql: String, args: Array<String>): List<Ayah> = database.rawQuery(sql, args).use { cursor: Cursor ->
        buildList { while (cursor.moveToNext()) add(Ayah(cursor.getInt(0), cursor.getInt(1), cursor.getString(2))) }
    }

    override fun close() = database.close()

    companion object {
        const val DEFAULT_EDITION = CorpusInfo.EDITION_ID
        const val DEFAULT_TEXT_VERSION = CorpusInfo.TEXT_VERSION_ID

        @Synchronized
        fun open(context: Context): QuranRepository {
            val directory = context.applicationContext.noBackupFilesDir
            val destination = File(directory, CorpusInfo.FILE_NAME)
            if (!destination.exists() || sha256(destination) != CorpusInfo.SHA256) {
                val temporary = File.createTempFile("quran-corpus-", ".tmp", directory)
                try {
                    context.assets.open(CorpusInfo.FILE_NAME).use { input ->
                        FileOutputStream(temporary).use { output ->
                            input.copyTo(output)
                            output.fd.sync()
                        }
                    }
                    check(sha256(temporary) == CorpusInfo.SHA256) { "Bundled corpus digest mismatch." }
                    check(temporary.renameTo(destination)) { "Could not install bundled corpus atomically." }
                } finally {
                    temporary.delete()
                }
            }
            destination.setReadOnly()
            val database = SQLiteDatabase.openDatabase(
                destination.absolutePath, null, SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS,
            )
            try {
                check(database.version == CorpusInfo.SCHEMA_VERSION && database.isReadOnly) { "Unsupported corpus schema or access mode." }
                database.execSQL("PRAGMA query_only=ON")
                return QuranRepository(database)
            } catch (error: Exception) {
                database.close()
                throw error
            }
        }

        internal fun sha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }
        }
    }
}
