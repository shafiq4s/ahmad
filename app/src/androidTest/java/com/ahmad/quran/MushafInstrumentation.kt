package com.ahmad.quran

import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.widget.ListView
import androidx.lifecycle.ViewModelProvider
import com.ahmad.quran.data.AyahId
import com.ahmad.quran.mushaf.svg.NativeSvgPageView
import com.ahmad.quran.mushaf.svg.PagePoint
import com.ahmad.quran.ui.MainActivity
import com.ahmad.quran.ui.MainViewModel
import org.json.JSONObject
import java.io.File

/** Device-only measurements for the actual main-app view and bounded loader. */
class MushafInstrumentation:Instrumentation() {
    override fun onCreate(arguments:Bundle?) { super.onCreate(arguments);start() }
    override fun onStart() {
        var activity:MainActivity?=null
        val result=Bundle()
        try {
            val started=System.nanoTime()
            val a=startActivitySync(Intent(targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
            activity=a
            waitUntil { a.findViewById<ListView>(R.id.surah_list).adapter.count==114 && !ViewModelProvider(a)[MainViewModel::class.java].state.value.loading }
            val indexMs=(System.nanoTime()-started)/1e6
            check(a.reader.parsedDocumentCount==0 && a.reader.retainedDocumentCount==0)
            val view=a.findViewById<NativeSvgPageView>(R.id.mushaf_page)
            for(number in listOf(1,2,48,121,604,121,48,49,48)) {
                runOnMainSync { ViewModelProvider(a)[MainViewModel::class.java].openPage(number) }
                waitUntil { view.page?.geometry?.pageNumber==number && a.reader.displaying }
                check(a.reader.retainedDocumentCount<=3)
                if(number==121) check(view.page!!.geometry.regions.first().ayahId==AyahId(5,78))
                if(number==604) {
                    check(view.page!!.geometry.regions.last().ayahId==AyahId(114,6))
                    lateinit var point:PagePoint
                    val rows=org.json.JSONArray(targetContext.assets.open("mushaf-svg/604.json").bufferedReader().use { it.readText() })
                    val row=rows.getJSONObject(rows.length()-1)
                    runOnMainSync {
                        point=view.screenPoint(PagePoint(row.getDouble("x").toFloat(),row.getDouble("y").toFloat()))
                        val now=SystemClock.uptimeMillis();val e=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,point.x,point.y,0)
                        view.dispatchTouchEvent(e);e.recycle()
                    }
                    SystemClock.sleep(850)
                    runOnMainSync {
                        check(view.selectedAyah==AyahId(114,6))
                        val now=SystemClock.uptimeMillis();val e=MotionEvent.obtain(now,now,MotionEvent.ACTION_UP,point.x,point.y,0)
                        view.dispatchTouchEvent(e);e.recycle()
                    }
                }
                // Allow post-draw neighboring prefetch and process-PSS sampling to finish.
                SystemClock.sleep(700)
            }
            SystemClock.sleep(5000)
            runOnMainSync { a.reader.saveMeasurements() }
            val report=JSONObject(File(targetContext.filesDir,"mushaf-reading-measurements.json").readText())
            report.put("instrumentedIndexReadyMs",indexMs).put("startupParsedSvgDocuments",0)
                .put("instrumentationSuccess",true).put("deviceKind","Google API23 emulator; software CPU emulation, not physical-phone timings")
            File(targetContext.filesDir,"main-integration-device.json").writeText(report.toString(2))
            result.putBoolean("success",true)
            finish(-1,result)
        } catch(error:Throwable) {
            result.putBoolean("success",false);result.putString("error",error.toString());finish(0,result)
        } finally { activity?.let { a -> runOnMainSync { a.finish() } } }
    }
    private fun waitUntil(condition:()->Boolean) {
        val deadline=SystemClock.uptimeMillis()+30000
        while(SystemClock.uptimeMillis()<deadline) {
            var done=false
            runOnMainSync { done=condition() }
            if(done) return
            SystemClock.sleep(30)
        }
        error("Main app did not complete load/draw within 30 seconds")
    }
}
