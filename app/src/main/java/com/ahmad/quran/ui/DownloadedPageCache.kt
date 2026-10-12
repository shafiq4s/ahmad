package com.ahmad.quran.ui

import android.content.Context
import com.ahmad.quran.mushaf.svg.QuranSvgEdition
import java.io.File

/** Only downloaded artwork for the current edition; assets, SQLite and preferences are elsewhere. */
class DownloadedPageCache(context: Context) {
    private val directory = File(context.applicationContext.cacheDir, "mushaf-pages/${QuranSvgEdition.ID}")
    fun sizeBytes(): Long = if(directory.exists()) directory.walkTopDown().filter { it.isFile }.sumOf { it.length() } else 0L
    fun clear(): Boolean = !directory.exists() || directory.deleteRecursively()
}
