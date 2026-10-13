package com.ahmad.quran.svgnative

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.ahmad.quran.mushaf.svg.PagePoint
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23, 36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NativeSvgActivityTest {
    @Test fun devicePanelRecordsActualRequestsAndSeparatesCacheModes() {
        val controller=Robolectric.buildActivity(NativeSvgActivity::class.java).setup()
        val activity=controller.get()
        val metrics=activity.root.getChildAt(1) as TextView
        try {
            await { activity.pageView.page!=null }
            draw(activity)
            val text=metrics.text.toString()
            assertTrue(text.contains(Build.MODEL))
            assertTrue(text.contains("API ${Build.VERSION.SDK_INT}"))
            for(name in listOf("cold","parse","render","nav","median/P95","hit/miss","retained","PSS current/max","Ayah")) {
                assertTrue("Missing metric: $name",text.contains(name))
            }
            assertTrue(text.contains("cache 1 · retained 1"))
            // No fabricated PSS before any sampling callback ran.
            assertTrue(text.contains("PSS current/max —/—"))
            // Finish the attached window's queued traversal before mapping touch coordinates.
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
            val json=JSONArray(activity.assets.open("native-svg/001.json").bufferedReader().use { it.readText() }).getJSONObject(0)
            val point=activity.pageView.screenPoint(PagePoint(json.getDouble("x").toFloat(),json.getDouble("y").toFloat()))
            val now=SystemClock.uptimeMillis()
            val down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,point.x,point.y,0)
            activity.pageView.dispatchTouchEvent(down);down.recycle()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(950))
            assertTrue("Panel after press: ${metrics.text}; selected=${activity.pageView.selectedAyah}",
                metrics.text.toString().contains("Ayah 1:1"))
            val up=MotionEvent.obtain(now,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,point.x,point.y,0)
            activity.pageView.dispatchTouchEvent(up);up.recycle()

            val controls=activity.root.getChildAt(activity.root.childCount-1) as LinearLayout
            val toggle=controls.getChildAt(0) as Button
            assertTrue(toggle.text.toString().contains("A"))
            toggle.performClick()
            await { activity.pageView.page!=null }
            draw(activity)
            await { metrics.text.toString().contains("cache 3 · retained 2") }
            assertTrue(toggle.text.toString().contains("B"))
            assertTrue(metrics.text.toString().contains("Ayah —"))
            controller.pause()
            val report=JSONObject(File(activity.filesDir,"device-validation.json").readText())
            assertEquals(Build.MODEL,report.getString("deviceModel"))
            val rows=report.getJSONArray("transitions")
            assertEquals(2,rows.length())
            assertEquals(0,rows.getJSONObject(0).getInt("session"))
            assertEquals(1,rows.getJSONObject(1).getInt("session"))
            assertEquals(1,rows.getJSONObject(0).getInt("mode"))
            assertEquals(3,rows.getJSONObject(1).getInt("mode"))
            assertFalse(rows.getJSONObject(0).getBoolean("cacheHit"))
            assertFalse(rows.getJSONObject(1).getBoolean("cacheHit"))
            assertFalse(rows.getJSONObject(0).isNull("coldActivityContentMs"))
            assertTrue(rows.getJSONObject(1).isNull("coldActivityContentMs"))
        } finally {
            controller.stop().destroy()
        }
    }

    private fun await(condition: () -> Boolean) {
        val deadline=System.nanoTime()+10_000_000_000L
        while(!condition() && System.nanoTime()<deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertTrue("Background page load/UI completion timed out",condition())
    }

    private fun draw(activity: NativeSvgActivity) {
        activity.root.measure(View.MeasureSpec.makeMeasureSpec(412,View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(732,View.MeasureSpec.EXACTLY))
        activity.root.layout(0,0,412,732)
        val view=activity.pageView
        val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        bitmap.recycle()
    }
}
