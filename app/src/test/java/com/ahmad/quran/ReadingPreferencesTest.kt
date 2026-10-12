package com.ahmad.quran

import com.ahmad.quran.data.ReadingMetadata
import com.ahmad.quran.data.QuranRepository
import org.json.JSONObject
import com.ahmad.quran.ui.ReaderMode
import com.ahmad.quran.ui.ReadingLocation
import com.ahmad.quran.ui.ReadingPreferences
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23, 36])
class ReadingPreferencesTest {
    @Test fun settingsAndLocationsRoundTripWithValidatedBounds() {
        val app = RuntimeEnvironment.getApplication()
        val preferences = ReadingPreferences(app)
        assertNull(preferences.lastLocation)
        assertEquals(28, preferences.textSizeSp)
        assertFalse(preferences.darkMode)
        preferences.setTextSize(100)
        assertEquals(40, ReadingPreferences(app).textSizeSp)
        preferences.setTextSize(-1)
        assertEquals(20, ReadingPreferences(app).textSizeSp)
        preferences.setDarkMode(true)
        val location = ReadingLocation(ReaderMode.SURAH, 2, 140, 21)
        preferences.saveLocation(location)
        assertEquals(location, ReadingPreferences(app).lastLocation)
        assertTrue(ReadingPreferences(app).darkMode)
        val page = ReadingLocation(ReaderMode.PAGE, 112, 1, 604)
        preferences.saveLocation(page)
        assertEquals(page, ReadingPreferences(app).lastLocation)
        app.getSharedPreferences("reading", 0).edit().putInt("page", 605).commit()
        assertNull(ReadingPreferences(app).lastLocation)
    }

    @Test fun revelationTypesUseOnlyTheVerbatimApprovedOfficialXml() {
        val app = RuntimeEnvironment.getApplication()
        val source = File(System.getProperty("quran.sourceDir"), "quran-data.xml")
        assertArrayEquals(source.readBytes(), app.assets.open("quran-data.xml").use { it.readBytes() })
        val types = ReadingMetadata.getRevelationTypes(app)
        assertEquals(114, types.size)
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(source)
        val surahs = document.getElementsByTagName("sura")
        for (i in 0 until surahs.length) {
            val attributes = surahs.item(i).attributes
            assertEquals(attributes.getNamedItem("type").nodeValue, types[attributes.getNamedItem("index").nodeValue.toInt()])
        }
    }
    @Test fun allApprovedReferenceFilesRemainUnchanged() {
        val root = File(checkNotNull(System.getProperty("quran.sourceDir")), "../../..").canonicalFile
        val pinned = JSONObject(File(root, "tools/quran/reference-lock.json").readText()).getJSONObject("sha256")
        for (path in pinned.keys()) assertEquals(path, pinned.getString(path), QuranRepository.sha256(File(root, path)))
    }

}
