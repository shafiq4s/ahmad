package com.ahmad.quran

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.ahmad.quran.data.AyahId
import com.ahmad.quran.mushaf.svg.NativeSvgPageView
import com.ahmad.quran.mushaf.svg.QuranSvgEdition
import com.ahmad.quran.ui.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[23,36],qualifiers="ar-w360dp-h640dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReadingExperienceUiTest {
    @org.junit.Before fun fixtures() { SourcePageFixtures.install(RuntimeEnvironment.getApplication()) }
    private fun await(condition:()->Boolean) {
        val end=System.nanoTime()+20_000_000_000L
        while(!condition() && System.nanoTime()<end) { shadowOf(Looper.getMainLooper()).idle();Thread.sleep(10) }
        assertTrue("Reading experience state did not arrive",condition())
    }
    private fun layout(a:MainActivity) {
        a.window.decorView.apply { measure(View.MeasureSpec.makeMeasureSpec(720,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1280,View.MeasureSpec.EXACTLY));layout(0,0,720,1280) }
    }
    private fun ready(a:MainActivity) { await { !ViewModelProvider(a)[MainViewModel::class.java].state.value.loading };layout(a) }
    private fun page(a:MainActivity,n:Int):NativeSvgPageView {
        ViewModelProvider(a)[MainViewModel::class.java].openPage(n)
        val v=a.findViewById<NativeSvgPageView>(R.id.mushaf_page)
        await { v.page?.geometry?.pageNumber==n };layout(a)
        val bitmap=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888);v.draw(Canvas(bitmap));bitmap.recycle()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(400));layout(a)
        return v
    }
    @Test fun resumeCardSearchBookmarksModalAndJumpWorkInRtl() {
        val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get()
        try {
            ready(a);page(a,121)
            assertTrue(ReadingPreferences(a).lastReadAtMillis>0)
            a.findViewById<View>(R.id.toggle_bookmark).performClick()
            assertEquals(121,ReadingPreferences(a).bookmarks.single().pageNumber)
            ViewModelProvider(a)[MainViewModel::class.java].navigateTo(Screen.HOME);shadowOf(Looper.getMainLooper()).idle();layout(a)
            assertTrue(a.findViewById<TextView>(R.id.last_location).text.contains("المائدة"))
            capture(a.window.decorView,"01-home")
            a.findViewById<View>(R.id.open_surahs).performClick();shadowOf(Looper.getMainLooper()).idle();layout(a)
            val search=a.findViewById<EditText>(R.id.surah_search);search.setText("الاخلاص")
            val list=a.findViewById<ListView>(R.id.surah_list);assertEquals(1,list.adapter.count)
            assertEquals(112,list.adapter.getItemId(0).toInt())
            search.requestFocus()
            list.performItemClick(list.adapter.getView(0,null,list),0,112)
            page(a,604);assertFalse(search.hasFocus())
            ViewModelProvider(a)[MainViewModel::class.java].navigateTo(Screen.HOME)
            shadowOf(Looper.getMainLooper()).idle();search.setText("")
            a.findViewById<View>(R.id.open_bookmarks).performClick();shadowOf(Looper.getMainLooper()).idle();layout(a)
            capture(a.window.decorView,"02-bookmarks")
            val bookmarks=a.findViewById<ListView>(R.id.bookmarks_list)
            bookmarks.performItemClick(bookmarks.adapter.getView(0,null,bookmarks),0,121)
            await { a.findViewById<NativeSvgPageView>(R.id.mushaf_page).page?.geometry?.pageNumber==121 }
            page(a,121);capture(a.window.decorView,"03-reader")
            val details=runBlockingDetails(a,AyahId(5,78))
            a.ayahDialog.begin(AyahId(5,78),"المائدة",QuranSvgEdition.ID,121);a.ayahDialog.bind(details)
            a.ayahDialog.dialog.findViewById<Button>(R.id.ayah_dialog_bookmark)!!.performClick()
            assertTrue(ReadingPreferences(a).bookmarks.isEmpty());assertTrue(a.ayahDialog.isOpen)
            a.ayahDialog.dialog.findViewById<Button>(R.id.ayah_dialog_bookmark)!!.performClick()
            assertEquals(78,ReadingPreferences(a).bookmarks.single().ayahNumber);a.ayahDialog.dismiss()
            a.findViewById<View>(R.id.go_page).performClick()
            val jump=checkNotNull(a.pageJumpDialog);jump.findViewById<RadioGroup>(R.id.jump_mode)!!.check(R.id.jump_mode_juz)
            jump.findViewById<Spinner>(R.id.jump_selection)!!.setSelection(6)
            assertEquals(30,jump.findViewById<Spinner>(R.id.jump_selection)!!.count)
            capture(jump.window!!.decorView,"04-jump")
            jump.findViewById<View>(R.id.jump_confirm)!!.performClick()
            await { !ViewModelProvider(a)[MainViewModel::class.java].state.value.loading }
            assertEquals(121,ViewModelProvider(a)[MainViewModel::class.java].state.value.pageNumber)
            a.findViewById<View>(R.id.go_page).performClick()
            val again=checkNotNull(a.pageJumpDialog);again.findViewById<RadioGroup>(R.id.jump_mode)!!.check(R.id.jump_mode_surah)
            assertEquals(114,again.findViewById<Spinner>(R.id.jump_selection)!!.count)
            again.findViewById<Spinner>(R.id.jump_selection)!!.setSelection(1)
            again.findViewById<View>(R.id.jump_confirm)!!.performClick()
            await { a.findViewById<NativeSvgPageView>(R.id.mushaf_page).page?.geometry?.pageNumber==2 }
            a.findViewById<View>(R.id.nav_settings).performClick();shadowOf(Looper.getMainLooper()).idle();layout(a)
            captureSettings(a)
            assertTrue(a.findViewById<TextView>(R.id.quran_svg_attribution).text.contains("مشروع مستقل"))
        } finally { c.pause().stop().destroy() }
    }
    private fun runBlockingDetails(a:MainActivity,id:AyahId)=kotlinx.coroutines.runBlocking { ViewModelProvider(a)[MainViewModel::class.java].getAyahDetails(id) }
    @Test fun optionsPersistAndApplyOnlyToReaderAndAutoResumeDoesNotOverrideRestoredScreen() {
        val app=RuntimeEnvironment.getApplication();val p=ReadingPreferences(app)
        p.saveLocation(ReadingLocation(ReaderMode.PAGE,16,1,300,QuranSvgEdition.ID));p.setAutoResumeReading(true)
        val c=Robolectric.buildActivity(MainActivity::class.java).setup();var a=c.get()
        try {
            await { a.findViewById<NativeSvgPageView>(R.id.mushaf_page).page?.geometry?.pageNumber==300 };page(a,300)
            a.findViewById<View>(R.id.nav_settings).performClick();shadowOf(Looper.getMainLooper()).idle();layout(a)
            a.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.keep_screen_awake).isChecked=true
            a.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.show_reader_bars).isChecked=false
            assertTrue(ReadingPreferences(a).keepScreenAwake);assertFalse(ReadingPreferences(a).showReaderBarsOnOpen)
            assertEquals(0,a.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            page(a,1);assertTrue(a.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON!=0)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(400));layout(a)
            assertEquals(View.GONE,a.findViewById<View>(R.id.top_bar).visibility)
            ViewModelProvider(a)[MainViewModel::class.java].navigateTo(Screen.SETTINGS);shadowOf(Looper.getMainLooper()).idle()
            assertEquals(0,a.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            c.recreate();a=c.get();ready(a)
            assertEquals(Screen.SETTINGS,ViewModelProvider(a)[MainViewModel::class.java].state.value.screen)
        } finally { c.pause().stop().destroy() }
    }
    @Test fun cacheClearConfirmationPreservesStateCanonicalDataAndBundledAssets() {
        val c=Robolectric.buildActivity(MainActivity::class.java).setup();val a=c.get()
        try {
            ready(a);page(a,121);a.findViewById<View>(R.id.toggle_bookmark).performClick()
            a.findViewById<View>(R.id.nav_settings).performClick();shadowOf(Looper.getMainLooper()).idle();layout(a)
            val prefs=ReadingPreferences(a);val last=prefs.lastLocation;val bookmarks=prefs.bookmarks
            a.findViewById<View>(R.id.cache_clear).performClick();shadowOf(Looper.getMainLooper()).idle()
            val dialog=org.robolectric.shadows.ShadowDialog.getLatestDialog() as AlertDialog
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            await { a.findViewById<TextView>(R.id.cache_feedback).text==a.getString(R.string.cache_cleared) }
            assertEquals(0L,DownloadedPageCache(a).sizeBytes());assertEquals(last,prefs.lastLocation);assertEquals(bookmarks,prefs.bookmarks)
            assertTrue(a.assets.open("mushaf-svg/001.svg").use { it.read()>=0 });assertTrue(a.assets.open("mushaf-svg/121.json").use { it.read()>=0 })
            com.ahmad.quran.data.QuranRepository.open(a).use { assertEquals(6236,it.getSurahs().sumOf { s -> s.ayahCount }) }
        } finally { c.pause().stop().destroy() }
    }
    private fun capture(view:View,name:String) {
        if(android.os.Build.VERSION.SDK_INT!=36)return
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300))
        val bitmap=Bitmap.createBitmap(view.width.coerceAtLeast(1),view.height.coerceAtLeast(1),Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap)
        canvas.drawColor(androidx.core.content.ContextCompat.getColor(view.context,R.color.surface))
        view.draw(canvas)
        val root=File(requireNotNull(System.getProperty("quran.sourceDir")),"../../reading-experience/screenshots/simulated-api36").apply { mkdirs() }
        File(root,"$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
    }
    private fun captureSettings(a:MainActivity) {
        val view=a.findViewById<View>(R.id.settings_content)
        view.measure(View.MeasureSpec.makeMeasureSpec(720,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED))
        view.layout(0,0,720,view.measuredHeight);capture(view,"05-settings-support-about")
    }
}
