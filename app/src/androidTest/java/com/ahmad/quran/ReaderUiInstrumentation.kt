package com.ahmad.quran

import android.app.Instrumentation
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.ViewModelProvider
import com.ahmad.quran.data.AyahId
import com.ahmad.quran.data.QuranRepository
import com.ahmad.quran.mushaf.svg.NativeSvgPageView
import com.ahmad.quran.mushaf.svg.PagePoint
import com.ahmad.quran.mushaf.svg.QuranSvgEdition
import com.ahmad.quran.ui.MainActivity
import com.ahmad.quran.ui.MainViewModel
import com.ahmad.quran.ui.Screen
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Real Android text metrics and compositor captures, separate from offscreen simulation. */
class ReaderUiInstrumentation : Instrumentation() {
    private var designScreenshots = false
    private var readingExperience = false
    override fun onCreate(arguments: Bundle?) {
        designScreenshots = arguments?.getString("designScreenshots") == "true"
        readingExperience = arguments?.getString("readingExperience") == "true"
        super.onCreate(arguments); start()
    }
    override fun onStart() {
        val result = Bundle()
        try {
            val accessibility = uiAutomation.serviceInfo
            accessibility.flags = accessibility.flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            uiAutomation.serviceInfo = accessibility
            if (readingExperience) {
                com.ahmad.quran.ui.ReadingPreferences(targetContext).apply {
                    setAutoResumeReading(false); setKeepScreenAwake(false); setShowReaderBarsOnOpen(true)
                }
            }
            val activity = startActivitySync(Intent(targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
            val model = ViewModelProvider(activity)[MainViewModel::class.java]
            val view = activity.findViewById<NativeSvgPageView>(R.id.mushaf_page)
            waitUntil { !model.state.value.loading }
            if (readingExperience) {
                ReadingExperienceDeviceChecks(this, activity).run()
                result.putBoolean("success", true); finish(-1, result); return
            }
            if (designScreenshots) captureReaderDesign(activity, model, view)
            val report = JSONArray()
            QuranRepository.open(targetContext).use { repository ->
                for ((id, page) in listOf(AyahId(1, 1) to 1, AyahId(1, 7) to 1, AyahId(2, 282) to 48, AyahId(5, 78) to 121, AyahId(114, 6) to 604)) {
                    runOnMainSync { model.openPage(page) }
                    waitUntil { view.page?.geometry?.pageNumber == page && activity.reader.displaying }
                    val rows = JSONArray(targetContext.assets.open("mushaf-svg/%03d.json".format(java.util.Locale.ROOT, page)).bufferedReader().use { it.readText() })
                    val row = (0 until rows.length()).map { rows.getJSONObject(it) }.first { it.getInt("surahNumber") == id.surahNumber && it.getInt("ayahNumber") == id.ayahNumber }
                    lateinit var point: PagePoint
                    if (id == AyahId(2, 282)) {
                        runOnMainSync {
                            val tapPoint = view.screenPoint(PagePoint(row.getDouble("x").toFloat(), row.getDouble("y").toFloat()))
                            val now = SystemClock.uptimeMillis()
                            for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                                val tap = MotionEvent.obtain(now, now + if (action == MotionEvent.ACTION_UP) 60 else 0, action, tapPoint.x, tapPoint.y, 0)
                                view.dispatchTouchEvent(tap); tap.recycle()
                            }
                            check(view.selectedAyah == id && !activity.ayahDialog.isOpen)
                        }
                        SystemClock.sleep(500)
                        capture("reader-long-line-highlight.png")
                    }
                    val downTime = SystemClock.uptimeMillis()
                    runOnMainSync {
                        point = view.screenPoint(PagePoint(row.getDouble("x").toFloat(), row.getDouble("y").toFloat()))
                        check(view.hitTest(point.x, point.y) == id)
                        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, point.x, point.y, 0)
                        view.dispatchTouchEvent(down); down.recycle()
                    }
                    SystemClock.sleep((android.view.ViewConfiguration.getLongPressTimeout() + android.view.ViewConfiguration.getTapTimeout() + 50).toLong())
                    runOnMainSync {
                        val up = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, point.x, point.y, 0)
                        view.dispatchTouchEvent(up); up.recycle()
                    }
                    waitUntil { activity.ayahDialog.dialog.findViewById<Button>(R.id.ayah_dialog_copy)!!.isEnabled }
                    val text = repository.getAyah(id.surahNumber, id.ayahNumber)!!.text
                    SystemClock.sleep(500)
                    runOnMainSync {
                        check(activity.ayahDialog.ayahId == id && view.selectedAyah == id)
                        check(activity.ayahDialog.editionId == QuranSvgEdition.ID && activity.ayahDialog.pageNumber == page)
                        check(activity.ayahDialog.dialog.findViewById<TextView>(R.id.ayah_dialog_text)!!.text.toString() == text)
                    }
                    if (id == AyahId(1, 1)) capture("reader-popup-short.png")
                    if (id == AyahId(2, 282)) {
                        val scroll = activity.ayahDialog.dialog.findViewById<NestedScrollView>(R.id.ayah_dialog_scroll)!!
                        runOnMainSync { check(scroll.canScrollVertically(1)) }
                        capture("reader-popup-long.png")
                        runOnMainSync {
                            check(scroll.canScrollVertically(1))
                            scroll.scrollTo(0, scroll.getChildAt(0).height)
                            check(scroll.scrollY > 0)
                        }
                    }
                    runOnMainSync {
                        activity.ayahDialog.dialog.findViewById<Button>(R.id.ayah_dialog_copy)!!.performClick()
                        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        check(clipboard.primaryClip!!.getItemAt(0).text.toString() == text)
                    }
                    // Send real system Back to the focused dialog window, not the Activity callback.
                    val backDown = SystemClock.uptimeMillis()
                    for (action in listOf(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.ACTION_UP)) {
                        val key = android.view.KeyEvent(backDown, SystemClock.uptimeMillis(), action, android.view.KeyEvent.KEYCODE_BACK,
                            0, 0, android.view.KeyCharacterMap.VIRTUAL_KEYBOARD, 0, android.view.KeyEvent.FLAG_FROM_SYSTEM, android.view.InputDevice.SOURCE_KEYBOARD)
                        check(uiAutomation.injectInputEvent(key, true))
                    }
                    waitUntil { !activity.ayahDialog.isOpen && view.selectedAyah == null }
                    runOnMainSync { check(model.state.value.screen == Screen.READER) }
                    SystemClock.sleep(2500)
                    report.put(JSONObject().put("ayah", "${id.surahNumber}:${id.ayahNumber}").put("page", page)
                        .put("canonicalExact", true).put("copyExact", true).put("backKeepsReader", true))
                }
            }
            File(targetContext.filesDir, "reader-ui-device.json").writeText(JSONObject().put("api", android.os.Build.VERSION.SDK_INT)
                .put("success", true).put("cases", report).put("realAndroidTextScrollingVerified", true).toString(2))
            result.putBoolean("success", true); finish(-1, result)
        } catch (error: Throwable) {
            result.putBoolean("success", false); result.putString("error", android.util.Log.getStackTraceString(error)); finish(0, result)
        }
    }
    private fun captureReaderDesign(activity: MainActivity, model: MainViewModel, view: NativeSvgPageView) {
        SystemClock.sleep(500)
        capture("design-index.png")
        runOnMainSync { model.openPage(121) }
        waitUntil { view.page?.geometry?.pageNumber == 121 && activity.reader.displaying }
        SystemClock.sleep(500)
        capture("design-reader.png")
        runOnMainSync { activity.findViewById<android.view.View>(R.id.go_page).performClick() }
        SystemClock.sleep(700)
        // Hide only the keyboard for the compositor capture, keeping the numeric dialog open.
        runOnMainSync {
            val input=activity.pageJumpDialog!!.findViewById<android.widget.EditText>(R.id.page_number)!!
            androidx.core.view.WindowCompat.getInsetsController(activity.pageJumpDialog!!.window!!,input)
                .hide(androidx.core.view.WindowInsetsCompat.Type.ime())
        }
        SystemClock.sleep(500)
        capture("design-jump.png")
        runOnMainSync { activity.pageJumpDialog!!.dismiss();model.openPage(48) }
        waitUntil { view.page?.geometry?.pageNumber == 48 && activity.reader.displaying }
        SystemClock.sleep(500)
        runOnMainSync {
            check(view.hitTest(1f,1f)==null)
            val now=SystemClock.uptimeMillis()
            for(action in listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP)) {
                val e=MotionEvent.obtain(now,now+if(action==MotionEvent.ACTION_UP)60 else 0,action,1f,1f,0)
                view.dispatchTouchEvent(e);e.recycle()
            }
        }
        waitUntil { activity.findViewById<android.view.View>(R.id.top_bar).visibility==android.view.View.GONE }
        SystemClock.sleep(500)
        capture("design-immersive.png")
        runOnMainSync {
            val now=SystemClock.uptimeMillis()
            for(action in listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP)) {
                val e=MotionEvent.obtain(now,now+if(action==MotionEvent.ACTION_UP)60 else 0,action,1f,1f,0)
                view.dispatchTouchEvent(e);e.recycle()
            }
        }
        waitUntil { activity.findViewById<android.view.View>(R.id.top_bar).visibility==android.view.View.VISIBLE }
        SystemClock.sleep(500)
        runOnMainSync { activity.findViewById<android.view.View>(R.id.nav_settings).performClick() }
        SystemClock.sleep(500)
        capture("design-settings.png")
        File(targetContext.filesDir,"reader-design-device.json").writeText(JSONObject().put("api",android.os.Build.VERSION.SDK_INT)
            .put("success",true).put("readerPage",121).put("immersivePage",48).put("screenshots",5).toString(2))
    }

    private fun capture(name: String) {
        // Only acknowledge known emulator boot dialogs. API23 uses ordinary AlertDialog
        // button IDs, unlike newer Android's aerr_wait, so find the actual WAIT button.
        repeat(3) {
            val window = uiAutomation.rootInActiveWindow
            val bootDialog = window != null && (window.findAccessibilityNodeInfosByText("Launcher3").isNotEmpty() ||
                window.findAccessibilityNodeInfosByText("Process system").isNotEmpty())
            if (bootDialog) {
                val wait = (window!!.findAccessibilityNodeInfosByText("WAIT") + window.findAccessibilityNodeInfosByText("Wait"))
                    .firstOrNull { it.isClickable }
                check(wait != null && wait.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK))
                SystemClock.sleep(700)
            }
        }
        SystemClock.sleep(500)
        val bitmap = checkNotNull(uiAutomation.takeScreenshot())
        File(targetContext.filesDir, name).outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    private fun waitUntil(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 45000
        while (SystemClock.uptimeMillis() < deadline) {
            var done = false; runOnMainSync { done = condition() }
            if (done) return
            SystemClock.sleep(50)
        }
        error("Actual Android view did not become ready")
    }
}
