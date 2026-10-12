package com.ahmad.quran.mushaf.svg

import com.ahmad.quran.mushaf.svg.OpeningPageHitOverrides

import com.ahmad.quran.mushaf.svg.NativeSvgPageView

import com.ahmad.quran.mushaf.svg.NativeSvgPageLoader

import com.ahmad.quran.mushaf.svg.NativeSvgPage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.ahmad.quran.data.AyahId
import com.ahmad.quran.mushaf.svg.AyahPolygonRegion
import com.ahmad.quran.mushaf.svg.PagePoint
import com.ahmad.quran.mushaf.svg.PagePolygon
import com.ahmad.quran.mushaf.svg.PolygonMushafPage
import org.json.JSONArray
import org.json.JSONObject
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
@Config(sdk = [23, 36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MushafSvgGeometryTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Test fun sourceIdentityAndOpeningOverridesRemainEditionScopedAndUnambiguous() {
        NativeSvgPageLoader(context, 1, "mushaf-svg").use { loader ->
            for (number in listOf(1,2,48,121,604)) {
                val page = loader.load(number).geometry
                assertTrue(page.regions.all { it.editionId == page.editionId && it.pageNumber == number })
                if (number in 1..2) {
                    for (y in 0..440) for (x in 0..480) {
                        val matches = page.regions.filter { region -> region.polygons.any { it.contains(x/2f, y/2f) } }
                        assertTrue("Ambiguous hit page $number at ${x/2f},${y/2f}", matches.size <= 1)
                    }
                    if (android.os.Build.VERSION.SDK_INT >= 26) {
                        page.regions.forEachIndexed { i, a -> page.regions.drop(i+1).forEach { b ->
                            val intersect = Path()
                            assertTrue(intersect.op(a.path(), b.path(), Path.Op.INTERSECT))
                            val bounds = RectF()
                            intersect.computeBounds(bounds, true)
                            assertTrue("Unexpected positive-area overlap", bounds.isEmpty)
                        } }
                    }
                }
            }
            assertEquals(AyahId(5, 78), loader.load(121).geometry.regions.first().ayahId)
            assertEquals(AyahId(114, 6), loader.load(604).geometry.regions.last().ayahId)
            assertEquals(listOf(AyahId(2, 282)), loader.load(48).geometry.regions.map { it.ayahId })
        }
        val overrides = OpeningPageHitOverrides(context.assets.open("mushaf-svg/opening-hit-overrides.json").bufferedReader().use { it.readText() })
        for (number in 3..604) {
            val region = AyahPolygonRegion("quran_ws_hafs_kfqc_b4155f0", number, 1, 1,
                listOf(PagePolygon(listOf(PagePoint(0f,0f),PagePoint(10f,0f),PagePoint(10f,10f),PagePoint(0f,10f)))))
            val page = PolygonMushafPage(region.editionId, number, RectF(0f,0f,345f,550f),RectF(0f,0f,345f,550f),listOf(region))
            assertSame(page, overrides.apply(page))
        }
    }

    @Test fun everySampleAyahAndReviewedInkNotchesResolveAfterScalingAndZoom() {
        val view = NativeSvgPageView(context).apply { layoutDirection = View.LAYOUT_DIRECTION_RTL }
        assertEquals(if (android.os.Build.VERSION.SDK_INT < 26) View.LAYER_TYPE_SOFTWARE else View.LAYER_TYPE_NONE, view.layerType)
        var returned: AyahId? = null
        view.onAyahLongPress = { returned = it }
        NativeSvgPageLoader(context, 1, "mushaf-svg").use { loader ->
            for (number in listOf(1,2,48,121,604)) {
                val loaded = loader.load(number)
                view.display(loaded)
                val json = JSONArray(context.assets.open("mushaf-svg/%03d.json".format(number)).bufferedReader().use { it.readText() })
                val samples = (0 until json.length()).map { index ->
                    val row = json.getJSONObject(index)
                    PagePoint(row.getDouble("x").toFloat(),row.getDouble("y").toFloat()) to
                        AyahId(row.getInt("surahNumber"), row.getInt("ayahNumber"))
                } + if (number == 2) listOf(
                    PagePoint(175f,133.7f) to AyahId(2,4), // Damma belongs to the lower line's unzila.
                    PagePoint(65.5f,159f) to AyahId(2,4), // Descender in hum belongs to the upper line.
                    PagePoint(110f,158.5f) to AyahId(2,5), // Waqf sign belongs to the next line.
                    PagePoint(196.5f,160f) to AyahId(2,5),
                ) else if (number == 1) listOf(
                    PagePoint(100f,130.1f) to AyahId(1,5),
                    PagePoint(110f,130.5f) to AyahId(1,7),
                    PagePoint(150f,160.5f) to AyahId(1,6),
                    PagePoint(73f,205f) to AyahId(1,7),
                ) else emptyList()
                for ((width,height) in listOf(320 to 480,412 to 732,600 to 960)) {
                    layout(view,width,height)
                    for (zoom in listOf(1f,2f,4f)) {
                        view.setZoom(zoom)
                        for ((point, expected) in samples) {
                            val screen = view.screenPoint(point)
                            assertEquals(expected,view.hitTest(screen.x,screen.y))
                            val first = returned
                            touch(view,screen,false)
                            assertEquals(first,returned)
                            touch(view,screen,true)
                            assertEquals(expected,returned)
                        }
                    }
                }
            }
        }
        view.release()
    }

    @Test fun cachesRetainOnlyRequestedCapacityAndCancelStaleViewSelection() {
        for (capacity in listOf(1,3)) NativeSvgPageLoader(context, capacity, "mushaf-svg").use { loader ->
            val page = loader.load(48)
            if(capacity==3)loader.prefetchNeighbours(48)
            assertEquals(capacity,loader.cache.size())
            if(capacity==3)assertSame(page,loader.load(48))
            for(number in listOf(1,2,48,121,604)) {
                loader.prepareCurrent(number,capacity)
                loader.load(number)
                if(capacity==3)loader.prefetchNeighbours(number)
                assertTrue(loader.cache.size()<=capacity)
                val index=loader.pages.indexOf(number)
                val allowed=if(capacity==1)setOf(number) else
                    listOfNotNull(loader.pages.getOrNull(index-1),number,loader.pages.getOrNull(index+1)).toSet()
                assertTrue(allowed.containsAll(loader.cache.snapshot().keys))
            }
        }
    }

    @Test fun pendingLongPressCannotSelectTheReplacementPageAndHeadingsRemainUnassigned() {
        val view=NativeSvgPageView(context)
        layout(view,412,732)
        var selected: AyahId?=null
        view.onAyahLongPress={selected=it}
        NativeSvgPageLoader(context, 1, "mushaf-svg").use { loader ->
            for(number in listOf(1,2)) {
                view.display(loader.load(number))
                assertNull(view.page!!.geometry.hitTest(110f,-40f))
                if(number==2)assertNull(view.page!!.geometry.hitTest(110f,30f))
            }
            val page=loader.load(604)
            view.display(page)
            val row=JSONArray(context.assets.open("mushaf-svg/604.json").bufferedReader().use { it.readText() }).getJSONObject(0)
            val point=view.screenPoint(PagePoint(row.getDouble("x").toFloat(),row.getDouble("y").toFloat()))
            val now=SystemClock.uptimeMillis()
            val down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,point.x,point.y,0)
            view.dispatchTouchEvent(down);down.recycle()
            view.release()
            view.display(page)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1200))
            assertNull(selected)
            assertNull(view.selectedAyah)
        }
        view.release()
    }

    private fun touch(view: View,point: PagePoint,long: Boolean) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2000))
        val now = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,point.x,point.y,0)
        view.dispatchTouchEvent(down);down.recycle()
        if(long)shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(
            ViewConfiguration.getLongPressTimeout().toLong()+ViewConfiguration.getTapTimeout()+150))
        val up = MotionEvent.obtain(now,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,point.x,point.y,0)
        view.dispatchTouchEvent(up);up.recycle()
    }
    private fun layout(view: View,w: Int,h: Int) {
        view.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY))
        view.layout(0,0,w,h)
    }
    private fun capture(view: View,w: Int,h: Int) = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).also {
        it.eraseColor(Color.WHITE);view.draw(Canvas(it))
    }
    private fun save(bitmap: Bitmap,file: File) { file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) } }
}
