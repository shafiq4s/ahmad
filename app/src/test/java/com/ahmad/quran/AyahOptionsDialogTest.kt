package com.ahmad.quran

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
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
@Config(sdk=[23,36],qualifiers="ar-w412dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AyahOptionsDialogTest {
    @org.junit.Before fun installLocalRenderingFixtures() {
        com.ahmad.quran.SourcePageFixtures.install(org.robolectric.RuntimeEnvironment.getApplication())
    }
    private val width=824
    private val height=1600
    private fun await(condition:()->Boolean) {
        val deadline=System.nanoTime()+20_000_000_000L
        while(!condition() && System.nanoTime()<deadline) { shadowOf(Looper.getMainLooper()).idle();Thread.sleep(10) }
        assertTrue("Requested local state did not arrive",condition())
    }
    private fun layout(a:MainActivity) {
        val root=a.window.decorView
        root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY))
        root.layout(0,0,width,height)
        if(a.ayahDialog.dialog.isShowing) {
            val w=a.ayahDialog.dialog.window!!;val decor=w.decorView
            decor.measure(View.MeasureSpec.makeMeasureSpec(w.attributes.width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(w.attributes.height,View.MeasureSpec.EXACTLY))
            decor.layout(0,0,decor.measuredWidth,decor.measuredHeight)
        }
    }
    private fun settle(a:MainActivity) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300));layout(a) }
    private fun ready(a:MainActivity) { await { !ViewModelProvider(a)[MainViewModel::class.java].state.value.loading };layout(a) }
    private fun page(a:MainActivity,n:Int):NativeSvgPageView {
        ViewModelProvider(a)[MainViewModel::class.java].openPage(n)
        val view=a.findViewById<NativeSvgPageView>(R.id.mushaf_page)
        await { view.page?.geometry?.pageNumber==n };layout(a)
        val b=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888);view.draw(Canvas(b));b.recycle();settle(a)
        return view
    }
    private fun point(a:MainActivity,v:NativeSvgPageView,id:AyahId):PagePoint {
        val rows=JSONArray(a.assets.open("mushaf-svg/%03d.json".format(java.util.Locale.ROOT,v.page!!.geometry.pageNumber)).bufferedReader().use { it.readText() })
        val r=(0 until rows.length()).map { rows.getJSONObject(it) }.first { it.getInt("surahNumber")==id.surahNumber && it.getInt("ayahNumber")==id.ayahNumber }
        return v.screenPoint(PagePoint(r.getDouble("x").toFloat(),r.getDouble("y").toFloat()))
    }
    private fun touch(v:NativeSvgPageView,p:PagePoint,long:Boolean) {
        val now=SystemClock.uptimeMillis()
        val down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,p.x,p.y,0);v.dispatchTouchEvent(down);down.recycle()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(if(long)(ViewConfiguration.getLongPressTimeout()+ViewConfiguration.getTapTimeout()+30).toLong() else 60))
        val up=MotionEvent.obtain(now,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,p.x,p.y,0);v.dispatchTouchEvent(up);up.recycle()
    }
    private fun press(a:MainActivity,v:NativeSvgPageView,id:AyahId) {
        val p=point(a,v,id);assertEquals(id,v.hitTest(p.x,p.y));touch(v,p,true)
        await { a.ayahDialog.dialog.findViewById<Button>(R.id.ayah_dialog_copy)!!.isEnabled };settle(a)
        assertEquals(id,v.selectedAyah);assertEquals(id,a.ayahDialog.ayahId);assertEquals(id,v.hitTest(p.x,p.y))
    }

    @Test fun fiveCanonicalAyahsKeepTextMetadataAndTextOnlyCopyShareInCenteredModal() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup();val a=controller.get()
        val report=JSONArray()
        try {
            ready(a)
            QuranRepository.open(a).use { repository ->
                for((id,n,juz) in listOf(Triple(AyahId(1,1),1,1),Triple(AyahId(1,7),1,1),Triple(AyahId(2,282),48,3),Triple(AyahId(5,78),121,6),Triple(AyahId(114,6),604,30))) {
                    val v=page(a,n);press(a,v,id)
                    val d=a.ayahDialog.dialog;val source=repository.getAyah(id.surahNumber,id.ayahNumber)!!.text
                    assertEquals(source,d.findViewById<TextView>(R.id.ayah_dialog_text)!!.text.toString())
                    assertEquals(QuranSvgEdition.ID,a.ayahDialog.editionId);assertEquals(n,a.ayahDialog.pageNumber)
                    assertEquals(a.getString(R.string.ayah_juz,juz),d.findViewById<TextView>(R.id.ayah_dialog_juz)!!.text.toString())
                    assertEquals(a.getString(R.string.ayah_surah,repository.getSurahs().first { it.surahNumber==id.surahNumber }.name),d.findViewById<TextView>(R.id.ayah_dialog_surah)!!.text.toString())
                    assertEquals(Gravity.CENTER,d.window!!.attributes.gravity);assertEquals(.22f,d.window!!.attributes.dimAmount,.001f)
                    assertEquals(View.LAYOUT_DIRECTION_RTL,d.findViewById<View>(R.id.ayah_dialog)!!.layoutDirection)
                    assertFalse(d.findViewById<Button>(R.id.ayah_dialog_play)!!.isEnabled);assertFalse(d.findViewById<Button>(R.id.ayah_dialog_tafsir)!!.isEnabled)
                    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2200));assertEquals(id,v.selectedAyah)
                    if(n==121) {
                        assertEquals(AyahId(5,77),repository.getPage(QuranRepository.DEFAULT_EDITION,121)!!.ayahs.first().id)
                        assertEquals(AyahId(5,78),v.page!!.geometry.regions.first().ayahId)
                    }
                    if(id==AyahId(1,1)) capture(a,"popup-short")
                    if(id==AyahId(2,282)) {
                        val scroll=d.findViewById<NestedScrollView>(R.id.ayah_dialog_scroll)!!
                        if(android.os.Build.VERSION.SDK_INT>=26) assertTrue("Long canonical text must scroll",scroll.canScrollVertically(1))
                        assertTrue(d.window!!.attributes.height<=height*.80f)
                        capture(a,"popup-long")
                        scroll.scrollTo(0,scroll.getChildAt(0).height)
                        if(android.os.Build.VERSION.SDK_INT>=26) assertTrue(scroll.scrollY>0)
                        assertEquals(source,d.findViewById<TextView>(R.id.ayah_dialog_text)!!.text.toString())
                    }
                    d.findViewById<Button>(R.id.ayah_dialog_copy)!!.performClick()
                    val clip=a.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    assertEquals(source,clip.primaryClip!!.getItemAt(0).text.toString())
                    d.findViewById<Button>(R.id.ayah_dialog_share)!!.performClick()
                    val chooser=shadowOf(a).nextStartedActivity;assertEquals(Intent.ACTION_CHOOSER,chooser.action)
                    @Suppress("DEPRECATION") val send=chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
                    assertEquals(Intent.ACTION_SEND,send.action);assertEquals("text/plain",send.type)
                    assertEquals(source,send.getStringExtra(Intent.EXTRA_TEXT));assertEquals(setOf(Intent.EXTRA_TEXT),send.extras!!.keySet())
                    report.put(JSONObject().put("ayah","${id.surahNumber}:${id.ayahNumber}").put("page",n).put("juz",juz).put("canonicalExact",true).put("copyExact",true).put("shareExact",true))
                    a.onBackPressedDispatcher.onBackPressed();settle(a)
                    assertFalse(a.ayahDialog.isOpen);assertNull(v.selectedAyah)
                    assertEquals(Screen.READER,ViewModelProvider(a)[MainViewModel::class.java].state.value.screen)
                }
            }
            output().resolve("canonical-api-${android.os.Build.VERSION.SDK_INT}.json").writeText(report.toString(2))
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun singleTapSelectsOnlyAndBlankTapExpandsViewportWithoutBreakingIdentityZoomOrSwipe() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup();val a=controller.get()
        try {
            ready(a);val v=page(a,48);val id=AyahId(2,282);val p=point(a,v,id)
            touch(v,p,false);settle(a);assertEquals(id,v.selectedAyah);assertFalse(a.ayahDialog.isOpen)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2500));assertEquals(id,v.selectedAyah)
            capture(a,"long-line-highlight")
            val blank=PagePoint(1f,1f);assertNull(v.hitTest(blank.x,blank.y))
            val originalSize=v.width to v.height
            touch(v,blank,false);settle(a)
            for(r in listOf(R.id.top_bar,R.id.bottom_bar)) assertEquals(View.GONE,a.findViewById<View>(r).visibility)
            assertEquals(originalSize.first,v.width);assertTrue(v.height>originalSize.second)
            val enlarged=point(a,v,id);assertEquals(id,v.hitTest(enlarged.x,enlarged.y));assertFalse(a.ayahDialog.isOpen)
            assertNull(v.hitTest(blank.x,blank.y))
            touch(v,blank,false);settle(a);assertEquals(View.VISIBLE,a.findViewById<View>(R.id.top_bar).visibility)
            assertEquals(originalSize,v.width to v.height)
            for(zoom in listOf(1f,2f,4f)) {
                v.setZoom(zoom,p.x,p.y);val z=point(a,v,id);assertEquals(id,v.hitTest(z.x,z.y));touch(v,z,false)
                assertEquals(id,v.selectedAyah);assertFalse(a.ayahDialog.isOpen)
            }
            fun swipe() {
                val now=SystemClock.uptimeMillis();val y=v.height/2f
                for((action,x,time) in listOf(Triple(MotionEvent.ACTION_DOWN,20f,now),Triple(MotionEvent.ACTION_MOVE,200f,now+30),Triple(MotionEvent.ACTION_UP,500f,now+60))) {
                    val e=MotionEvent.obtain(now,time,action,x,y,0);v.dispatchTouchEvent(e);e.recycle()
                }
            }
            swipe();settle(a);assertEquals(48,v.page!!.geometry.pageNumber)
            v.setZoom(1f)
            pinch(v)
            assertFalse(a.ayahDialog.isOpen)
            assertEquals(View.VISIBLE,a.findViewById<View>(R.id.top_bar).visibility)
            assertEquals(id,v.selectedAyah)
            val zoomedPoint=point(a,v,id);assertEquals(id,v.hitTest(zoomedPoint.x,zoomedPoint.y))
            v.setZoom(1f);val now=SystemClock.uptimeMillis();val y=v.height/2f
            for((action,x,time) in listOf(Triple(MotionEvent.ACTION_DOWN,20f,now),Triple(MotionEvent.ACTION_MOVE,200f,now+30),Triple(MotionEvent.ACTION_UP,500f,now+60))) {
                val e=MotionEvent.obtain(now,time,action,x,y,0);v.dispatchTouchEvent(e);e.recycle()
            }
            await { v.page?.geometry?.pageNumber==49 };layout(a);assertFalse(a.ayahDialog.isOpen);assertNull(v.selectedAyah)
        } finally { controller.pause().stop().destroy() }
    }

    @Test fun naturalLongPressUpdatesOneDialogAndCloseBackOrOutsideCancelClearsSelection() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup();val a=controller.get()
        try {
            ready(a);val v=page(a,1);val p=point(a,v,AyahId(1,1));val now=SystemClock.uptimeMillis()
            val down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,p.x,p.y,0);v.dispatchTouchEvent(down);down.recycle()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100));assertFalse(a.ayahDialog.isOpen)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis((ViewConfiguration.getLongPressTimeout()+ViewConfiguration.getTapTimeout()).toLong()))
            await { a.ayahDialog.isOpen };assertEquals(AyahId(1,1),a.ayahDialog.ayahId)
            val up=MotionEvent.obtain(now,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,p.x,p.y,0);v.dispatchTouchEvent(up);up.recycle()
            await { a.ayahDialog.dialog.findViewById<Button>(R.id.ayah_dialog_copy)!!.isEnabled }
            val original=a.ayahDialog.dialog;press(a,v,AyahId(1,7));assertSame(original,a.ayahDialog.dialog)
            original.findViewById<Button>(R.id.ayah_dialog_close)!!.performClick();settle(a);assertFalse(a.ayahDialog.isOpen);assertNull(v.selectedAyah)
            press(a,v,AyahId(1,1))
            // API23 closes on outside DOWN; modern Android closes on UP. Replay the full tap.
            var consumed=false
            for(action in listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP)) {
                val outside=MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),action,-100f,-100f,0)
                consumed=original.onTouchEvent(outside) || consumed;outside.recycle()
            }
            assertTrue(consumed);settle(a)
            assertFalse(a.ayahDialog.isOpen);assertNull(v.selectedAyah)
            press(a,v,AyahId(1,7));original.onBackPressedDispatcher.onBackPressed();settle(a)
            assertFalse(a.ayahDialog.isOpen);assertNull(v.selectedAyah);assertEquals(Screen.READER,ViewModelProvider(a)[MainViewModel::class.java].state.value.screen)
        } finally { controller.pause().stop().destroy() }
    }

    private fun pinch(v:NativeSvgPageView) {
        val before=v.screenPoint(PagePoint(20f,20f)).x-v.screenPoint(PagePoint(10f,20f)).x
        val now=SystemClock.uptimeMillis();val x=v.width/2f;val y=v.height/2f
        val properties=Array(2) { index -> MotionEvent.PointerProperties().apply { id=index;toolType=MotionEvent.TOOL_TYPE_FINGER } }
        fun event(action:Int,count:Int,span:Float,time:Long) {
            val coords=Array(count) { index -> MotionEvent.PointerCoords().apply {
                this.x=x+if(index==0)-span else span;this.y=y;pressure=1f;size=1f
            } }
            val e=MotionEvent.obtain(now,time,action,count,properties,coords,0,0,1f,1f,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0)
            v.dispatchTouchEvent(e);e.recycle()
        }
        event(MotionEvent.ACTION_DOWN,1,80f,now)
        event(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),2,80f,now+30)
        event(MotionEvent.ACTION_MOVE,2,180f,now+60)
        event(MotionEvent.ACTION_MOVE,2,220f,now+90)
        event(MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),2,220f,now+120)
        event(MotionEvent.ACTION_UP,1,220f,now+150)
        val after=v.screenPoint(PagePoint(20f,20f)).x-v.screenPoint(PagePoint(10f,20f)).x
        assertTrue("Two-pointer pinch must still scale the existing matrix",after>before)
    }

    private fun output()=File(requireNotNull(System.getProperty("quran.sourceDir")),"../../reader-ui").apply { mkdirs() }
    private fun capture(a:MainActivity,name:String) {
        if(android.os.Build.VERSION.SDK_INT!=36)return
        settle(a)
        val decor=if(a.ayahDialog.isOpen)a.ayahDialog.dialog.window!!.decorView else a.window.decorView
        val b=Bitmap.createBitmap(decor.width,decor.height,Bitmap.Config.ARGB_8888);decor.draw(Canvas(b))
        val folder=output().resolve("screenshots/simulated-api36").apply { mkdirs() }
        folder.resolve("$name.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG,100,it) };b.recycle()
    }
}
