package com.ahmad.quran.mushaf.svg

import android.graphics.Path
import android.graphics.RectF
import com.ahmad.quran.data.AyahId

data class PagePoint(val x: Float, val y: Float)

/** Edition coordinates, never RTL-reflected; independent of the upstream JSON syntax. */
class PagePolygon(points: List<PagePoint>) {
    val points: List<PagePoint> = points.toList()
    init {
        require(points.size >= 3 && points.all { it.x.isFinite() && it.y.isFinite() })
    }

    fun path(): Path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }

    fun contains(x: Float, y: Float): Boolean {
        // Winding rule matches the source SVG, including the hand-corrected opening spread.
        var winding = 0
        for (i in points.indices) {
            val a = points[i]
            val b = points[(i + 1) % points.size]
            val side = (b.x - a.x) * (y - a.y) - (x - a.x) * (b.y - a.y)
            if (a.y <= y && b.y > y && side > 0) winding++
            if (a.y > y && b.y <= y && side < 0) winding--
        }
        return winding != 0
    }
}

data class AyahPolygonRegion(
    val editionId: String,
    val pageNumber: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val polygons: List<PagePolygon>,
) {
    val ayahId: AyahId get() = AyahId(surahNumber, ayahNumber)
    fun path(): Path = Path().apply { polygons.forEach { addPath(it.path()) } }
}

class PolygonMushafPage(
    val editionId: String,
    val pageNumber: Int,
    viewBox: RectF,
    renderBox: RectF,
    val regions: List<AyahPolygonRegion>,
) {
    private val sourceBounds = RectF(viewBox)
    private val displayBounds = RectF(renderBox)
    val viewBox: RectF get() = RectF(sourceBounds)
    val renderBox: RectF get() = RectF(displayBounds)

    init {
        require(!sourceBounds.isEmpty && !displayBounds.isEmpty)
        require(regions.isNotEmpty() && regions.map { it.ayahId }.distinct().size == regions.size)
        require(regions.all { region ->
            region.editionId == editionId && region.pageNumber == pageNumber && region.polygons.isNotEmpty() &&
                region.polygons.all { polygon -> polygon.points.all {
                    it.x >= sourceBounds.left && it.x <= sourceBounds.right &&
                        it.y >= sourceBounds.top && it.y <= sourceBounds.bottom
                } }
        }) { "Polygon outside source page or inconsistent edition/page." }
    }

    /** Ambiguous source overlaps fail closed instead of selecting a different ayah. */
    fun hitTest(x: Float, y: Float): AyahId? = regions.filter {
        it.polygons.any { polygon -> polygon.contains(x, y) }
    }.map { it.ayahId }.singleOrNull()
}
