package com.ahmad.quran.ui

import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Spinner
import android.widget.RadioGroup
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.text.format.Formatter
import android.os.Build
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.core.view.doOnLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDialog
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
import kotlinx.coroutines.Job

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: MainViewModel
    private lateinit var backCallback: OnBackPressedCallback
    private lateinit var bookmarkAdapter: BookmarkAdapter
    private lateinit var historyAdapter: ReadingHistoryAdapter
    private lateinit var searchAdapter: SearchResultAdapter
    private var launchComplete = false
    private var previousScreen: Screen? = null
    private var cacheRefresh: Job? = null
    private var cacheClearBusy = false
    private val pageSourceConfigured by lazy { assets.open("mushaf-svg/page-assets-config.json").bufferedReader().use { JSONObject(it.readText()).getString("baseUrl").isNotBlank() } }
    private lateinit var surahAdapter: SurahAdapter
    lateinit var reader: MushafReaderController
        private set
    private lateinit var pageView: NativeSvgPageView
    lateinit var ayahDialog: AyahOptionsDialog
        private set
    private var ayahLoad: Job? = null
    private var readerChromeVisible = true
    private var chromeFraction = 1f
    private var chromeAnimation: ValueAnimator? = null
    private var topHeight = 0
    private var bottomHeight = 0
    private var displayedPage: Int? = null
    var pageJumpDialog: AppCompatDialog? = null
        private set

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(InterfaceLocales.wrap(newBase))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        val createdAt=System.nanoTime()
        setTheme(R.style.Theme_QuranReader)
        delegate.localNightMode=if(ReadingPreferences(this).darkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window,false)
        setContentView(R.layout.activity_main)
        val root=findViewById<View>(R.id.app_root)
        val direction=InterfaceLocales.direction(InterfaceLocales.effective(this))
        window.decorView.layoutDirection=direction
        root.layoutDirection=direction
        root.textDirection=if(direction==View.LAYOUT_DIRECTION_RTL) View.TEXT_DIRECTION_RTL else View.TEXT_DIRECTION_LTR
        findViewById<android.widget.ImageButton>(R.id.previous_page).setImageResource(if(direction==View.LAYOUT_DIRECTION_RTL) R.drawable.ic_chevron_right else R.drawable.ic_chevron_left)
        findViewById<android.widget.ImageButton>(R.id.next_page).setImageResource(if(direction==View.LAYOUT_DIRECTION_RTL) R.drawable.ic_chevron_left else R.drawable.ic_chevron_right)
        ViewCompat.setAccessibilityHeading(findViewById(R.id.screen_title),true)
        mapOf(R.id.home_panel to R.string.home,R.id.quran_panel to R.string.surah_index,
            R.id.search_panel to R.string.quran_search_title,R.id.bookmarks_panel to R.string.bookmarks,
            R.id.settings_panel to R.string.settings,R.id.history_panel to R.string.reading_history,
            R.id.about_panel to R.string.about_heading,R.id.support_panel to R.string.support_heading).forEach { (id,title) ->
            ViewCompat.setAccessibilityPaneTitle(findViewById(id),getString(title))
        }
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
            updatePageAssetStatus()
            if (displayedPage!=null && displayedPage!=number) {
                pageView.animate().alpha(1f).setDuration(110).start()
            }
            if(displayedPage==null || displayedPage==number) pageView.alpha=1f
            displayedPage=number
            applyNavigationSelection(viewModel.state.value)
        },{
            findViewById<View>(R.id.loading).isVisible=false
            updatePageAssetStatus()
        })
        pageView.onPageSwipe={ step -> viewModel.openPage(viewModel.state.value.pageNumber+step) }
        pageView.onReaderTap={ toggleReaderChrome() }
        pageView.onAyahPress={ id ->
            if(readerChromeVisible) toggleReaderChrome()
            pageView.page?.geometry?.pageNumber?.let { viewModel.recordPageDisplayed(it,id) }
        }
        pageView.onAyahLongPress={ id ->
            pageView.page?.geometry?.pageNumber?.let { viewModel.recordPageDisplayed(it,id) }
            val page=checkNotNull(pageView.page).geometry
            pageView.holdSelection()
            ayahLoad?.cancel()
            val surah=viewModel.state.value.surahs.first { it.surahNumber==id.surahNumber }
            ayahDialog.begin(id,surah.name,page.editionId,page.pageNumber)
            ayahLoad=lifecycleScope.launch {
                try {
                    val details=viewModel.getAyahDetails(id)
                    if (pageView.selectedAyah==id && pageView.page?.geometry===page) ayahDialog.bind(details)
                } catch (error: Exception) {
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    ayahDialog.dismiss()
                    android.widget.Toast.makeText(applicationContext,R.string.load_error,android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            findViewById<TextView>(R.id.selected_ayah).apply {
                text=getString(R.string.selected_ayah,id.surahNumber,id.ayahNumber);isVisible=false
            }
        }
        surahAdapter=SurahAdapter(this) { viewModel.firstPageForSurah(it) }
        findViewById<ListView>(R.id.surah_list).apply {
            adapter=surahAdapter
            setOnItemClickListener { _,_,position,_ -> viewModel.openSurah(surahAdapter.getItem(position).surahNumber) }
        }
        findViewById<EditText>(R.id.surah_search).doAfterTextChanged {
            surahAdapter.filter(it?.toString().orEmpty())
            findViewById<View>(R.id.surah_search_empty).isVisible=surahAdapter.count==0 && !viewModel.state.value.loading
        }
        searchAdapter=SearchResultAdapter(this)
        findViewById<ListView>(R.id.search_results).apply {
            adapter=searchAdapter
            setOnItemClickListener { _,_,position,_ -> viewModel.openAyah(searchAdapter.getItem(position).id) }
        }
        findViewById<EditText>(R.id.quran_search_query).apply {
            setText(viewModel.state.value.searchQuery)
            doAfterTextChanged { viewModel.search(it?.toString().orEmpty()) }
            setOnEditorActionListener { _,action,_ ->
                if(action==EditorInfo.IME_ACTION_SEARCH) {
                    WindowCompat.getInsetsController(window,this).hide(WindowInsetsCompat.Type.ime());true
                } else false
            }
        }
        findViewById<View>(R.id.search_more).setOnClickListener { viewModel.loadMoreSearchResults() }
        bookmarkAdapter=BookmarkAdapter(this) { viewModel.removeBookmark(it.editionId,it.pageNumber) }
        findViewById<ListView>(R.id.bookmarks_list).apply {
            adapter=bookmarkAdapter
            setOnItemClickListener { _,_,position,_ -> viewModel.openBookmark(bookmarkAdapter.getItem(position)) }
        }
        historyAdapter=ReadingHistoryAdapter(this)
        findViewById<ListView>(R.id.history_list).apply {
            adapter=historyAdapter
            setOnItemClickListener { _,_,position,_ -> viewModel.openHistory(historyAdapter.getItem(position)) }
        }
        backCallback=object:OnBackPressedCallback(false) {
            override fun handleOnBackPressed() { if(!cacheClearBusy) viewModel.navigateBack() }
        }
        onBackPressedDispatcher.addCallback(this,backCallback)
        ayahDialog=AyahOptionsDialog(this) {
            ayahLoad?.cancel()
            pageView.clearAyahSelection()
            findViewById<View>(R.id.selected_ayah).isVisible=false
        }
        ayahDialog.isPageBookmarked={ page -> viewModel.state.value.bookmarks.any { it.pageNumber==page } }
        ayahDialog.onBookmarkToggle={ id,page -> viewModel.toggleBookmark(page,id) }
        findViewById<View>(R.id.open_bookmarks).setOnClickListener { viewModel.navigateTo(Screen.BOOKMARKS) }
        findViewById<View>(R.id.open_surahs).setOnClickListener { viewModel.navigateTo(Screen.QURAN) }
        findViewById<View>(R.id.choose_language).setOnClickListener { showLanguageChoice() }
        refreshLanguageChoice()
        findViewById<View>(R.id.open_history).setOnClickListener { viewModel.navigateTo(Screen.HISTORY) }
        findViewById<View>(R.id.open_about).setOnClickListener { viewModel.navigateTo(Screen.ABOUT) }
        findViewById<View>(R.id.open_support).setOnClickListener { viewModel.navigateTo(Screen.SUPPORT) }
        findViewById<View>(R.id.open_search).setOnClickListener { viewModel.navigateTo(Screen.SEARCH) }
        findViewById<View>(R.id.toggle_bookmark).setOnClickListener {
            val page=pageView.page?.geometry?.pageNumber
            val state=viewModel.state.value
            if(reader.displaying && !state.loading && !state.error && page==state.pageNumber)
                viewModel.toggleBookmark(checkNotNull(page),pageView.selectedAyah)
        }
        findViewById<SwitchCompat>(R.id.keep_screen_awake).setOnCheckedChangeListener { _,enabled -> viewModel.setKeepScreenAwake(enabled) }
        findViewById<SwitchCompat>(R.id.auto_resume_reading).setOnCheckedChangeListener { _,enabled -> viewModel.setAutoResumeReading(enabled) }
        findViewById<SwitchCompat>(R.id.show_reader_bars).setOnCheckedChangeListener { _,enabled -> viewModel.setShowReaderBarsOnOpen(enabled) }
        findViewById<View>(R.id.cache_clear).setOnClickListener { confirmClearCache() }
        findViewById<View>(R.id.nav_quran).setOnClickListener { viewModel.navigateTo(Screen.QURAN) }
        findViewById<View>(R.id.nav_settings).setOnClickListener { viewModel.navigateTo(Screen.SETTINGS) }
        findViewById<Button>(R.id.continue_reading).setOnClickListener { viewModel.resumeReading() }
        findViewById<Button>(R.id.read_pages).setOnClickListener { viewModel.openPage(1) }
        findViewById<SwitchCompat>(R.id.dark_mode).setOnCheckedChangeListener { _,enabled ->
            if (enabled!=viewModel.state.value.darkMode) {
                viewModel.setDarkMode(enabled)
                delegate.localNightMode=if(enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            }
        }
        findViewById<Button>(R.id.retry_load).setOnClickListener { viewModel.retry() }
        findViewById<Button>(R.id.page_asset_retry).setOnClickListener {
            if(reader.failed) {
                reader.retry(viewModel.state.value.pageNumber)
                updatePageAssetStatus()
            }
        }
        findViewById<View>(R.id.previous_page).setOnClickListener { viewModel.openPage(viewModel.state.value.pageNumber-1) }
        findViewById<View>(R.id.next_page).setOnClickListener { viewModel.openPage(viewModel.state.value.pageNumber+1) }
        findViewById<Button>(R.id.go_page).setOnClickListener { showPageJump() }
        findViewById<TextView>(R.id.app_version).text=getString(R.string.app_version,com.ahmad.quran.BuildConfig.VERSION_NAME)
        findViewById<Button>(R.id.show_license).setOnClickListener {
            findViewById<TextView>(R.id.license_notice).apply { isVisible=!isVisible }
        }
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.state.collect(::render) } }
    }
    private fun render(state: QuranUiState) {
        val reading=state.screen==Screen.READER
        val enteringReader=reading && previousScreen!=Screen.READER
        val enteringSettings=state.screen==Screen.SETTINGS && previousScreen!=Screen.SETTINGS
        if(!state.loading) launchComplete=true
        findViewById<View>(R.id.launch_panel).isVisible=!launchComplete
        findViewById<View>(R.id.app_root).isVisible=launchComplete
        if(previousScreen!=state.screen) {
            currentFocus?.clearFocus()
            WindowCompat.getInsetsController(window,findViewById(R.id.app_root)).hide(WindowInsetsCompat.Type.ime())
        }
        if (ayahDialog.isOpen && (!reading || ayahDialog.pageNumber!=state.pageNumber)) ayahDialog.dismiss()
        if (!reading && (!readerChromeVisible || chromeFraction!=1f)) restoreReaderChrome()
        if (!reading) pageJumpDialog?.dismiss()
        findViewById<TextView>(R.id.screen_title).apply {
            text=when(state.screen) {
                Screen.READER -> state.surahs.firstOrNull { it.surahNumber==state.anchor.surahNumber }?.let { getString(R.string.ayah_surah,InterfaceLocales.displayName(this@MainActivity,it.name)) }
                    ?: getString(R.string.app_name)
                Screen.SETTINGS -> getString(R.string.settings)
                Screen.HOME -> getString(R.string.app_name)
                Screen.QURAN -> getString(R.string.surah_index)
                Screen.BOOKMARKS -> getString(R.string.bookmarks)
                Screen.SEARCH -> getString(R.string.quran_search_title)
                Screen.HISTORY -> getString(R.string.reading_history)
                Screen.ABOUT -> getString(R.string.about_heading)
                Screen.SUPPORT -> getString(R.string.support_heading)
            }
        }
        surahAdapter.submit(state.surahs,state.revelationTypes)
        findViewById<View>(R.id.surah_search_empty).isVisible=surahAdapter.count==0 && !state.loading
        bookmarkAdapter.submit(state.bookmarks,state.surahs)
        historyAdapter.submit(state.history,state.surahs)
        findViewById<View>(R.id.history_empty).isVisible=state.history.isEmpty()
        findViewById<View>(R.id.history_list).isVisible=state.history.isNotEmpty()
        findViewById<View>(R.id.bookmarks_list).isVisible=state.bookmarks.isNotEmpty()
        searchAdapter.submit(state.searchResults,state.surahs)
        findViewById<View>(R.id.search_progress).isVisible=state.searchLoading
        findViewById<TextView>(R.id.search_status).text=when {
            state.searchLoading -> getString(R.string.quran_search_loading)
            state.searchError -> getString(R.string.quran_search_error)
            state.searchQuery.isBlank() -> getString(R.string.quran_search_initial)
            state.searchResults.isEmpty() -> getString(R.string.quran_search_empty)
            else -> getString(R.string.quran_search_count,state.searchResults.size)
        }
        findViewById<Button>(R.id.search_more).apply {
            isVisible=state.searchHasMore || state.searchError
            isEnabled=!state.searchLoading
            setText(if(state.searchError)R.string.retry else R.string.quran_search_more)
        }
        findViewById<View>(R.id.bookmarks_empty).isVisible=state.bookmarks.isEmpty()
        val bookmarked=state.bookmarks.any { it.pageNumber==state.pageNumber }
        findViewById<android.widget.ImageButton>(R.id.toggle_bookmark).apply {
            setImageResource(if(bookmarked)R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark)
            contentDescription=getString(if(bookmarked)R.string.bookmark_remove_action else R.string.bookmark_add_action)
            isEnabled=reading && !state.loading && !state.error && reader.displaying && pageView.page?.geometry?.pageNumber==state.pageNumber
        }
        ayahDialog.refreshBookmarkAction()
        mapOf(Screen.HOME to R.id.home_panel,Screen.SEARCH to R.id.search_panel,Screen.QURAN to R.id.quran_panel,Screen.READER to R.id.reader_panel,Screen.SETTINGS to R.id.settings_panel,Screen.BOOKMARKS to R.id.bookmarks_panel,Screen.HISTORY to R.id.history_panel,Screen.ABOUT to R.id.about_panel,Screen.SUPPORT to R.id.support_panel).forEach { (screen,id) ->
            findViewById<View>(id).isVisible=state.screen==screen
        }
        mapOf(Screen.QURAN to R.id.nav_quran,Screen.SETTINGS to R.id.nav_settings).forEach { (screen,id) ->
            findViewById<View>(id).isEnabled=state.screen!=screen && !cacheClearBusy
        }
        if (reading && !state.loading && !state.error) {
            if (pageView.page?.geometry?.pageNumber!=state.pageNumber) findViewById<View>(R.id.selected_ayah).isVisible=false
            if (displayedPage!=null && pageView.page?.geometry?.pageNumber!=state.pageNumber) {
                pageView.animate().cancel();pageView.alpha=0.82f
            }
            reader.show(state.pageNumber)
            // A bundled page can finish drawing before canonical metadata completes.
            if(reader.displaying && pageView.page?.geometry?.pageNumber==state.pageNumber &&
                (state.lastLocation?.pageNumber!=state.pageNumber || state.lastLocation?.editionId!=state.edition?.editionId))
                viewModel.recordPageDisplayed(state.pageNumber)
            applyNavigationSelection(state)
        } else if (!reading) reader.leave()
        findViewById<View>(R.id.loading).isVisible=state.loading
        updatePageAssetStatus()
        findViewById<View>(R.id.load_error).isVisible=state.error
        findViewById<View>(R.id.retry_load).isVisible=state.error
        findViewById<TextView>(R.id.reader_location).text=when(state.screen) {
            Screen.READER -> state.juzNumber?.let { getString(R.string.ayah_juz,it) }
                ?: getString(R.string.mushaf_edition_label)
            Screen.SETTINGS -> getString(R.string.settings_subtitle)
            Screen.HOME, Screen.QURAN -> getString(R.string.mushaf_edition_label)
            Screen.BOOKMARKS -> getString(R.string.bookmarks_subtitle)
            Screen.SEARCH -> getString(R.string.quran_search_help)
            Screen.HISTORY -> getString(R.string.history_subtitle)
            Screen.ABOUT -> getString(R.string.about_subtitle)
            Screen.SUPPORT -> getString(R.string.support_subtitle)
        }
        findViewById<TextView>(R.id.page_indicator).apply {
            text=getString(R.string.page_fraction,state.pageNumber,604)
            contentDescription=getString(R.string.page_location,state.pageNumber,604)
        }
        findViewById<Button>(R.id.continue_reading).apply { isEnabled=state.lastLocation!=null && !state.loading }
        findViewById<Button>(R.id.read_pages).isEnabled=!state.loading && state.edition!=null
        findViewById<TextView>(R.id.last_location).apply {
            text=state.lastLocation?.let { location ->
                val name=state.surahs.firstOrNull { it.surahNumber==location.surahNumber }?.name.orEmpty()
                if(location.editionId==state.edition?.editionId) getString(R.string.resume_details,InterfaceLocales.displayName(this@MainActivity,name),location.pageNumber)
                else getString(R.string.resume_surah,InterfaceLocales.displayName(this@MainActivity,name),location.ayahNumber)
            } ?: getString(R.string.last_read_empty)
        }
        findViewById<SwitchCompat>(R.id.dark_mode).isChecked=state.darkMode
        findViewById<SwitchCompat>(R.id.keep_screen_awake).isChecked=state.keepScreenAwake
        findViewById<SwitchCompat>(R.id.auto_resume_reading).isChecked=state.autoResumeReading
        findViewById<SwitchCompat>(R.id.show_reader_bars).isChecked=state.showReaderBarsOnOpen
        if(reading && state.keepScreenAwake) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        syncSystemBars()
        findViewById<View>(R.id.previous_page).isEnabled=!state.loading && state.pageNumber>1
        findViewById<View>(R.id.next_page).isEnabled=!state.loading && state.pageNumber<604
        findViewById<Button>(R.id.go_page).isEnabled=!state.loading
        findViewById<TextView>(R.id.license_notice).text=state.notice
        backCallback.isEnabled=state.screen!=Screen.HOME
        previousScreen=state.screen
        if(enteringSettings) refreshCacheSize()
        if(enteringReader && !state.showReaderBarsOnOpen) findViewById<View>(R.id.bottom_bar).doOnLayout {
            if(viewModel.state.value.screen==Screen.READER && readerChromeVisible) toggleReaderChrome()
        }
    }
    override fun onPause() { if (::pageView.isInitialized) pageView.cancelPendingInteraction();if (::reader.isInitialized) reader.saveMeasurements();super.onPause() }
    private fun applyNavigationSelection(state: QuranUiState) {
        val id=state.targetAyah ?: return
        if(state.screen==Screen.READER && !state.loading && !state.error && reader.displaying &&
            pageView.page?.geometry?.pageNumber==state.pageNumber) {
            pageView.setZoom(1f)
            if(pageView.selectAyah(id)) {
                viewModel.recordPageDisplayed(state.pageNumber,id)
                viewModel.consumeTarget(id)
            }
        }
    }
    private fun updatePageAssetStatus() {
        val reading=viewModel.state.value.screen==Screen.READER
        findViewById<View>(R.id.page_asset_status).isVisible=reading && !viewModel.state.value.error && !reader.displaying
        findViewById<View>(R.id.page_asset_progress).isVisible=!reader.failed
        findViewById<Button>(R.id.page_asset_retry).apply { isVisible=reader.failed;isEnabled=reader.failed }
        findViewById<TextView>(R.id.page_asset_message).setText(when {
            !reader.failed -> R.string.page_asset_loading
            !pageSourceConfigured -> R.string.page_asset_unavailable
            else -> R.string.page_asset_error
        })
        val state=viewModel.state.value
        findViewById<View>(R.id.toggle_bookmark).isEnabled=reading && !state.loading && !state.error && reader.displaying && pageView.page?.geometry?.pageNumber==state.pageNumber
    }
    @Suppress("DEPRECATION")
    private fun syncSystemBars() {
        val dark=ReadingPreferences(this).darkMode
        window.statusBarColor=ContextCompat.getColor(this,R.color.surface)
        window.navigationBarColor=ContextCompat.getColor(this,if(Build.VERSION.SDK_INT>=26)R.color.surface else R.color.primary_dark)
        WindowCompat.getInsetsController(window,findViewById(R.id.app_root)).apply {
            isAppearanceLightStatusBars=!dark
            isAppearanceLightNavigationBars=Build.VERSION.SDK_INT>=26 && !dark
        }
        if(Build.VERSION.SDK_INT>=29) {
            window.isStatusBarContrastEnforced=false
            window.isNavigationBarContrastEnforced=false
        }
    }
    override fun onResume() { super.onResume();syncSystemBars() }
    override fun onWindowFocusChanged(hasFocus: Boolean) { super.onWindowFocusChanged(hasFocus);if(hasFocus)syncSystemBars() }
    private fun refreshCacheSize() {
        cacheRefresh?.cancel()
        cacheRefresh=lifecycleScope.launch {
            val bytes=withContext(Dispatchers.IO) { DownloadedPageCache(applicationContext).sizeBytes() }
            findViewById<TextView>(R.id.cache_size).text=getString(R.string.cache_size,Formatter.formatShortFileSize(this@MainActivity,bytes))
            findViewById<TextView>(R.id.cache_help).setText(if(bytes==0L)R.string.cache_empty else R.string.cache_help)
            findViewById<View>(R.id.cache_clear).isEnabled=bytes>0L && !cacheClearBusy
        }
    }
    private fun confirmClearCache() {
        val dialog=AlertDialog.Builder(this).setTitle(R.string.cache_clear).setMessage(R.string.cache_confirm)
            .setNegativeButton(R.string.cancel,null).setPositiveButton(R.string.cache_clear) { _,_ ->
                cacheClearBusy=true;render(viewModel.state.value)
                findViewById<View>(R.id.cache_clear).isEnabled=false
                findViewById<TextView>(R.id.cache_feedback).apply { isVisible=true;setText(R.string.cache_clearing) }
                reader.clearDownloadedPages { success ->
                    cacheClearBusy=false;render(viewModel.state.value)
                    findViewById<View>(R.id.cache_clear).isEnabled=true
                    findViewById<TextView>(R.id.cache_feedback).setText(if(success)R.string.cache_cleared else R.string.cache_clear_failed)
                    refreshCacheSize()
                }
            }.create()
        dialog.show();dialog.window?.decorView?.layoutDirection=InterfaceLocales.direction(InterfaceLocales.effective(this));dialog.window?.setBackgroundDrawableResource(R.drawable.ayah_dialog_background)
    }
    private fun refreshLanguageChoice() {
        val choice=InterfaceLocales.choice(this)
        findViewById<TextView>(R.id.language_value).text=if(choice==InterfaceLocales.SYSTEM) getString(R.string.language_system)
            else resources.getStringArray(R.array.language_names)[InterfaceLocales.tags.indexOf(choice)]
    }
    private fun showLanguageChoice() {
        val choices=listOf(InterfaceLocales.SYSTEM)+InterfaceLocales.tags
        val labels=arrayOf(getString(R.string.language_system))+resources.getStringArray(R.array.language_names)
        val dialog=AlertDialog.Builder(this).setTitle(R.string.language)
            .setSingleChoiceItems(labels,choices.indexOf(InterfaceLocales.choice(this))) { window,index ->
                window.dismiss()
                InterfaceLocales.select(this,choices[index])
                refreshLanguageChoice()
            }.setNegativeButton(R.string.cancel,null).create()
        dialog.show()
        dialog.window?.decorView?.layoutDirection=InterfaceLocales.direction(InterfaceLocales.effective(this))
        dialog.window?.setBackgroundDrawableResource(R.drawable.ayah_dialog_background)
    }
    private fun readerChrome(): List<View> = listOf(R.id.top_bar,R.id.bottom_bar).map { findViewById(it) }

    private fun toggleReaderChrome() {
        if (viewModel.state.value.screen!=Screen.READER) return
        val bars=readerChrome()
        if (topHeight==0) topHeight=bars[0].height
        if (bottomHeight==0) bottomHeight=bars[1].height
        readerChromeVisible=!readerChromeVisible
        chromeAnimation?.cancel()
        bars.forEach { it.visibility=View.VISIBLE }
        chromeAnimation=ValueAnimator.ofFloat(chromeFraction,if(readerChromeVisible)1f else 0f).apply {
            duration=190
            addUpdateListener { animation ->
                chromeFraction=animation.animatedValue as Float
                bars.forEachIndexed { index,bar ->
                    bar.alpha=chromeFraction
                    val height=((if(index==0)topHeight else bottomHeight)*chromeFraction).toInt()
                    if(bar.layoutParams.height!=height) bar.layoutParams=bar.layoutParams.apply { this.height=height }
                    bar.visibility=if(chromeFraction==0f && !readerChromeVisible) View.GONE else View.VISIBLE
                }
            }
            start()
        }
    }

    private fun restoreReaderChrome() {
        chromeAnimation?.cancel()
        chromeFraction=1f;readerChromeVisible=true
        topHeight=0;bottomHeight=0
        readerChrome().forEach { bar ->
            bar.alpha=1f;bar.isVisible=true
            bar.layoutParams=bar.layoutParams.apply { height=android.view.ViewGroup.LayoutParams.WRAP_CONTENT }
        }
    }

    private fun showPageJump() {
        pageJumpDialog?.dismiss()
        val dialog=AppCompatDialog(this)
        pageJumpDialog=dialog
        dialog.supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.page_jump_dialog)
        dialog.setCanceledOnTouchOutside(true)
        val input=checkNotNull(dialog.findViewById<EditText>(R.id.page_number))
        input.setText(getString(R.string.page_value,viewModel.state.value.pageNumber))
        val modes=checkNotNull(dialog.findViewById<RadioGroup>(R.id.jump_mode))
        val selection=checkNotNull(dialog.findViewById<Spinner>(R.id.jump_selection))
        val hint=checkNotNull(dialog.findViewById<TextView>(R.id.jump_hint))
        val ayahInput=checkNotNull(dialog.findViewById<EditText>(R.id.jump_ayah_number))
        val ayahLabel=checkNotNull(dialog.findViewById<TextView>(R.id.jump_ayah_label))
        input.doAfterTextChanged { input.error=null }
        ayahInput.doAfterTextChanged { ayahInput.error=null }
        selection.onItemSelectedListener=object:AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(modes.checkedRadioButtonId==R.id.jump_mode_surah) {
                    val count=viewModel.state.value.surahs[position].ayahCount
                    ayahInput.hint=getString(R.string.jump_ayah_hint,count)
                    ayahInput.contentDescription=getString(R.string.jump_ayah_hint,count)
                    ayahLabel.text=getString(R.string.jump_ayah_hint,count)
                    ayahInput.error=null
                }
            }
        }
        modes.setOnCheckedChangeListener { _,checked ->
            val pageMode=checked==R.id.jump_mode_page
            input.isVisible=pageMode;selection.isVisible=!pageMode
            ayahInput.isVisible=checked==R.id.jump_mode_surah
            ayahLabel.isVisible=checked==R.id.jump_mode_surah
            selection.contentDescription=getString(if(checked==R.id.jump_mode_juz)R.string.jump_juz_hint else R.string.jump_surah_hint)
            hint.setText(when(checked) { R.id.jump_mode_juz -> R.string.jump_juz_hint;R.id.jump_mode_surah -> R.string.jump_surah_hint;else -> R.string.page_jump_hint })
            if(!pageMode) {
                WindowCompat.getInsetsController(checkNotNull(dialog.window),input).hide(WindowInsetsCompat.Type.ime())
                val labels=if(checked==R.id.jump_mode_juz) (1..30).map { getString(R.string.ayah_juz,it) }
                    else viewModel.state.value.surahs.map { getString(R.string.surah_choice,it.surahNumber,InterfaceLocales.displayName(this@MainActivity,it.name)) }
                selection.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,labels)
                selection.setSelection(if(checked==R.id.jump_mode_juz)(viewModel.state.value.juzNumber ?: 1)-1 else viewModel.state.value.anchor.surahNumber-1)
            } else input.requestFocus()
        }
        fun confirm() {
            if(modes.checkedRadioButtonId!=R.id.jump_mode_page) {
                if(modes.checkedRadioButtonId==R.id.jump_mode_juz) {
                    dialog.dismiss();viewModel.openJuz(selection.selectedItemPosition+1)
                } else {
                    val surah=viewModel.state.value.surahs[selection.selectedItemPosition]
                    val number=ayahInput.text.toString().toIntOrNull()
                    if(number==null || number !in 1..surah.ayahCount) ayahInput.error=getString(R.string.jump_ayah_invalid,surah.ayahCount)
                    else {
                        WindowCompat.getInsetsController(checkNotNull(dialog.window),ayahInput).hide(WindowInsetsCompat.Type.ime())
                        dialog.dismiss();viewModel.openAyah(com.ahmad.quran.data.AyahId(surah.surahNumber,number))
                    }
                }
                return
            }
            val number=input.text.toString().toIntOrNull()
            if(number==null || number !in 1..604) input.error=getString(R.string.page_invalid,604)
            else {
                WindowCompat.getInsetsController(checkNotNull(dialog.window),input).hide(WindowInsetsCompat.Type.ime())
                dialog.dismiss()
                viewModel.openPage(number)
            }
        }
        dialog.findViewById<Button>(R.id.jump_confirm)!!.setOnClickListener { confirm() }
        dialog.findViewById<Button>(R.id.jump_cancel)!!.setOnClickListener { dialog.dismiss() }
        input.setOnEditorActionListener { _,action,_ -> if(action==EditorInfo.IME_ACTION_GO) { confirm();true } else false }
        ayahInput.setOnEditorActionListener { _,action,_ -> if(action==EditorInfo.IME_ACTION_GO) { confirm();true } else false }
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setGravity(Gravity.CENTER)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.22f)
            attributes=attributes.apply { windowAnimations=R.style.AyahDialogAnimation }
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
        dialog.show()
        dialog.window?.decorView?.layoutDirection=InterfaceLocales.direction(InterfaceLocales.effective(this))
        val width=minOf((480*resources.displayMetrics.density).toInt(),(findViewById<View>(R.id.app_root).width*.90f).toInt())
        dialog.window?.setLayout(width,android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        input.requestFocus();input.selectAll()
    }
    override fun onDestroy() {
        ayahLoad?.cancel()
        cacheRefresh?.cancel()
        chromeAnimation?.cancel()
        if (::pageView.isInitialized) pageView.animate().cancel()
        pageJumpDialog?.dismiss()
        if (::ayahDialog.isInitialized) ayahDialog.dismiss()
        if (::reader.isInitialized) reader.close()
        super.onDestroy()
    }
}
