package com.ahmad.quran.mushaf.svg

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[36],qualifiers="xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AyahHighlightCaptureTest {
    @Test fun captureReferencePages() {
        val app=RuntimeEnvironment.getApplication()
        val stage=System.getProperty("highlight.captureStage","after")
        val tools=File(requireNotNull(System.getProperty("quran.sourceDir")),"../..")
        val output=File(tools,"ayah-highlight/screenshots/$stage").apply { mkdirs() }
        NativeSvgPageLoader(app,1).use { loader ->
            for(number in listOf(1,2,48,121,604)) {
                val view=NativeSvgPageView(app);view.layout(0,0,1080,1722);view.display(loader.load(number))
                val rows=JSONArray(app.assets.open("mushaf-svg/%03d.json".format(java.util.Locale.ROOT,number)).bufferedReader().use { it.readText() })
                val index=if(number==1)5 else if(number==2)3 else if(number==604)rows.length()-1 else 0
                val row=rows.getJSONObject(index)
                val point=PagePoint(row.getDouble("x").toFloat(),row.getDouble("y").toFloat())
                for(zoom in listOf(1f,2f,4f)) {
                    view.setZoom(1f);val focus=view.screenPoint(point);view.setZoom(zoom,focus.x,focus.y)
                    val p=view.screenPoint(point);val now=SystemClock.uptimeMillis()
                    val down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,p.x,p.y,0);view.dispatchTouchEvent(down);down.recycle()
                    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(900))
                    assertEquals(com.ahmad.quran.data.AyahId(row.getInt("surahNumber"),row.getInt("ayahNumber")),view.selectedAyah)
                    val bitmap=Bitmap.createBitmap(1080,1722,Bitmap.Config.ARGB_8888);bitmap.eraseColor(android.graphics.Color.WHITE)
                    view.draw(Canvas(bitmap))
                    File(output,"page-$number-zoom${zoom.toInt()}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
                    val up=MotionEvent.obtain(now,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,p.x,p.y,0);view.dispatchTouchEvent(up);up.recycle()
                    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2200))
                }
                view.release()
            }
        }
    }
}
