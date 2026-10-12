package com.ahmad.quran.mushaf.svg

import android.graphics.RectF
import com.ahmad.quran.data.AyahId
import org.json.JSONArray

/** The only reader of Quran SVG's external per-page format. Unsupported syntax is rejected. */
class QuranSvgPageAdapter(private val ayahExists: (AyahId) -> Boolean) {
    fun parse(editionId: String, pageNumber: Int, viewBox: RectF, renderBox: RectF, json: String): PolygonMushafPage {
        require(editionId.isNotBlank() && pageNumber in 1..604)
        val array = JSONArray(json)
        val regions = (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val id = AyahId(exactInt(item.get("surahNumber")), exactInt(item.get("ayahNumber")))
            require(id.surahNumber in 1..114 && id.ayahNumber > 0 && ayahExists(id)) { "Unknown AyahId $id" }
            AyahPolygonRegion(editionId, pageNumber, id.surahNumber, id.ayahNumber, parsePolygon(item.getString("polygon")))
        }
        return PolygonMushafPage(editionId, pageNumber, viewBox, renderBox, regions)
    }

    private fun exactInt(value: Any): Int {
        require(value is Number && value.toDouble().isFinite() && value.toDouble() == value.toInt().toDouble())
        return value.toInt()
    }

    private fun parsePolygon(value: String): List<PagePolygon> {
        val tokens = TOKEN.findAll(value).map { it.value }.toList()
        require(value.replace(TOKEN, "").all { it.isWhitespace() || it == ',' }) { "Unsupported polygon syntax" }
        require(tokens.isNotEmpty())
        if (tokens.first() != "M") {
            require(tokens.none { it in listOf("M", "L", "Z") } && tokens.size % 2 == 0)
            return listOf(PagePolygon(tokens.chunked(2).map { PagePoint(it[0].toFloat(), it[1].toFloat()) }))
        }
        val polygons = mutableListOf<PagePolygon>()
        var position = 0
        while (position < tokens.size) {
            require(tokens[position++] == "M")
            val points = mutableListOf<PagePoint>()
            while (true) {
                require(position + 1 < tokens.size)
                points.add(PagePoint(tokens[position++].toFloat(), tokens[position++].toFloat()))
                require(position < tokens.size)
                when (tokens[position++]) {
                    "L" -> continue
                    "Z" -> break
                    else -> error("Expected closed M/L/Z polygon")
                }
            }
            polygons.add(PagePolygon(points))
        }
        return polygons
    }

    companion object {
        private val TOKEN = Regex("[MLZ]|[-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][-+]?\\d+)?")
    }
}
