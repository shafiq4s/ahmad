package com.ahmad.quran

import android.content.Context
import com.ahmad.quran.mushaf.svg.QuranSvgEdition
import java.io.File
import java.nio.file.Files

/** Rendering regressions use pinned workspace files in disk cache, never extra packaged SVG. */
object SourcePageFixtures {
    private val root get() = File(requireNotNull(System.getProperty("quran.sourceDir"))).toPath().parent.parent.parent.toFile()
    fun original(name: String) = File(root, "tools/mushaf-svg/local-assets/mushaf-svg/$name")
    fun install(context: Context) {
        val cache = File(context.cacheDir, "mushaf-pages/${QuranSvgEdition.ID}").apply { mkdirs() }
        val samples = listOf(1,2,25,48,50,100,121,150,200,250,300,350,400,450,500,550,600,604)
        val pages = samples.flatMap { (it-2..it+2).filter { n -> n in 1..604 } }.toSet()
        for (number in pages) {
            val name = "%03d.svg".format(java.util.Locale.ROOT, number)
            val target = File(cache, name)
            if (!target.exists()) {
                val source = original(name)
                runCatching { Files.createLink(target.toPath(), source.toPath()) }.getOrElse { source.copyTo(target) }
            }
        }
    }
}
