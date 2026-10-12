package com.ahmad.quran

import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.ViewModelProvider
import com.ahmad.quran.data.AyahId
import com.ahmad.quran.data.QuranRepository
import com.ahmad.quran.mushaf.svg.NativeSvgPageView
import com.ahmad.quran.mushaf.svg.PagePoint
import com.ahmad.quran.mushaf.svg.QuranSvgEdition
import com.ahmad.quran.ui.*
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[23,36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MainActivityTest {
    @org.junit.Before fun installLocalRenderingFixtures() {
        com.ahmad.quran.SourcePageFixtures.install(org.robolectric.RuntimeEnvironment.getApplication())
    }
    private fun await(condition:()->Boolean) {
        val deadline=System.nanoTime()+20_000_000_000L
        while (!condition() && System.nanoTime()<deadline) { shadowOf(Looper.getMainLooper()).idle();Thread.sleep(10) }
        assertTrue("Background load did not complete",condition())
    }
    private fun ready(activity: MainActivity) {
        await { activity.findViewById<ListView>(R.id.surah_list).adapter.count==114 && !ViewModelProvider(activity)[MainViewModel::class.java].state.value.loading }
    }
    private fun draw(activity:MainActivity) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        val root=activity.window.decorView
        root.measure(View.MeasureSpec.makeMeasureSpec(412,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY))
        root.layout(0,0,412,800)
        val view=activity.findViewById<NativeSvgPageView>(R.id.mushaf_page)
        assertTrue(view.width>0 && view.height>0)
        val image=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        view.draw(Canvas(image));image.recycle()
        shadowOf(Looper.getMainLooper()).idle()
    }
    private fun page(activity:MainActivity,number:Int):NativeSvgPageView {
        ViewModelProvider(activity)[MainViewModel::class.java].openPage(number)
        val view=activity.findViewById<NativeSvgPageView>(R.id.mushaf_page)
        await { view.page?.geometry?.pageNumber==number }
        draw(activity)
        return view
    }
    private fun press(view:NativeSvgPageView,p:PagePoint) {
        val start=SystemClock.uptimeMillis()
        val down=MotionEvent.obtain(start,start,MotionEvent.ACTION_DOWN,p.x,p.y,0)
        view.dispatchTouchEvent(down);down.recycle()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(850))
        val up=MotionEvent.obtain(start,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,p.x,p.y,0)
        view.dispatchTouchEvent(up);up.recycle()
    }
    @Test fun offlineIndexIsArabicAndStartupParsesZeroSvgDocuments() {
        val app=RuntimeEnvironment.getApplication()
        Settings.Global.putInt(app.contentResolver,Settings.Global.AIRPLANE_MODE_ON,1)
        assertEquals(PackageManager.PERMISSION_GRANTED,app.packageManager.checkPermission("android.permission.INTERNET",app.packageName))
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=controller.get();ready(a)
            assertEquals(View.LAYOUT_DIRECTION_RTL,a.findViewById<View>(R.id.app_root).layoutDirection)
            assertEquals("ar",a.resources.configuration.localesCompat())
            assertEquals(0,a.reader.parsedDocumentCount);assertEquals(0,a.reader.retainedDocumentCount)
            val list=a.findViewById<ListView>(R.id.surah_list)
            a.findViewById<View>(R.id.open_surahs).performClick();shadowOf(Looper.getMainLooper()).idle()
            assertEquals(114,list.adapter.count)
            val row=list.adapter.getView(1,null,list)
            assertTrue(row.findViewById<TextView>(R.id.surah_details).text.contains(a.getString(R.string.medinan)))
            for(surah in listOf(1,2,3,112,114)) {
                a.findViewById<View>(R.id.open_surahs).performClick();shadowOf(Looper.getMainLooper()).idle()
                list.performItemClick(list.adapter.getView(surah-1,null,list),surah-1,surah.toLong())
                val target=QuranSvgEdition(a).firstPage(surah)
                await { a.findViewById<NativeSvgPageView>(R.id.mushaf_page).page?.geometry?.pageNumber==target }
                draw(a)
                assertEquals(target,ViewModelProvider(a)[MainViewModel::class.java].state.value.pageNumber)
                assertTrue(a.reader.retainedDocumentCount<=3)
                a.onBackPressedDispatcher.onBackPressed();shadowOf(Looper.getMainLooper()).idle()
            }
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun referencePagesLongPressAndZoomUseCanonicalIdentity() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=controller.get();ready(a)
            for(number in listOf(1,2,48,121,604)) {
                val view=page(a,number)
                val rows=JSONArray(a.assets.open("mushaf-svg/%03d.json".format(java.util.Locale.ROOT,number)).bufferedReader().use { it.readText() })
                val selected=if(number==1 || number==2)(0 until rows.length()).toList() else listOf(if(number==604)rows.length()-1 else 0)
                for(i in selected) {
                    val row=rows.getJSONObject(i);val id=AyahId(row.getInt("surahNumber"),row.getInt("ayahNumber"))
                    val point=PagePoint(row.getDouble("x").toFloat(),row.getDouble("y").toFloat())
                    for(zoom in listOf(1f,2f,4f)) {
                        view.setZoom(1f);val focus=view.screenPoint(point);view.setZoom(zoom,focus.x,focus.y)
                        press(view,view.screenPoint(point));assertEquals(id,view.selectedAyah)
                        assertEquals(a.getString(R.string.selected_ayah,id.surahNumber,id.ayahNumber),a.findViewById<TextView>(R.id.selected_ayah).text.toString())
                    }
                }
                if(number==48) {
                    view.setZoom(1f)
                    for(p in listOf(PagePoint(120f,18f),PagePoint(120f,250f),PagePoint(120f,490f))) {
                        assertEquals(AyahId(2,282),view.hitTest(view.screenPoint(p).x,view.screenPoint(p).y))
                    }
                }
                if(number==121) assertEquals(AyahId(5,78),view.page!!.geometry.regions.first().ayahId)
                if(number==604) assertEquals(AyahId(114,6),view.page!!.geometry.regions.last().ayahId)
            }
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun rtlSwipeButtonsBoundsAndFarNavigationWork() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val a=controller.get();ready(a);var view=page(a,1)
            assertFalse(a.findViewById<View>(R.id.previous_page).isEnabled)
            val now=SystemClock.uptimeMillis();val y=view.height/2f
            for((action,x,time) in listOf(Triple(MotionEvent.ACTION_DOWN,20f,now),Triple(MotionEvent.ACTION_MOVE,100f,now+30),Triple(MotionEvent.ACTION_UP,300f,now+60))) {
                val e=MotionEvent.obtain(now,time,action,x,y,0);view.dispatchTouchEvent(e);e.recycle()
            }
            await { view.page?.geometry?.pageNumber==2 };draw(a)
            view.onPageSwipe!!.invoke(-1);await { view.page?.geometry?.pageNumber==1 };draw(a)
            for(n in listOf(604,25,400,100,600,121)) ViewModelProvider(a)[MainViewModel::class.java].openPage(n)
            await { view.page?.geometry?.pageNumber==121 };draw(a)
            assertEquals(121,ReadingPreferences(a).lastLocation!!.pageNumber)
            page(a,604);assertFalse(a.findViewById<View>(R.id.next_page).isEnabled)
            a.findViewById<View>(R.id.previous_page).performClick();await { view.page?.geometry?.pageNumber==603 };draw(a)
            a.findViewById<Button>(R.id.go_page).performClick()
            val jump=checkNotNull(a.pageJumpDialog)
            val input=checkNotNull(jump.findViewById<EditText>(R.id.page_number))
            input.setText("605");jump.findViewById<Button>(R.id.jump_confirm)!!.performClick()
            assertNotNull(input.error)
            input.setText("604");jump.findViewById<Button>(R.id.jump_confirm)!!.performClick()
            await { view.page?.geometry?.pageNumber==604 };draw(a)
            assertFalse(jump.isShowing)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun resumeRecreationAndLegacyTanzilLocationMigrateByAyah() {
        val app=RuntimeEnvironment.getApplication()
        ReadingPreferences(app).saveLocation(ReadingLocation(ReaderMode.PAGE,5,77,121,QuranRepository.DEFAULT_EDITION))
        var controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            var a=controller.get();ready(a);a.findViewById<Button>(R.id.continue_reading).performClick()
            val view=a.findViewById<NativeSvgPageView>(R.id.mushaf_page)
            await { view.page?.geometry?.pageNumber==120 };draw(a)
            page(a,400);controller.recreate();a=controller.get();ready(a)
            await { a.findViewById<NativeSvgPageView>(R.id.mushaf_page).page?.geometry?.pageNumber==400 };draw(a)
            controller.pause().stop().destroy();controller=Robolectric.buildActivity(MainActivity::class.java).setup()
            a=controller.get();ready(a);a.findViewById<Button>(R.id.continue_reading).performClick()
            await { a.findViewById<NativeSvgPageView>(R.id.mushaf_page).page?.geometry?.pageNumber==400 };draw(a)
            assertEquals(QuranSvgEdition.ID,ReadingPreferences(a).lastLocation!!.editionId)
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun localDarkSettingAndBothAttributionsRemainAvailable() {
        val controller=Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            var a=controller.get();ready(a);a.findViewById<View>(R.id.nav_settings).performClick();shadowOf(Looper.getMainLooper()).idle()
            a.findViewById<SwitchCompat>(R.id.dark_mode).isChecked=true;shadowOf(Looper.getMainLooper()).idle()
            assertTrue(ReadingPreferences(a).darkMode)
            controller.recreate();a=controller.get();ready(a)
            assertEquals(Configuration.UI_MODE_NIGHT_YES,a.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
            assertTrue(a.findViewById<TextView>(R.id.tanzil_attribution).text.contains("https://tanzil.net"))
            assertTrue(a.findViewById<TextView>(R.id.quran_svg_attribution).text.contains("مشروع مستقل"))
            a.findViewById<Button>(R.id.show_license).performClick()
            assertTrue(a.findViewById<TextView>(R.id.license_notice).text.contains("Tanzil Project"))
            assertTrue(a.findViewById<TextView>(R.id.license_notice).text.contains("CC BY 4.0"))
        } finally { controller.pause().stop().destroy() }
    }
    @Suppress("DEPRECATION") private fun Configuration.localesCompat()=locale.language
}
