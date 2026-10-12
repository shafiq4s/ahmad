package com.ahmad.quran.ui

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ahmad.quran.R
import com.ahmad.quran.mushaf.svg.NativeSvgPageView
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: MainViewModel
    private lateinit var backCallback: OnBackPressedCallback
    private lateinit var surahAdapter: SurahAdapter
    lateinit var reader: MushafReaderController
        private set
    private lateinit var pageView: NativeSvgPageView

    override fun attachBaseContext(newBase: Context) {
        val arabic=Locale.forLanguageTag("ar")
        val configuration=Configuration(newBase.resources.configuration).apply { setLocale(arabic);setLayoutDirection(arabic) }
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        val createdAt=System.nanoTime()
        delegate.localNightMode=if(ReadingPreferences(this).darkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window,false)
        setContentView(R.layout.activity_main)
        val root=findViewById<View>(R.id.app_root)
        ViewCompat.setAccessibilityHeading(findViewById(R.id.screen_title),true)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view,insets ->
            val bars=insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left,bars.top,bars.right,bars.bottom);insets
        }
        ViewCompat.requestApplyInsets(root)
        viewModel=ViewModelProvider(this)[MainViewModel::class.java]
        pageView=findViewById(R.id.mushaf_page)
        reader=MushafReaderController(this,pageView,createdAt,{ number ->
            viewModel.recordPageDisplayed(number)
            findViewById<View>(R.id.loading).isVisible=false
        },{
            findViewById<View>(R.id.loading).isVisible=false
            findViewById<View>(R.id.load_error).isVisible=true
            findViewById<View>(R.id.retry_load).isVisible=true
        })
        pageView.onPageSwipe={ step -> viewModel.openPage(viewModel.state.value.pageNumber+step) }
        pageView.onAyahLongPress={ id ->
            findViewById<TextView>(R.id.selected_ayah).apply {
                text=getString(R.string.selected_ayah,id.surahNumber,id.ayahNumber);isVisible=true
            }
        }
        listOf(R.id.zoom_1 to 1f,R.id.zoom_2 to 2f,R.id.zoom_4 to 4f).forEach { (id,zoom) ->
            findViewById<Button>(id).setOnClickListener { pageView.setZoom(zoom) }
        }
        surahAdapter=SurahAdapter(this)
        findViewById<ListView>(R.id.surah_list).apply {
            adapter=surahAdapter
            setOnItemClickListener { _,_,position,_ -> viewModel.openSurah(surahAdapter.getItem(position).surahNumber) }
        }
        backCallback=object:OnBackPressedCallback(false) {
            override fun handleOnBackPressed() { viewModel.navigateTo(Screen.QURAN) }
        }
        onBackPressedDispatcher.addCallback(this,backCallback)
        findViewById<Button>(R.id.nav_quran).setOnClickListener { viewModel.navigateTo(Screen.QURAN) }
        findViewById<Button>(R.id.nav_settings).setOnClickListener { viewModel.navigateTo(Screen.SETTINGS) }
        findViewById<Button>(R.id.continue_reading).setOnClickListener { viewModel.resumeReading() }
        findViewById<Button>(R.id.read_pages).setOnClickListener { viewModel.openPage(1) }
        findViewById<SwitchCompat>(R.id.dark_mode).setOnCheckedChangeListener { _,enabled ->
            if (enabled!=viewModel.state.value.darkMode) {
                viewModel.setDarkMode(enabled)
                delegate.localNightMode=if(enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            }
        }
        findViewById<Button>(R.id.retry_load).setOnClickListener { viewModel.retry() }
        findViewById<Button>(R.id.previous_page).setOnClickListener { viewModel.openPage(viewModel.state.value.pageNumber-1) }
        findViewById<Button>(R.id.next_page).setOnClickListener { viewModel.openPage(viewModel.state.value.pageNumber+1) }
        findViewById<Button>(R.id.go_page).setOnClickListener {
            val input=findViewById<EditText>(R.id.page_number)
            val page=input.text.toString().toIntOrNull()
            if (page==null || page !in 1..604) input.error=getString(R.string.page_invalid,604)
            else {
                input.error=null;input.clearFocus()
                WindowCompat.getInsetsController(window,input).hide(WindowInsetsCompat.Type.ime())
                viewModel.openPage(page)
            }
        }
        findViewById<Button>(R.id.show_license).setOnClickListener {
            findViewById<TextView>(R.id.license_notice).apply { isVisible=!isVisible }
        }
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.state.collect(::render) } }
    }
    private fun render(state: QuranUiState) {
        val reading=state.screen==Screen.READER
        findViewById<View>(R.id.app_label).isVisible=!reading
        findViewById<TextView>(R.id.screen_title).apply {
            isVisible=state.screen==Screen.SETTINGS
            text=getString(R.string.settings)
        }
        surahAdapter.submit(state.surahs,state.revelationTypes)
        mapOf(Screen.QURAN to R.id.quran_panel,Screen.READER to R.id.reader_panel,Screen.SETTINGS to R.id.settings_panel).forEach { (screen,id) ->
            findViewById<View>(id).isVisible=state.screen==screen
        }
        mapOf(Screen.QURAN to R.id.nav_quran,Screen.SETTINGS to R.id.nav_settings).forEach { (screen,id) ->
            findViewById<Button>(id).isEnabled=state.screen!=screen
        }
        if (reading && !state.loading && !state.error) {
            if (pageView.page?.geometry?.pageNumber!=state.pageNumber) findViewById<View>(R.id.selected_ayah).isVisible=false
            reader.show(state.pageNumber)
        } else if (!reading) reader.leave()
        findViewById<View>(R.id.loading).isVisible=state.loading || (reading && !reader.displaying && !state.error)
        findViewById<View>(R.id.load_error).isVisible=state.error
        findViewById<View>(R.id.retry_load).isVisible=state.error
        findViewById<TextView>(R.id.edition_name).text=state.edition?.name.orEmpty()
        findViewById<TextView>(R.id.reader_location).text=state.juzNumber?.let { getString(R.string.page_juz,state.pageNumber,604,it) }
            ?: getString(R.string.page_location,state.pageNumber,604)
        findViewById<Button>(R.id.continue_reading).apply { isVisible=state.lastLocation!=null;isEnabled=!state.loading }
        findViewById<Button>(R.id.read_pages).isEnabled=!state.loading && state.edition!=null
        findViewById<TextView>(R.id.last_location).apply {
            isVisible=state.lastLocation!=null
            text=state.lastLocation?.let { if(it.editionId==state.edition?.editionId) getString(R.string.resume_page,it.pageNumber)
                else getString(R.string.resume_surah,state.surahs.firstOrNull { s -> s.surahNumber==it.surahNumber }?.name.orEmpty(),it.ayahNumber) }.orEmpty()
        }
        findViewById<SwitchCompat>(R.id.dark_mode).isChecked=state.darkMode
        WindowCompat.getInsetsController(window,findViewById(R.id.app_root)).isAppearanceLightStatusBars=!state.darkMode
        val input=findViewById<EditText>(R.id.page_number)
        if (!input.hasFocus()) input.setText(getString(R.string.page_value,state.pageNumber))
        findViewById<Button>(R.id.previous_page).isEnabled=!state.loading && state.pageNumber>1
        findViewById<Button>(R.id.next_page).isEnabled=!state.loading && state.pageNumber<604
        findViewById<Button>(R.id.go_page).isEnabled=!state.loading
        findViewById<TextView>(R.id.license_notice).text=state.notice
        backCallback.isEnabled=state.screen!=Screen.QURAN
    }
    override fun onPause() { if (::reader.isInitialized) reader.saveMeasurements();super.onPause() }
    override fun onDestroy() { if (::reader.isInitialized) reader.close();super.onDestroy() }
}
