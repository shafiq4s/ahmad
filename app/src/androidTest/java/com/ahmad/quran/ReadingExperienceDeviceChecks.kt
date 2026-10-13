package com.ahmad.quran

import android.app.Instrumentation
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.ViewModelProvider
import com.ahmad.quran.data.AyahId
import com.ahmad.quran.data.QuranRepository
import com.ahmad.quran.mushaf.svg.NativeSvgPageView
import com.ahmad.quran.mushaf.svg.PagePoint
import com.ahmad.quran.ui.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Explicit test-only entry: no server, production endpoint or extra bundled artwork. */
class ReadingExperienceDeviceChecks(private val test: Instrumentation, private val activity: MainActivity) {
    private val model = ViewModelProvider(activity)[MainViewModel::class.java]
    private val view = activity.findViewById<NativeSvgPageView>(R.id.mushaf_page)
    private val preferences = ReadingPreferences(activity)
    private fun main(action: () -> Unit) = test.runOnMainSync(action)
    private fun waitUntil(check: () -> Boolean) {
        val end = SystemClock.uptimeMillis() + 45000
        while (SystemClock.uptimeMillis() < end) {
            var ready = false; main { ready = check() }
            if (ready) return
            SystemClock.sleep(60)
        }
        error("Local reading experience did not become ready")
    }
    private fun capture(name: String) {
        SystemClock.sleep(500)
        acknowledgeEmulatorBootDialog()
        val bitmap = checkNotNull(test.uiAutomation.takeScreenshot())
        File(activity.filesDir, "experience-$name.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
    private fun acknowledgeEmulatorBootDialog() {
        // Only known software-emulator boot dialogs; an app ANR is never acknowledged.
        repeat(3) {
            val root = test.uiAutomation.rootInActiveWindow ?: return
            if (root.findAccessibilityNodeInfosByText("Process system").isEmpty() &&
                root.findAccessibilityNodeInfosByText("Launcher3").isEmpty()) return
            val wait = (root.findAccessibilityNodeInfosByText("WAIT") + root.findAccessibilityNodeInfosByText("Wait"))
                .firstOrNull { it.isClickable }
            check(wait != null && wait.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK))
            SystemClock.sleep(700)
        }
    }
    fun run() {
        main { model.openPage(121) }
        waitUntil { activity.reader.displaying && view.page?.geometry?.pageNumber == 121 }
        check(preferences.lastLocation?.pageNumber == 121 && preferences.lastReadAtMillis > 0)
        main {
            if (preferences.bookmarks.any { it.pageNumber == 121 }) model.removeBookmark(com.ahmad.quran.mushaf.svg.QuranSvgEdition.ID, 121)
            activity.findViewById<View>(R.id.toggle_bookmark).performClick()
        }
        check(preferences.bookmarks.single().pageNumber == 121)
        check(preferences.history.first().pageNumber == 121 && preferences.history.size <= 10)
        main { model.navigateTo(Screen.HOME) }
        waitUntil { activity.findViewById<View>(R.id.home_panel).visibility == View.VISIBLE }
        main {
            check(activity.findViewById<TextView>(R.id.last_location).text.contains("المائدة"))
        }
        capture("01-home")
        main { activity.findViewById<View>(R.id.open_surahs).performClick() }
        waitUntil { activity.findViewById<View>(R.id.quran_panel).visibility == View.VISIBLE }
        main {
            activity.findViewById<EditText>(R.id.surah_search).setText("الاخلاص")
            val rows = activity.findViewById<ListView>(R.id.surah_list)
            check(rows.adapter.count == 1 && rows.adapter.getItemId(0) == 112L)
            activity.findViewById<EditText>(R.id.surah_search).setText("")
        }
        main { model.navigateTo(Screen.HOME) }
        main { activity.findViewById<View>(R.id.open_bookmarks).performClick() }
        waitUntil { activity.findViewById<View>(R.id.bookmarks_panel).visibility == View.VISIBLE }
        capture("02-bookmarks")
        main {
            val rows = activity.findViewById<ListView>(R.id.bookmarks_list)
            rows.performItemClick(rows.adapter.getView(0, null, rows), 0, 121)
        }
        waitUntil { activity.reader.displaying && view.page?.geometry?.pageNumber == 121 }
        capture("03-reader")
        // Real platform long press: selection and the new bookmark action use the same canonical ID.
        val rows = JSONArray(activity.assets.open("mushaf-svg/121.json").bufferedReader().use { it.readText() })
        val row = rows.getJSONObject(0)
        lateinit var point: PagePoint
        val downTime = SystemClock.uptimeMillis()
        main {
            point = view.screenPoint(PagePoint(row.getDouble("x").toFloat(), row.getDouble("y").toFloat()))
            check(view.hitTest(point.x, point.y) == AyahId(5, 78))
            val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, point.x, point.y, 0)
            view.dispatchTouchEvent(down); down.recycle()
        }
        SystemClock.sleep(950L)
        main {
            val up = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, point.x, point.y, 0)
            view.dispatchTouchEvent(up); up.recycle()
        }
        waitUntil { activity.ayahDialog.isOpen && activity.ayahDialog.dialog.findViewById<Button>(R.id.ayah_dialog_bookmark)!!.isEnabled }
        val canonical = QuranRepository.open(activity).use { it.getAyah(5, 78)!!.text }
        main {
            check(activity.ayahDialog.ayahId == AyahId(5, 78) && view.selectedAyah == AyahId(5, 78))
            check(activity.ayahDialog.dialog.findViewById<TextView>(R.id.ayah_dialog_text)!!.text.toString() == canonical)
            activity.ayahDialog.dialog.findViewById<Button>(R.id.ayah_dialog_bookmark)!!.performClick()
            check(preferences.bookmarks.isEmpty() && activity.ayahDialog.isOpen)
            activity.ayahDialog.dialog.findViewById<Button>(R.id.ayah_dialog_bookmark)!!.performClick()
            check(preferences.bookmarks.single().ayahNumber == 78)
            activity.ayahDialog.dismiss()
            activity.findViewById<View>(R.id.go_page).performClick()
        }
        SystemClock.sleep(400)
        main {
            activity.pageJumpDialog!!.findViewById<RadioGroup>(R.id.jump_mode)!!.check(R.id.jump_mode_juz)
            activity.pageJumpDialog!!.findViewById<Spinner>(R.id.jump_selection)!!.setSelection(6)
        }
        capture("04-jump")
        main { activity.pageJumpDialog!!.findViewById<View>(R.id.jump_confirm)!!.performClick() }
        waitUntil { activity.reader.displaying && model.state.value.pageNumber == 121 }
        main { model.navigateTo(Screen.SETTINGS) }
        waitUntil { activity.findViewById<View>(R.id.settings_panel).visibility == View.VISIBLE }
        main {
            activity.findViewById<SwitchCompat>(R.id.keep_screen_awake).isChecked = true
            activity.findViewById<SwitchCompat>(R.id.auto_resume_reading).isChecked = true
            activity.findViewById<SwitchCompat>(R.id.show_reader_bars).isChecked = false
            check(activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON == 0)
        }
        capture("05-settings")
        main { activity.findViewById<View>(R.id.open_about).performClick() }
        waitUntil { activity.findViewById<View>(R.id.about_panel).visibility == View.VISIBLE }
        main {
            check(activity.findViewById<TextView>(R.id.quran_svg_attribution).text.contains("مشروع مستقل"))
            check(activity.findViewById<TextView>(R.id.app_version).text.contains(BuildConfig.VERSION_NAME))
        }
        capture("06-about")
        main { activity.onBackPressedDispatcher.onBackPressed() }
        waitUntil { activity.findViewById<View>(R.id.settings_panel).visibility == View.VISIBLE }
        main { activity.findViewById<View>(R.id.open_support).performClick() }
        waitUntil { activity.findViewById<View>(R.id.support_panel).visibility == View.VISIBLE }
        capture("07-support")
        main { activity.onBackPressedDispatcher.onBackPressed() }
        waitUntil { activity.findViewById<View>(R.id.settings_panel).visibility == View.VISIBLE }
        check(preferences.keepScreenAwake && preferences.autoResumeReading && !preferences.showReaderBarsOnOpen)
        main { model.openPage(1) }
        waitUntil { activity.reader.displaying && activity.findViewById<View>(R.id.top_bar).visibility == View.GONE }
        main { check(activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0) }
        main { model.navigateTo(Screen.SETTINGS) }
        waitUntil { activity.findViewById<View>(R.id.settings_panel).visibility == View.VISIBLE }
        val last = preferences.lastLocation
        val bookmarks = preferences.bookmarks
        val history = preferences.history
        acknowledgeEmulatorBootDialog()
        main { activity.findViewById<View>(R.id.cache_clear).performClick() }
        SystemClock.sleep(400)
        acknowledgeEmulatorBootDialog()
        val root = test.uiAutomation.rootInActiveWindow
        check(root.findAccessibilityNodeInfosByText(activity.getString(R.string.cache_confirm)).isNotEmpty())
        // Locate the real positive button through the focused Android dialog window.
        val positive = root.findAccessibilityNodeInfosByViewId("android:id/button1").first()
        check(positive.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK))
        waitUntil { activity.findViewById<TextView>(R.id.cache_feedback).text == activity.getString(R.string.cache_cleared) }
        check(DownloadedPageCache(activity).sizeBytes() == 0L)
        check(preferences.lastLocation == last && preferences.bookmarks == bookmarks && preferences.history == history)
        check(activity.assets.open("mushaf-svg/001.svg").use { it.read() } >= 0)
        check(activity.assets.open("mushaf-svg/121.json").use { it.read() } >= 0)
        QuranRepository.open(activity).use { check(it.getSurahs().sumOf { s -> s.ayahCount } == 6236) }
        File(activity.filesDir, "reading-experience-device.json").writeText(JSONObject()
            .put("api", android.os.Build.VERSION.SDK_INT).put("success", true)
            .put("resumeTimestamp", true).put("bookmarkModalExactId", true).put("surahNameSearch", true)
            .put("juz7ArtworkPage", 121).put("optionsPersist", true).put("awakeOnlyInReader", true)
            .put("entryBarsHidden", true).put("cacheClearPreservesLocalStateAndCanonicalAssets", true)
            .put("historyBoundedAndPreserved", true).put("aboutSupportNaturalBack", true)
            .put("screenshots", 7).toString(2))
    }
}
