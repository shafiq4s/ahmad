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
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.ahmad.quran.data.AyahId
import com.ahmad.quran.mushaf.svg.PagePoint
import kotlin.math.max
import kotlin.math.min

/** Direct vector rendering and edition touch geometry share exactly one display matrix. */
class NativeSvgPageView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    init {
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
    var onAyahTap: ((AyahId) -> Unit)? = null
    var onBlankTap: (() -> Unit)? = null
    private var multiplePointers = false
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
    private val gestures = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (multiplePointers) return true
            val chosen = hitTest(e.x, e.y)
            if (chosen == null) onBlankTap?.invoke()
            else {
                holdSelection()
                select(chosen)
                onAyahTap?.invoke(chosen)
            }
            return performClick()
        }
        override fun onFling(start: MotionEvent?, end: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (start == null || zoom > 1f || page == null) return false
            val dx = end.x-start.x
            if (kotlin.math.abs(dx) < 60*resources.displayMetrics.density ||
                kotlin.math.abs(dx) <= kotlin.math.abs(end.y-start.y) || kotlin.math.abs(velocityX) < 150) return false
            // RTL book order: a rightward swipe advances; artwork coordinates stay unchanged.
            onPageSwipe?.invoke(if (dx > 0) 1 else -1)
            return true
        }
        override fun onLongPress(e: MotionEvent) {
            if (multiplePointers) return
            val start = System.nanoTime()
            val chosen = hitTest(e.x, e.y) ?: return
            lastLongPressDispatchNanos = System.nanoTime()-start
            select(chosen)
            onAyahLongPress?.invoke(chosen)
        }
    }).apply { setOnDoubleTapListener(null) }
    private val scaleGestures = android.view.ScaleGestureDetector(context,
        object : android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: android.view.ScaleGestureDetector): Boolean {
                setZoom(zoom*detector.scaleFactor, detector.focusX, detector.focusY)
                return true
            }
        })

    fun display(page: NativeSvgPage) {
        cancelPendingGesture()
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
        cancelPendingGesture()
        removeCallbacks(clearSelection)
        onNextRender = null
        page = null
        selectedAyah = null
        selectionHeld = false
        resetHighlight()
        invalidate()
    }

    private fun cancelPendingGesture() {
        val now = android.os.SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        gestures.onTouchEvent(event)
        event.recycle()
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
    override fun performClick(): Boolean { super.performClick(); return true }
    @SuppressLint("ClickableViewAccessibility") // GestureDetector delegates ordinary taps to performClick.
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) multiplePointers = false
        if (event.pointerCount > 1) multiplePointers = true
        scaleGestures.onTouchEvent(event)
        if (scaleGestures.isInProgress) {
            val cancel = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
            gestures.onTouchEvent(cancel)
            cancel.recycle()
        } else gestures.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP && selectedAyah != null && !selectionHeld) postDelayed(clearSelection, 1800)
        return true
    }
}
