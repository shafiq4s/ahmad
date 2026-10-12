package com.ahmad.quran.mushaf.svg

import android.content.Context
import com.ahmad.quran.data.AyahId
import com.ahmad.quran.data.MushafEdition
import org.json.JSONObject

/** Small audited catalog only: opening the index never parses any SVG document. */
class QuranSvgEdition(context: Context) : com.ahmad.quran.mushaf.MushafPageMap {
    private val catalog = context.assets.open("mushaf-svg/pages.json").bufferedReader().use { JSONObject(it.readText()) }
    override val edition = MushafEdition(ID, "مصحف المدينة · حفص", com.ahmad.quran.data.QuranRepository.DEFAULT_TEXT_VERSION, 604, COMMIT)
    init {
        require(catalog.getString("editionId") == ID && catalog.getString("commit") == COMMIT)
        require(catalog.getJSONArray("pages").length() == 604)
    }
    override fun firstPage(surahNumber: Int): Int {
        require(surahNumber in 1..114)
        return catalog.getJSONObject("surahFirstPages").getInt(surahNumber.toString())
    }
    override fun firstAyah(pageNumber: Int): AyahId = identity(page(pageNumber).getString("firstAyahId"))
    override fun lastAyah(pageNumber: Int): AyahId = identity(page(pageNumber).getString("lastAyahId"))
    override fun pageForAyah(id: AyahId): Int? = (1..604).firstOrNull {
        val start=firstAyah(it);val end=lastAyah(it)
        val key=id.surahNumber*1000+id.ayahNumber
        key >= start.surahNumber*1000+start.ayahNumber && key <= end.surahNumber*1000+end.ayahNumber
    }
    private fun page(number: Int): JSONObject {
        require(number in 1..604)
        return catalog.getJSONArray("pages").getJSONObject(number-1).also { require(it.getInt("pageNumber")==number) }
    }
    private fun identity(value: String): AyahId {
        val parts=value.split(':')
        return AyahId(parts[0].toInt(),parts[1].toInt())
    }
    companion object {
        const val ID="quran_ws_hafs_kfqc_b4155f0"
        const val COMMIT="b4155f07e4aea087d2c458069acff9f7c7e749f6"
    }
}
