package com.ahmad.quran.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDialog
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import com.ahmad.quran.R
import com.ahmad.quran.data.AyahId
import kotlin.math.min

/** One centered modal window, with bounded, independently scrolling canonical text. */
class AyahOptionsDialog(private val activity: AppCompatActivity, private val onClosed: () -> Unit) {
    val dialog = AppCompatDialog(activity)
    private val root: View
    var ayahId: AyahId? = null
        private set
    var editionId: String? = null
        private set
    var pageNumber: Int? = null
        private set
    private var text: String? = null
    val isOpen: Boolean get() = ayahId != null
    var onBookmarkToggle: ((AyahId, Int) -> Unit)? = null
    var isPageBookmarked: (Int) -> Boolean = { false }
    fun refreshBookmarkAction() {
        view<Button>(R.id.ayah_dialog_bookmark).setText(if(pageNumber?.let(isPageBookmarked)==true) R.string.bookmark_remove_action else R.string.bookmark_add_action)
    }
    private val back = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = dismiss()
    }

    init {
        dialog.supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.ayah_options_dialog)
        root = checkNotNull(dialog.findViewById(R.id.ayah_dialog))
        root.layoutDirection=InterfaceLocales.direction(InterfaceLocales.effective(activity))
        dialog.setCanceledOnTouchOutside(true)
        dialog.setOnDismissListener { if (!dialog.isShowing) finishDismiss() }
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setGravity(Gravity.CENTER)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.22f)
            attributes = attributes.apply { windowAnimations = R.style.AyahDialogAnimation }
        }
        activity.onBackPressedDispatcher.addCallback(activity, back)
        ViewCompat.setAccessibilityPaneTitle(root, activity.getString(R.string.ayah_options_title))
        ViewCompat.setAccessibilityHeading(view(R.id.ayah_dialog_surah), true)
        view<Button>(R.id.ayah_dialog_bookmark).setOnClickListener {
            val id=ayahId;val page=pageNumber
            if(id!=null && page!=null) { onBookmarkToggle?.invoke(id,page);refreshBookmarkAction() }
        }
        view<Button>(R.id.ayah_dialog_close).setOnClickListener { dismiss() }
        view<Button>(R.id.ayah_dialog_copy).setOnClickListener {
            text?.let {
                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText(activity.getString(R.string.ayah_options_title), it))
                if (android.os.Build.VERSION.SDK_INT < 33) Toast.makeText(activity.applicationContext, activity.getString(R.string.ayah_copied), Toast.LENGTH_SHORT).show()
            }
        }
        view<Button>(R.id.ayah_dialog_share).setOnClickListener {
            text?.let {
                activity.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, it)
                }, activity.getString(R.string.ayah_share)))
            }
        }
    }

    private fun <T : View> view(id: Int): T = root.findViewById(id)

    fun begin(id: AyahId, surahName: String, edition: String, page: Int) {
        ayahId = id; editionId = edition; pageNumber = page; text = null
        back.isEnabled = true
        view<TextView>(R.id.ayah_dialog_surah).text = activity.getString(R.string.ayah_surah, InterfaceLocales.displayName(activity, surahName))
        view<TextView>(R.id.ayah_dialog_number).text = activity.getString(R.string.ayah_number, id.ayahNumber)
        view<TextView>(R.id.ayah_dialog_page).text = activity.getString(R.string.ayah_page, page)
        view<View>(R.id.ayah_dialog_juz).isVisible = false
        view<TextView>(R.id.ayah_dialog_text).text = ""
        view<View>(R.id.ayah_dialog_loading).isVisible = true
        actionsEnabled(false)
        refreshBookmarkAction()
        view<NestedScrollView>(R.id.ayah_dialog_scroll).scrollTo(0, 0)
        if (!dialog.isShowing) dialog.show()
        resize()
    }

    fun bind(details: AyahDetails) {
        if (details.ayah.id != ayahId) return
        text = details.ayah.text
        view<TextView>(R.id.ayah_dialog_text).text = details.ayah.text
        view<TextView>(R.id.ayah_dialog_juz).apply {
            isVisible = details.juzNumber != null
            text = details.juzNumber?.let { activity.getString(R.string.ayah_juz, it) }.orEmpty()
        }
        view<View>(R.id.ayah_dialog_loading).isVisible = false
        actionsEnabled(true)
        resize()
    }

    private fun resize() {
        val available = activity.findViewById<View>(R.id.app_root)
        if (available.width <= 0 || available.height <= 0) return
        val width = min((480 * activity.resources.displayMetrics.density).toInt(), (available.width * 0.90f).toInt())
        val maxHeight = ((available.height - available.paddingTop - available.paddingBottom) * 0.80f).toInt()
        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST))
        dialog.window?.setLayout(width, root.measuredHeight)
    }

    private fun actionsEnabled(enabled: Boolean) {
        view<Button>(R.id.ayah_dialog_bookmark).isEnabled = enabled && onBookmarkToggle != null
        view<Button>(R.id.ayah_dialog_copy).isEnabled = enabled
        view<Button>(R.id.ayah_dialog_share).isEnabled = enabled
    }

    fun dismiss() {
        finishDismiss()
        dialog.dismiss()
    }

    private fun finishDismiss() {
        if (!isOpen) return
        ayahId = null; editionId = null; pageNumber = null; text = null
        back.isEnabled = false
        actionsEnabled(false)
        onClosed()
    }
}
