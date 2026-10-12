package com.ahmad.quran.mushaf.svg

import android.graphics.RectF
import com.ahmad.quran.data.AyahId
import com.ahmad.quran.data.QuranRepository
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[23,36])
class MushafSvgTest {
    private val context get()=RuntimeEnvironment.getApplication()
    private fun read(name:String)=context.assets.open("mushaf-svg/$name").bufferedReader().use { it.readText() }
    private fun box(a:JSONArray)=RectF(a.getDouble(0).toFloat(),a.getDouble(1).toFloat(),(a.getDouble(0)+a.getDouble(2)).toFloat(),(a.getDouble(1)+a.getDouble(3)).toFloat())
    @Test fun fullCorpusHashesCanonicalIdentityAndEditionMapsRemainIndependent() {
        val catalog=JSONObject(read("pages.json"));val manifest=JSONObject(read("source-manifest.json"));val seen=mutableSetOf<AyahId>()
        assertEquals(QuranSvgEdition.COMMIT,manifest.getString("commit"))
        val files=manifest.getJSONArray("files")
        for(i in 0 until files.length()) {
            val row=files.getJSONObject(i);val digest=MessageDigest.getInstance("SHA-256");var bytes=0L
            context.assets.open("mushaf-svg/"+row.getString("filename")).use { stream ->
                val buffer=ByteArray(65536);var count=stream.read(buffer)
                while(count>=0) { digest.update(buffer,0,count);bytes+=count;count=stream.read(buffer) }
            }
            assertEquals(row.getLong("bytes"),bytes)
            assertEquals(row.getString("sha256"),digest.digest().joinToString("") { "%02x".format(it) })
        }
        QuranRepository.open(context).use { repo ->
            val counts=repo.getSurahs().associate { it.surahNumber to it.ayahCount }
            val adapter=QuranSvgPageAdapter { it.ayahNumber in 1..(counts[it.surahNumber] ?: 0) }
            val overrides=OpeningPageHitOverrides(read("opening-hit-overrides.json"));val edition=QuranSvgEdition(context)
            assertEquals(114,counts.size);assertEquals(604,catalog.getJSONArray("pages").length())
            for(number in 1..604) {
                val row=catalog.getJSONArray("pages").getJSONObject(number-1)
                val original=adapter.parse(QuranSvgEdition.ID,number,box(row.getJSONArray("viewBox")),box(row.getJSONArray("renderBox")),read("%03d.json".format(java.util.Locale.ROOT,number)))
                val page=overrides.apply(original)
                if(number>=3) assertSame(original,page)
                assertEquals(edition.firstAyah(number),page.regions.first().ayahId)
                assertEquals(edition.lastAyah(number),page.regions.last().ayahId)
                seen.addAll(page.regions.map { it.ayahId })
            }
            assertEquals(6236,seen.size)
            for(surah in 1..114) {
                val page=edition.firstPage(surah)
                val rows=JSONArray(read("%03d.json".format(java.util.Locale.ROOT,page)))
                assertTrue((0 until rows.length()).any { rows.getJSONObject(it).getInt("surahNumber")==surah && rows.getJSONObject(it).getInt("ayahNumber")==1 })
            }
            assertEquals(AyahId(5,78),edition.firstAyah(121))
            assertEquals(AyahId(5,77),repo.getPage(QuranRepository.DEFAULT_EDITION,121)!!.ayahs.first().id)
            assertEquals(120,edition.pageForAyah(AyahId(5,77)))
            assertEquals(AyahId(114,6),edition.lastAyah(604))
        }
    }
    @Test fun distributedSvgSamplingParsesAndCacheNeverRetainsMoreThanThreeDocuments() {
        NativeSvgPageLoader(context,3).use { loader ->
            assertEquals((1..604).toList(),loader.pages);assertEquals(0,loader.parsedDocumentCount)
            for(number in listOf(1,2,25,48,50,100,121,150,200,250,300,350,400,450,500,550,600,604)) {
                loader.prepareCurrent(number,3);val page=loader.load(number)
                assertEquals(number,page.geometry.pageNumber);assertNotNull(page.artwork)
                loader.prefetchNeighbours(number);assertTrue(loader.cache.size()<=3)
                assertSame(page,loader.load(number))
                assertTrue(loader.cache.snapshot().keys.all { it in (number-1)..(number+1) })
            }
        }
    }
    @Test fun invalidIdentitiesAndOutOfBoundsAreRejectedAndHeadingsAreUnassigned() {
        val adapter=QuranSvgPageAdapter { it==AyahId(114,6) }
        for(id in listOf("112:5","114:7")) {
            val parts=id.split(':');val json="""[{"surahNumber":${parts[0]},"ayahNumber":${parts[1]},"polygon":"M 0 0 L 1 0 L 1 1 Z"}]"""
            assertThrows(IllegalArgumentException::class.java) { adapter.parse(QuranSvgEdition.ID,604,RectF(0f,0f,345f,550f),RectF(0f,0f,345f,550f),json) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            adapter.parse(QuranSvgEdition.ID,604,RectF(0f,0f,345f,550f),RectF(0f,0f,345f,550f),"""[{"surahNumber":114,"ayahNumber":6,"polygon":"M -1 0 L 1 0 L 1 1 Z"}]""")
        }
        NativeSvgPageLoader(context,1).use { loader ->
            for(number in listOf(1,2)) {
                val page=loader.load(number).geometry;assertNull(page.hitTest(110f,-40f))
                if(number==2) assertNull(page.hitTest(110f,30f))
            }
        }
    }
}
