package com.ahmad.quran.mushaf.svg

import android.content.Context
import android.graphics.RectF
import android.util.LruCache
import com.ahmad.quran.data.QuranRepository
import com.ahmad.quran.mushaf.svg.PolygonMushafPage
import com.ahmad.quran.mushaf.svg.QuranSvgPageAdapter
import com.caverock.androidsvg.SVG
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class NativeSvgPage(val geometry: PolygonMushafPage, val artwork: SVG, val parseNanos: Long)

/** Single caller thread in the app. Standard LRU bounds retained documents to one or three. */
class NativeSvgPageLoader(private val context: Context, capacity: Int = 1, private val assetRoot: String = "mushaf-svg") : AutoCloseable, com.ahmad.quran.mushaf.MushafPageAssetProvider {
    val cache = LruCache<Int, NativeSvgPage>(capacity)
    private val repository = QuranRepository.open(context)
    private val adapter = QuranSvgPageAdapter { repository.getAyah(it.surahNumber, it.ayahNumber) != null }
    private val catalog = JSONObject(read("pages.json"))
    val pages = (0 until catalog.getJSONArray("pages").length()).map {
        catalog.getJSONArray("pages").getJSONObject(it).getInt("pageNumber")
    }
    override val editionId: String get()=catalog.getString("editionId")
    override val availablePages: List<Int> get()=pages
    override fun openPage(pageNumber: Int): java.io.InputStream {
        require(pageNumber in pages)
        return context.assets.open("$assetRoot/"+"%03d.svg".format(Locale.ROOT,pageNumber))
    }
    @Volatile var parsedDocumentCount = 0
        private set
    private val overrides = OpeningPageHitOverrides(read("opening-hit-overrides.json"))

    init { require(capacity == 1 || capacity == 3) }

    /** Called on the loader's worker before changing the current document. */
    fun prepareCurrent(number: Int, capacity: Int) {
        require(number in pages && capacity in listOf(1, 3))
        cache.resize(capacity)
        val index = pages.indexOf(number)
        val allowed = if (capacity == 1) setOf(number) else
            listOfNotNull(pages.getOrNull(index-1), number, pages.getOrNull(index+1)).toSet()
        cache.snapshot().keys.filter { it !in allowed }.forEach { cache.remove(it) }
    }

    fun load(number: Int): NativeSvgPage {
        require(number in pages)
        cache.get(number)?.let { return it }
        if (cache.maxSize() == 1) cache.evictAll()
        val stem = "%03d".format(Locale.ROOT, number)
        val metadata = catalog.getJSONArray("pages").getJSONObject(pages.indexOf(number))
        val source = adapter.parse(catalog.getString("editionId"), number,
            box(metadata.getJSONArray("viewBox")), box(metadata.getJSONArray("renderBox")), read("$stem.json"))
        val start = System.nanoTime()
        val svg = openPage(number).use { SVG.getFromInputStream(it) }
        parsedDocumentCount++
        val elapsed = System.nanoTime()-start
        return NativeSvgPage(overrides.apply(source), svg, elapsed).also { cache.put(number, it) }
    }

    fun prefetchNeighbours(number: Int, shouldContinue: () -> Boolean = { true }) {
        require(cache.maxSize() == 3)
        prepareCurrent(number, 3)
        val index = pages.indexOf(number)
        listOfNotNull(pages.getOrNull(index-1), pages.getOrNull(index+1)).forEach {
            if (!shouldContinue()) return
            load(it)
        }
    }

    private fun read(name: String) = context.assets.open("$assetRoot/$name").bufferedReader().use { it.readText() }
    private fun box(array: JSONArray): RectF {
        val x = array.getDouble(0).toFloat()
        val y = array.getDouble(1).toFloat()
        return RectF(x, y, x+array.getDouble(2).toFloat(), y+array.getDouble(3).toFloat())
    }
    override fun close() { cache.evictAll(); repository.close() }
}
