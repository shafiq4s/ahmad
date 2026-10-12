package com.ahmad.quran

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.ahmad.quran.data.AyahId
import com.ahmad.quran.data.QuranRepository
import com.ahmad.quran.mushaf.svg.NativeSvgPageView
import com.ahmad.quran.mushaf.svg.PagePoint
import com.ahmad.quran.mushaf.svg.QuranSvgEdition
import com.ahmad.quran.ui.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import java.io.File
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23, 36], qualifiers = "en-rUS-w360dp-h640dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LocalizationUiTest {
    private fun await(check: () -> Boolean) {
        val end = System.nanoTime() + 20_000_000_000L
        while (!check() && System.nanoTime() < end) { shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(10) }
        assertTrue("Localized UI did not become ready", check())
    }
    private fun layout(a: MainActivity) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300))
        a.window.decorView.apply {
            measure(View.MeasureSpec.makeMeasureSpec(720, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(1280, View.MeasureSpec.EXACTLY))
            layout(0, 0, 720, 1280)
        }
    }
    private fun ready(a: MainActivity) { await { !ViewModelProvider(a)[MainViewModel::class.java].state.value.loading }; layout(a) }
    private fun drawPage(a: MainActivity, number: Int) {
        ready(a)
        val view = a.findViewById<NativeSvgPageView>(R.id.mushaf_page)
        await { view.page?.geometry?.pageNumber == number }; layout(a)
        val b = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(b)); b.recycle(); layout(a)
    }

    @Test fun systemToEveryManualLanguageAndBackPreservesAllLocalReadingData() {
        val app = RuntimeEnvironment.getApplication()
        SourcePageFixtures.install(app)
        val prefs = ReadingPreferences(app)
        prefs.saveLocation(ReadingLocation(ReaderMode.PAGE, 5, 78, 121, QuranSvgEdition.ID))
        prefs.saveBookmark(ReadingBookmark(121, 5, 78))
        prefs.recordHistory(ReadingHistoryEntry(121, 5, 78))
        prefs.setKeepScreenAwake(true); prefs.setShowReaderBarsOnOpen(false)
        val before = app.getSharedPreferences("reading", Context.MODE_PRIVATE).all
        val cache = File(app.cacheDir, "mushaf-pages/${QuranSvgEdition.ID}")
        val cached = cache.listFiles()!!.associate { it.name to sha256(it) }
        val c = Robolectric.buildActivity(MainActivity::class.java).setup(); var a = c.get()
        try {
            ready(a)
            assertEquals(InterfaceLocales.SYSTEM, InterfaceLocales.choice(a))
            assertEquals("en", InterfaceLocales.effective(a))
            assertEquals(0, a.reader.parsedDocumentCount)
            for (tag in InterfaceLocales.tags + InterfaceLocales.SYSTEM) {
                ViewModelProvider(a)[MainViewModel::class.java].navigateTo(Screen.SETTINGS); layout(a)
                a.findViewById<View>(R.id.choose_language).performClick()
                val dialog = ShadowDialog.getLatestDialog() as AlertDialog
                assertEquals(8, dialog.listView.adapter.count)
                val index = (listOf(InterfaceLocales.SYSTEM) + InterfaceLocales.tags).indexOf(tag)
                dialog.listView.performItemClick(dialog.listView.adapter.getView(index, null, dialog.listView), index, index.toLong())
                // Emulate the platform's configuration restart; Robolectric LocaleManager does not drive OS lifecycle.
                c.recreate(); a = c.get(); ready(a)
                val effective = if (tag == InterfaceLocales.SYSTEM) "en" else tag
                assertEquals(tag, InterfaceLocales.choice(a))
                assertEquals(effective, InterfaceLocales.effective(a))
                val resFolder=when(effective) { "ar" -> "values-ar"; "en" -> "values-en"; else -> "values-b+${effective.replace('-', '+')}" }
                val xml=File(requireNotNull(System.getProperty("quran.sourceDir")), "../../../app/src/main/res/$resFolder/strings.xml")
                val messages=javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml).getElementsByTagName("string")
                val expected=(0 until messages.length).map { messages.item(it) as org.w3c.dom.Element }.first { it.getAttribute("name")=="open_mushaf" }.textContent
                assertEquals("Actual resource lookup for $effective", expected, a.getString(R.string.open_mushaf))
                val direction = InterfaceLocales.direction(effective)
                assertEquals(direction, a.findViewById<View>(R.id.app_root).layoutDirection)
                assertEquals(before, app.getSharedPreferences("reading", Context.MODE_PRIVATE).all)
                assertEquals(cached, cache.listFiles()!!.associate { it.name to sha256(it) })
                assertEquals(Screen.SETTINGS, ViewModelProvider(a)[MainViewModel::class.java].state.value.screen)
                a.onBackPressedDispatcher.onBackPressed(); layout(a)
                assertEquals(Screen.HOME, ViewModelProvider(a)[MainViewModel::class.java].state.value.screen)
                assertEquals(a.getString(R.string.app_name), a.findViewById<TextView>(R.id.screen_title).text.toString())
                val name = ViewModelProvider(a)[MainViewModel::class.java].state.value.surahs.first { it.surahNumber == 5 }.name
                val displayed = InterfaceLocales.displayName(a, name)
                assertTrue(displayed.contains(name))
                if (!InterfaceLocales.isRtl(effective)) assertTrue(displayed.contains('\u202B'))
                assertEquals(a.getString(R.string.resume_details, displayed, 121), a.findViewById<TextView>(R.id.last_location).text.toString())
                if (tag != InterfaceLocales.SYSTEM) capture(a.window.decorView, tag)
                a.findViewById<View>(R.id.open_surahs).performClick(); layout(a)
                val rows = a.findViewById<ListView>(R.id.surah_list)
                assertEquals(114, rows.adapter.count)
                assertEquals(direction, rows.getChildAt(0).layoutDirection)
                a.findViewById<View>(R.id.nav_settings).performClick(); layout(a)
                a.findViewById<View>(R.id.open_about).performClick(); layout(a)
                assertEquals(direction, a.findViewById<View>(R.id.about_panel).layoutDirection)
                assertTrue(a.findViewById<TextView>(R.id.tanzil_attribution).text.contains("Tanzil"))
                a.onBackPressedDispatcher.onBackPressed(); layout(a)
                a.findViewById<View>(R.id.open_support).performClick(); layout(a)
                assertEquals(direction, a.findViewById<View>(R.id.support_panel).layoutDirection)
                a.onBackPressedDispatcher.onBackPressed(); layout(a)
                ViewModelProvider(a)[MainViewModel::class.java].navigateTo(Screen.HOME); layout(a)
            }
        } finally { InterfaceLocales.select(a, InterfaceLocales.SYSTEM); c.pause().stop().destroy() }
    }

    @Test fun ltrAndHazaragiRtlDialogsLeaveCanonicalTextArtworkHitTestingAndSharingUnchanged() {
        val app = RuntimeEnvironment.getApplication(); SourcePageFixtures.install(app)
        val c = Robolectric.buildActivity(MainActivity::class.java).setup(); var a = c.get()
        try {
            for (tag in listOf("en", "haz-AF")) {
                ready(a); InterfaceLocales.select(a, tag); c.recreate(); a = c.get(); ready(a)
                assertTrue(ViewModelProvider(a)[MainViewModel::class.java].openAyah(AyahId(5, 78)))
                drawPage(a, 121)
                val view = a.findViewById<NativeSvgPageView>(R.id.mushaf_page)
                val geometry = view.page!!.geometry
                for (zoom in listOf(1f, 2f, 4f)) {
                    view.setZoom(zoom)
                    val first = org.json.JSONArray(a.assets.open("mushaf-svg/121.json").bufferedReader().use { it.readText() }).getJSONObject(0)
                    val p = view.screenPoint(PagePoint(first.getDouble("x").toFloat(), first.getDouble("y").toFloat()))
                    assertEquals(AyahId(5, 78), view.hitTest(p.x, p.y))
                }
                val details = kotlinx.coroutines.runBlocking { ViewModelProvider(a)[MainViewModel::class.java].getAyahDetails(AyahId(5, 78)) }
                a.ayahDialog.begin(AyahId(5, 78), details.surahName, QuranSvgEdition.ID, 121); a.ayahDialog.bind(details); layout(a)
                assertEquals(InterfaceLocales.direction(tag), a.ayahDialog.dialog.findViewById<View>(R.id.ayah_dialog)!!.layoutDirection)
                val text = a.ayahDialog.dialog.findViewById<TextView>(R.id.ayah_dialog_text)!!
                QuranRepository.open(a).use { assertEquals(it.getAyah(5, 78)!!.text, text.text.toString()) }
                assertEquals(View.LAYOUT_DIRECTION_RTL, text.layoutDirection)
                a.ayahDialog.dialog.findViewById<View>(R.id.ayah_dialog_copy)!!.performClick()
                val clipboard = a.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                assertEquals(details.ayah.text, clipboard.primaryClip!!.getItemAt(0).text.toString())
                a.ayahDialog.dialog.findViewById<View>(R.id.ayah_dialog_share)!!.performClick()
                val share = shadowOf(a).nextStartedActivity.getParcelableExtra<android.content.Intent>(android.content.Intent.EXTRA_INTENT)!!
                assertEquals(details.ayah.text, share.getStringExtra(android.content.Intent.EXTRA_TEXT))
                a.ayahDialog.dismiss()
                a.findViewById<View>(R.id.go_page).performClick(); layout(a)
                assertEquals(InterfaceLocales.direction(tag), a.pageJumpDialog!!.window!!.decorView.layoutDirection)
                a.pageJumpDialog!!.dismiss()
                assertSame(geometry, view.page!!.geometry)
                assertTrue(a.reader.retainedDocumentCount <= 3)
            }
        } finally { InterfaceLocales.select(a, InterfaceLocales.SYSTEM); c.pause().stop().destroy() }
    }

    @Test fun systemPersianRegionsAndPlatformLanguageChangesUseTheSamePolicy() {
        val c=Robolectric.buildActivity(MainActivity::class.java).setup();var a=c.get()
        try {
            ready(a)
            for((qualifier,expected) in listOf("fr" to "ar", "fa-rIR" to "ar", "fa" to "fa-AF", "fa-rAF" to "fa-AF")) {
                RuntimeEnvironment.setQualifiers("$qualifier-w360dp-h640dp-xhdpi")
                c.recreate();a=c.get();ready(a)
                assertEquals(InterfaceLocales.SYSTEM,InterfaceLocales.choice(a))
                assertEquals(expected,InterfaceLocales.effective(a))
                assertEquals(InterfaceLocales.direction(expected),a.findViewById<View>(R.id.app_root).layoutDirection)
                assertEquals(if(expected=="ar") "فتح المصحف" else "باز کردن مصحف",a.getString(R.string.open_mushaf))
            }
            if(android.os.Build.VERSION.SDK_INT>=33) {
                a.getSystemService(android.app.LocaleManager::class.java).applicationLocales=android.os.LocaleList.forLanguageTags("uz-UZ")
                c.recreate();a=c.get();ready(a)
                assertEquals("uz-UZ",InterfaceLocales.choice(a))
                assertEquals("Mushafni ochish",a.getString(R.string.open_mushaf))
            } else {
                InterfaceLocales.select(a,"fa-AF");c.recreate();a=c.get();ready(a)
                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.getEmptyLocaleList())
                c.recreate();a=c.get();ready(a)
                assertEquals("fa-AF",InterfaceLocales.choice(a))
                assertEquals("باز کردن مصحف",a.getString(R.string.open_mushaf))
            }
        } finally { InterfaceLocales.select(a,InterfaceLocales.SYSTEM);c.pause().stop().destroy() }
    }

    private fun sha256(file: File): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(8192)
            while (true) { val size = stream.read(buffer); if (size < 0) break; digest.update(buffer, 0, size) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    private fun capture(view: View, tag: String) {
        if (android.os.Build.VERSION.SDK_INT != 36) return
        val b = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(b))
        val dir = File(requireNotNull(System.getProperty("quran.sourceDir")), "../../localization/screenshots/simulated-api36").apply { mkdirs() }
        File(dir, "$tag.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }; b.recycle()
    }
}
