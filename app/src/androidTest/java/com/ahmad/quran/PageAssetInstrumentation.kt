package com.ahmad.quran

import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import com.ahmad.quran.data.AyahId
import com.ahmad.quran.data.QuranRepository
import com.ahmad.quran.mushaf.svg.NativeSvgPageView
import com.ahmad.quran.mushaf.svg.PagePoint
import com.ahmad.quran.mushaf.svg.QuranSvgEdition
import com.ahmad.quran.ui.MainActivity
import com.ahmad.quran.ui.MainViewModel
import com.ahmad.quran.ui.Screen
import org.json.JSONObject
import org.json.JSONArray
import java.io.File

/** Development-only rendezvous: the host stops its local server before the cache reopen. */
class PageAssetInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments);start() }
    override fun onStart() {
        val result=Bundle()
        try {
            val ready=File(targetContext.filesDir,"page-assets-cache-ready.json").apply { delete() }
            val offline=File(targetContext.filesDir,"page-assets-offline-ready").apply { delete() }
            val cache=File(targetContext.cacheDir,"mushaf-pages/${QuranSvgEdition.ID}").apply { deleteRecursively() }
            val a=startActivitySync(Intent(targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
            val model=ViewModelProvider(a)[MainViewModel::class.java];val view=a.findViewById<NativeSvgPageView>(R.id.mushaf_page)
            waitUntil { !model.state.value.loading }
            check(a.reader.parsedDocumentCount==0)
            runOnMainSync { model.openPage(1) }
            waitUntil { view.page?.geometry?.pageNumber==1 && a.reader.displaying }
            check(!cache.exists())
            runOnMainSync { model.openPage(121) }
            waitUntil { view.page?.geometry?.pageNumber==121 && a.reader.displaying }
            runOnMainSync {
                val p=view.screenPoint(PagePoint(120f,18f));check(view.hitTest(p.x,p.y)==AyahId(5,78))
            }
            waitUntil { (119..123).all { File(cache,"$it.svg").isFile } && a.reader.retainedDocumentCount==3 }
            val manifest=JSONObject(targetContext.assets.open("mushaf-svg/source-manifest.json").bufferedReader().use { it.readText() }).getJSONArray("files")
            for(n in 119..123) {
                val row=(0 until manifest.length()).map { manifest.getJSONObject(it) }.first { it.getString("filename")=="$n.svg" }
                check(QuranRepository.sha256(File(cache,"$n.svg"))==row.getString("sha256"))
            }
            check(a.reader.parsedDocumentCount<=5) // Page1/2 plus current121 and nearest120/122 only.
            ready.writeText(JSONObject().put("currentPage",121).put("diskPages",JSONArray(listOf(119,120,121,122,123))).put("sha256Exact",true).toString(2))
            val end=SystemClock.uptimeMillis()+120000
            while(!offline.exists() && SystemClock.uptimeMillis()<end) SystemClock.sleep(100)
            check(offline.exists()) { "Host did not stop the test server" }
            runOnMainSync { model.navigateTo(Screen.QURAN) }
            waitUntil { a.reader.retainedDocumentCount==0 }
            runOnMainSync { model.openPage(121) }
            waitUntil { view.page?.geometry?.pageNumber==121 && a.reader.displaying }
            check(!a.reader.failed)
            // Acknowledge only known software-emulator boot dialogs, never an application ANR.
            repeat(3) {
                val window=uiAutomation.rootInActiveWindow
                if(window!=null && (window.findAccessibilityNodeInfosByText("Launcher3").isNotEmpty() ||
                    window.findAccessibilityNodeInfosByText("Process system").isNotEmpty())) {
                    val wait=(window.findAccessibilityNodeInfosByText("WAIT")+window.findAccessibilityNodeInfosByText("Wait")).firstOrNull { it.isClickable }
                    check(wait!=null && wait.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK))
                    SystemClock.sleep(700)
                }
            }
            val image=checkNotNull(uiAutomation.takeScreenshot())
            File(targetContext.filesDir,"page-assets-offline-121.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
            image.recycle()
            for(n in listOf(300,604)) {
                runOnMainSync { model.openPage(n) }
                waitUntil { view.page?.geometry?.pageNumber==n && a.reader.displaying }
            }
            check(view.page!!.geometry.regions.last().ayahId==AyahId(114,6))
            File(targetContext.filesDir,"page-assets-device.json").writeText(JSONObject().put("api",android.os.Build.VERSION.SDK_INT)
                .put("success",true).put("bundled",JSONArray(listOf(1,300,604))).put("downloaded",JSONArray(listOf(119,120,121,122,123)))
                .put("sha256Exact",true).put("offlineReopen121",true).put("memoryDocumentsMax",3).toString(2))
            result.putBoolean("success",true);finish(-1,result)
        } catch(error:Throwable) {
            result.putBoolean("success",false);result.putString("error",android.util.Log.getStackTraceString(error));finish(0,result)
        }
    }
    private fun waitUntil(condition:()->Boolean) {
        val end=SystemClock.uptimeMillis()+60000
        while(SystemClock.uptimeMillis()<end) {
            var done=false;runOnMainSync { done=condition() };if(done)return
            SystemClock.sleep(50)
        }
        error("Native on-demand reader did not become ready")
    }
}
