package com.ahmad.quran.mushaf.svg

import android.annotation.SuppressLint
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.os.Handler
import android.os.Looper
import android.view.ViewConfiguration
import android.view.MotionEvent
import android.view.View
import com.ahmad.quran.data.AyahId
import com.ahmad.quran.mushaf.svg.PagePoint
import kotlin.math.max
import kotlin.math.min

/** Direct vector rendering and edition touch geometry share exactly one display matrix. */
class NativeSvgPageView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    init {
        isClickable = true
        isFocusable = true
        // API 23 hardware path caching blurred the source's tiny transformed glyph paths.
        // Software redraw keeps vector zoom sharp; the OS owns one viewport layer, not page rasters.
        if (android.os.Build.VERSION.SDK_INT < 26) setLayerType(LAYER_TYPE_SOFTWARE, null)
    }
    var page: NativeSvgPage? = null
        private set
    private val matrix = Matrix()
    private val inverse = Matrix()
    private val highlight = Paint(Paint.ANTI_ALIAS_FLAG)
    private var ink: AyahHighlight? = null
    private var visualPath: Path? = null
    private var previousPath: Path? = null
    private var visualAlpha = 0f
    private var previousAlpha = 0f
    private var fade: ValueAnimator? = null
    private var zoom = 1f
    private var focus = PagePoint(0f, 0f)
    var selectedAyah: AyahId? = null
        private set
    private var selectionHeld = false
    var onAyahLongPress: ((AyahId) -> Unit)? = null
    var onAyahPress: ((AyahId) -> Unit)? = null
    var onReaderTap: (() -> Unit)? = null
    var onPageSwipe: ((Int) -> Unit)? = null
    /** One-shot completion of the next actual View draw, for the standalone gate. */
    var onNextRender: (() -> Unit)? = null
    var showAllRegions = false
    var lastRenderNanos = 0L
        private set
    var lastCanvasHardware = false
        private set
    var lastHitTestNanos = 0L
        private set
    var lastLongPressDispatchNanos = 0L
        private set
    private val clearSelection = Runnable { selectedAyah = null; ink = null; transitionTo(null) }
    companion object {
        const val AYAH_SELECT_HOLD_MS = 500L
        const val AYAH_OPTIONS_HOLD_MS = 900L
        private const val PAGE_SWIPE_DISTANCE_DP = 60f
    }
    private val touchHandler = Handler(Looper.getMainLooper())
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private var touchActive = false
    private var touchMoved = false
    private var pressSelected = false
    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L
    private var touchAyah: AyahId? = null
    private var touchPage: NativeSvgPage? = null
    private val selectHold = Runnable {
        val id = touchAyah
        if (touchActive && !touchMoved && id != null && page === touchPage) {
            holdSelection()
            select(id)
            pressSelected = true
            onAyahPress?.invoke(id)
        }
    }
    private val optionsHold = Runnable {
        val id = touchAyah
        if (touchActive && !touchMoved && pressSelected && id != null && page === touchPage) {
            val start = System.nanoTime()
            onAyahLongPress?.invoke(id)
            lastLongPressDispatchNanos = System.nanoTime() - start
        }
    }
    private fun cancelHoldTimers() {
        touchHandler.removeCallbacks(selectHold)
        touchHandler.removeCallbacks(optionsHold)
    }

    fun display(page: NativeSvgPage) {
        cancelPendingInteraction()
        removeCallbacks(clearSelection)
        this.page = page
        selectedAyah = null
        selectionHeld = false
        resetHighlight()
        zoom = 1f
        updateMatrix()
        invalidate()
    }

    fun release() {
        cancelPendingInteraction()
        removeCallbacks(clearSelection)
        onNextRender = null
        page = null
        selectedAyah = null
        selectionHeld = false
        resetHighlight()
        invalidate()
    }

    /** Cancel timers on page replacement, lifecycle changes or detached views. */
    fun cancelPendingInteraction() {
        cancelHoldTimers()
        touchActive = false
        touchAyah = null
        touchPage = null
    }
    override fun onDetachedFromWindow() {
        cancelPendingInteraction()
        super.onDetachedFromWindow()
    }

    private fun select(id: AyahId) {
        val current = page ?: return
        removeCallbacks(clearSelection)
        if (selectedAyah == id) return
        selectedAyah = id
        ink = AyahHighlight.prepare(current.geometry, current.geometry.regions.first { it.ayahId == id })
        transitionTo(visualHighlight())
    }

    /** Navigation uses the approved selection renderer, without synthesizing a touch event. */
    fun selectAyah(id: AyahId): Boolean {
        if (page?.geometry?.regions?.none { it.ayahId == id } != false) return false
        holdSelection()
        select(id)
        return true
    }

    /** Keep the selection until another ayah, page or an explicit dismissal replaces it. */
    fun holdSelection() {
        selectionHeld = true
        removeCallbacks(clearSelection)
    }

    fun clearAyahSelection() {
        selectionHeld = false
        removeCallbacks(clearSelection)
        clearSelection.run()
    }

    /** Programmatic matrix validation only; production touch input never changes scale or translation. */
    fun setZoom(value: Float, x: Float = width/2f, y: Float = height/2f) {
        zoom = max(1f, min(4f, value))
        focus = PagePoint(x, y)
        updateMatrix()
        invalidate()
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) = updateMatrix()
    private fun updateMatrix() {
        val source = page?.geometry?.renderBox ?: return
        if (width <= 0 || height <= 0) return
        matrix.setRectToRect(source, RectF(0f, 0f, width.toFloat(), height.toFloat()), Matrix.ScaleToFit.CENTER)
        matrix.postScale(zoom, zoom, focus.x, focus.y)
        check(matrix.invert(inverse))
        if (ink != null) visualPath = visualHighlight()
    }
    private fun visualHighlight(): Path? {
        val values = FloatArray(9)
        matrix.getValues(values)
        return ink?.path(1f/values[Matrix.MSCALE_X], resources.displayMetrics.density)
    }
    private fun transitionTo(path: Path?) {
        fade?.cancel()
        previousPath = visualPath
        val startingAlpha = visualAlpha
        visualPath = path
        visualAlpha = 0f
        previousAlpha = startingAlpha
        fade = ValueAnimator.ofFloat(0f,1f).apply {
            duration = 140
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                visualAlpha = if (path == null) 0f else progress
                previousAlpha = startingAlpha*(1f-progress)
                if (progress == 1f) previousPath = null
                invalidate()
            }
            start()
        }
    }
    private fun resetHighlight() {
        fade?.cancel();fade = null
        ink = null;visualPath = null;previousPath = null
        visualAlpha = 0f;previousAlpha = 0f
    }
    fun screenPoint(point: PagePoint): PagePoint {
        val p = floatArrayOf(point.x, point.y)
        matrix.mapPoints(p)
        return PagePoint(p[0], p[1])
    }
    fun hitTest(x: Float, y: Float): AyahId? {
        val start = System.nanoTime()
        val p = floatArrayOf(x, y)
        inverse.mapPoints(p)
        val id = page?.geometry?.hitTest(p[0], p[1])
        lastHitTestNanos = System.nanoTime()-start
        return id
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val current = page ?: return
        lastCanvasHardware = canvas.isHardwareAccelerated
        val start = System.nanoTime()
        canvas.save()
        canvas.concat(matrix)
        // Pale green line strips first; original vector letters, marks and numbers stay on top.
        fun backdrop(path: Path?, opacity: Float) {
            if (path != null && opacity > 0f) {
                highlight.color = android.graphics.Color.argb((46*opacity).toInt(),123,183,150)
                canvas.drawPath(path,highlight)
            }
        }
        backdrop(previousPath,previousAlpha)
        backdrop(visualPath,visualAlpha)
        // Equal viewport/viewBox coordinates give an identity SVG root transform.
        current.artwork.renderToCanvas(canvas, current.geometry.viewBox)
        current.geometry.regions.forEachIndexed { index, region ->
            if (showAllRegions) {
                highlight.color = android.graphics.Color.argb(60, index*73%210, 115, index*47%220)
                canvas.drawPath(region.path(), highlight)
            }
        }
        canvas.restore()
        lastRenderNanos = System.nanoTime()-start
        val completed = onNextRender
        onNextRender = null
        completed?.invoke()
    }
    override fun performClick(): Boolean {
        super.performClick()
        onReaderTap?.invoke()
        return true
    }
    @SuppressLint("ClickableViewAccessibility") // Short touches use performClick; holds resolve source polygons.
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                cancelPendingInteraction()
                touchActive = true
                touchMoved = false
                pressSelected = false
                // Global coordinates stay stable while hiding chrome resizes/repositions this View.
                downX = event.rawX; downY = event.rawY; downTime = event.eventTime
                touchPage = page
                touchAyah = hitTest(event.x, event.y)
                if (touchAyah != null) {
                    touchHandler.postDelayed(selectHold, AYAH_SELECT_HOLD_MS)
                    touchHandler.postDelayed(optionsHold, AYAH_OPTIONS_HOLD_MS)
                }
            }
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_CANCEL -> cancelPendingInteraction()
            MotionEvent.ACTION_MOVE -> if (touchActive) {
                val dx = event.rawX - downX; val dy = event.rawY - downY
                if (event.pointerCount != 1) cancelPendingInteraction()
                else if (!touchMoved && dx * dx + dy * dy > touchSlop * touchSlop) {
                    touchMoved = true
                    cancelHoldTimers()
                    if (pressSelected) clearAyahSelection()
                }
            }
            MotionEvent.ACTION_UP -> if (touchActive) {
                cancelHoldTimers()
                val dx = event.rawX - downX; val dy = event.rawY - downY
                if (pressSelected && dx * dx + dy * dy > touchSlop * touchSlop) clearAyahSelection()
                val distance = PAGE_SWIPE_DISTANCE_DP * resources.displayMetrics.density
                if (event.pointerCount == 1 && page != null && kotlin.math.abs(dx) >= distance &&
                    kotlin.math.abs(dx) > kotlin.math.abs(dy) * 1.2f) {
                    // Approved book order in every UI language; the artwork never mirrors.
                    onPageSwipe?.invoke(if (dx > 0) 1 else -1)
                } else if (!touchMoved && !pressSelected && dx * dx + dy * dy <= touchSlop * touchSlop &&
                    event.eventTime - downTime < AYAH_SELECT_HOLD_MS) {
                    performClick()
                }
                cancelPendingInteraction()
            }
        }
        return true
    }
}
