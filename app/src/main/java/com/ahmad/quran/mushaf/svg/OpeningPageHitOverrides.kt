package com.ahmad.quran.mushaf.svg

import com.ahmad.quran.mushaf.svg.PagePoint
import com.ahmad.quran.mushaf.svg.PagePolygon
import com.ahmad.quran.mushaf.svg.PolygonMushafPage
import org.json.JSONObject

/** Explicit audited, edition-scoped touch geometry. Artwork and official JSON remain intact. */
class OpeningPageHitOverrides(json: String) {
    private val source = JSONObject(json)

    fun apply(page: PolygonMushafPage): PolygonMushafPage {
        if (page.pageNumber !in 1..2) return page
        require(page.editionId == source.getString("editionId"))
        val replacements = source.getJSONArray("pages").getJSONObject(page.pageNumber-1).getJSONArray("regions")
        val regions = page.regions.map { region ->
            val row = (0 until replacements.length()).map { replacements.getJSONObject(it) }
                .firstOrNull { it.getInt("ayahNumber") == region.ayahNumber } ?: return@map region
            require(row.getInt("surahNumber") == region.surahNumber)
            val arrays = row.optJSONArray("polygons")?.let { multiple ->
                (0 until multiple.length()).map { multiple.getJSONArray(it) }
            } ?: listOf(row.getJSONArray("points"))
            region.copy(polygons = arrays.map { points ->
                PagePolygon((0 until points.length()).map {
                    PagePoint(points.getJSONArray(it).getDouble(0).toFloat(), points.getJSONArray(it).getDouble(1).toFloat())
                })
            })
        }
        return PolygonMushafPage(page.editionId, page.pageNumber, page.viewBox, page.renderBox, regions)
    }
}
