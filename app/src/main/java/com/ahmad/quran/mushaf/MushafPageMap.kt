package com.ahmad.quran.mushaf

import com.ahmad.quran.data.AyahId
import com.ahmad.quran.data.MushafEdition
import java.io.InputStream

/** Canonical identities mapped by an edition; never a universal ayah page number. */
interface MushafPageMap {
    val edition: MushafEdition
    fun firstPage(surahNumber: Int): Int
    fun firstAyah(pageNumber: Int): AyahId
    fun lastAyah(pageNumber: Int): AyahId
    fun pageForAyah(id: AyahId): Int?
}

/** Offline edition assets. Polygon coordinates are independent of UI direction. */
interface MushafPageAssetProvider {
    val editionId: String
    val availablePages: List<Int>
    fun openPage(pageNumber: Int): InputStream
}
