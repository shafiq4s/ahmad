package com.ahmad.quran.mushaf.svg

import android.graphics.Path
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.min

/** Presentation-only line strips for the pinned Hafs/KFQC artwork. Never used by hit testing. */
internal class AyahHighlight private constructor(val lines: List<RectF>) {
    fun path(sourcePixelsPerScreenPixel: Float, density: Float): Path = Path().apply {
        for (line in lines) {
            val radius = min(3f * density * sourcePixelsPerScreenPixel, min(line.height(), line.width()) / 4f)
            addRoundRect(line, radius, radius, Path.Direction.CW)
        }
    }

    companion object {
        fun prepare(page: PolygonMushafPage, region: AyahPolygonRegion): AyahHighlight {
            // The opening spread has a narrower, seven-row typesetting grid. Other pages
            // use the source's fifteen-row grid. These are visual bands, not touch overrides.
            val opening = page.pageNumber <= 2
            val firstCenter = if (opening) 32.5f else 25.25f
            val pitch = if (opening) 28.33f else 36f
            val height = pitch * 0.80f
            val bounds = page.viewBox
            val lines = (0 until if (opening) 7 else 15).mapNotNull { index ->
                val center = firstCenter + index * pitch
                val spans = region.polygons.flatMap { spansAt(it, center) }
                if (spans.isEmpty()) return@mapNotNull null
                // Unite every piece of this ayah on the same row, including intervening whitespace.
                val left = spans.minOf { it.first }.coerceAtLeast(bounds.left)
                val right = spans.maxOf { it.second }.coerceAtMost(bounds.right)
                if (right <= left) null else RectF(left, center - height / 2f, right, center + height / 2f)
            }
            return AyahHighlight(lines)
        }

        /** Horizontal winding-rule spans; source points and touch geometry remain immutable. */
        private fun spansAt(polygon: PagePolygon, y: Float): List<Pair<Float, Float>> {
            val crossings = polygon.points.indices.mapNotNull { index ->
                val a = polygon.points[index]
                val b = polygon.points[(index + 1) % polygon.points.size]
                if (!((a.y <= y && b.y > y) || (b.y <= y && a.y > y))) null
                else (a.x + (y - a.y) * (b.x - a.x) / (b.y - a.y)) to if (b.y > a.y) 1 else -1
            }.sortedBy { it.first }
            var winding = 0
            var left = 0f
            val spans = mutableListOf<Pair<Float, Float>>()
            for ((x, direction) in crossings) {
                if (winding == 0) left = x
                winding += direction
                if (winding == 0 && abs(x - left) > 0.001f) spans.add(left to x)
            }
            return spans
        }
    }
}
