package com.ahmad.quran.svgnative

import com.ahmad.quran.mushaf.svg.NativeSvgPageView

import com.ahmad.quran.mushaf.svg.NativeSvgPageLoader

import com.ahmad.quran.mushaf.svg.NativeSvgPage

import android.app.Activity
import android.app.Instrumentation
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** No additional test library: runs on actual Android emulators, reporting failures explicitly. */
class NativeSvgInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments);start() }
    override fun onStart() {
        try {
            val report = NativeSvgMeasurements.run(targetContext,"Android emulator, software CPU acceleration disabled") { action -> runOnMainSync { action() } }
            val pages = JSONArray()
            val pressLatencies = mutableListOf<Double>()
            val dispatchLatencies = mutableListOf<Double>()
            NativeSvgPageLoader(targetContext, 1, "native-svg").use { loader ->
                for(number in loader.pages) {
                    val loaded = loader.load(number)
                    val bitmap = Bitmap.createBitmap(1080,1722,Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    lateinit var view: NativeSvgPageView
                    lateinit var point: com.ahmad.quran.mushaf.svg.PagePoint
                    var callbackAt = 0L
                    var downTime = 0L
                    val id=loaded.geometry.regions.last().ayahId
                    runOnMainSync {
                        view = NativeSvgPageView(targetContext)
                        val exactW=android.view.View.MeasureSpec.makeMeasureSpec(1080,android.view.View.MeasureSpec.EXACTLY)
                        val exactH=android.view.View.MeasureSpec.makeMeasureSpec(1722,android.view.View.MeasureSpec.EXACTLY)
                        view.measure(exactW,exactH);view.layout(0,0,1080,1722);view.display(loaded);view.draw(Canvas(bitmap))
                        val source=JSONArray(targetContext.assets.open("native-svg/%03d.json".format(number)).bufferedReader().use { it.readText() })
                        val marker=source.getJSONObject(source.length()-1)
                        point=view.screenPoint(com.ahmad.quran.mushaf.svg.PagePoint(marker.getDouble("x").toFloat(),marker.getDouble("y").toFloat()))
                        check(view.hitTest(point.x,point.y)==id)
                        view.onAyahLongPress = { returned -> check(returned==id);callbackAt=System.nanoTime() }
                    }
                    val colors=IntArray(bitmap.width*bitmap.height)
                    bitmap.getPixels(colors,0,bitmap.width,0,0,bitmap.width,bitmap.height)
                    val ink=colors.count { Color.red(it)<100 }
                    check(ink>10000) { "SVG blank on API ${android.os.Build.VERSION.SDK_INT}, page $number" }
                    File(targetContext.filesDir,"page-$number-native.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
                    val startPress=System.nanoTime()
                    runOnMainSync {
                        downTime=android.os.SystemClock.uptimeMillis()
                        val down=android.view.MotionEvent.obtain(downTime,downTime,android.view.MotionEvent.ACTION_DOWN,point.x,point.y,0)
                        view.dispatchTouchEvent(down);down.recycle()
                    }
                    Thread.sleep(android.view.ViewConfiguration.getLongPressTimeout().toLong()+android.view.ViewConfiguration.getTapTimeout()+500)
                    runOnMainSync {
                        check(callbackAt>0 && view.selectedAyah==id) { "Long press failed on page $number" }
                        pressLatencies.add((callbackAt-startPress)/1_000_000.0)
                        dispatchLatencies.add(view.lastLongPressDispatchNanos/1_000_000.0)
                        view.draw(Canvas(bitmap))
                        val up=android.view.MotionEvent.obtain(downTime,android.os.SystemClock.uptimeMillis(),android.view.MotionEvent.ACTION_UP,point.x,point.y,0)
                        view.dispatchTouchEvent(up);up.recycle()
                        view.release()
                    }
                    File(targetContext.filesDir,"page-$number-selected.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
                    bitmap.recycle()
                    pages.put(JSONObject().put("page",number).put("inkPixels",ink).put("hitIdentityVerified",true))
                }
            }
            report.put("nativePageSmokeChecks",pages)
            report.put("longPressDownToCallbackMs",JSONArray(pressLatencies))
                .put("longPressDispatchMs",JSONArray(dispatchLatencies))
                .put("configuredLongPressTimeoutMs",android.view.ViewConfiguration.getLongPressTimeout())
                .put("legacyAdditionalTapTimeoutMs",if(android.os.Build.VERSION.SDK_INT==23)android.view.ViewConfiguration.getTapTimeout() else 0)
            File(targetContext.filesDir,"measurements-android.json").writeText(report.toString(2)+"\n")
            finish(Activity.RESULT_OK,Bundle().apply { putString("success","true");putString("report","measurements-android.json") })
        } catch(error: Throwable) {
            finish(Activity.RESULT_CANCELED,Bundle().apply { putString("success","false");putString("error",error.stackTraceToString()) })
        }
    }
}
