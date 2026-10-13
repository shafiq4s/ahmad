package com.ahmad.quran

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import com.ahmad.quran.data.AyahId
import com.ahmad.quran.mushaf.svg.*
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[23,36],qualifiers="xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class ReaderInteractionTest {
    private val app get()=RuntimeEnvironment.getApplication()
    private fun idle(ms:Long)=shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
    private fun withPage(block:(NativeSvgPageView,PagePoint)->Unit) {
        SourcePageFixtures.install(app)
        NativeSvgPageLoader(app,1).use { loader ->
            val v=NativeSvgPageView(app);v.layout(0,0,600,960);v.display(loader.load(1))
            val row=JSONArray(app.assets.open("mushaf-svg/001.json").bufferedReader().use { it.readText() }).getJSONObject(0)
            val point=v.screenPoint(PagePoint(row.getDouble("x").toFloat(),row.getDouble("y").toFloat()))
            assertEquals(AyahId(1,1),v.hitTest(point.x,point.y))
            try { block(v,point) } finally { v.release() }
        }
    }
    private fun event(v:NativeSvgPageView,action:Int,p:PagePoint,offsetY:Float=0f) {
        val now=SystemClock.uptimeMillis();val e=MotionEvent.obtain(now,now,action,p.x,p.y,0)
        if(offsetY!=0f)e.offsetLocation(0f,offsetY)
        v.dispatchTouchEvent(e);e.recycle()
    }
    @Test fun shortTextAndBlankTapsOnlyToggleChromeAndAccessibleClickDoesTheSame()=withPage { v,p ->
        var taps=0;var options=0
        assertTrue(v.isClickable);assertTrue(v.isFocusable)
        v.onReaderTap={taps++};v.onAyahLongPress={options++}
        event(v,MotionEvent.ACTION_DOWN,p);idle(60);event(v,MotionEvent.ACTION_UP,p)
        assertEquals(1,taps);assertNull(v.selectedAyah);idle(1000);assertEquals(0,options)
        val blank=PagePoint(1f,1f);assertNull(v.hitTest(blank.x,blank.y))
        event(v,MotionEvent.ACTION_DOWN,blank);idle(60);event(v,MotionEvent.ACTION_UP,blank)
        assertEquals(2,taps);assertNull(v.selectedAyah)
        v.performClick();assertEquals(3,taps)
    }
    @Test fun holdsHaveDistinct500And900msBoundariesAndReleaseKeepsSelection()=withPage { v,p ->
        var selected=0;var options=0;var taps=0
        v.onAyahPress={selected++};v.onAyahLongPress={options++};v.onReaderTap={taps++}
        val down=SystemClock.uptimeMillis()
        event(v,MotionEvent.ACTION_DOWN,p);idle(499);assertNull(v.selectedAyah)
        idle(1);assertEquals(AyahId(1,1),v.selectedAyah);assertEquals(1,selected);assertEquals(0,options)
        // Native animation frames can advance Robolectric's clock; release at an absolute800ms.
        idle((down+800-SystemClock.uptimeMillis()).coerceAtLeast(0))
        assertTrue(SystemClock.uptimeMillis()<down+900)
        event(v,MotionEvent.ACTION_UP,p);idle(1000)
        assertEquals(AyahId(1,1),v.selectedAyah);assertEquals(0,options);assertEquals(0,taps)
        event(v,MotionEvent.ACTION_DOWN,p);idle(899);assertEquals(0,options)
        idle(1);assertEquals(1,options);idle(300);assertEquals(1,options)
        event(v,MotionEvent.ACTION_UP,p);idle(1000);assertEquals(1,options)
    }
    @Test fun swipesCancelPendingHoldsAndNeverToggleChrome()=withPage { v,p ->
        val swipes=mutableListOf<Int>();var taps=0;var options=0
        v.onPageSwipe={swipes+=it};v.onReaderTap={taps++};v.onAyahLongPress={options++}
        val end=PagePoint(p.x+180f,p.y+5f)
        event(v,MotionEvent.ACTION_DOWN,p);idle(450);event(v,MotionEvent.ACTION_MOVE,end);idle(500);event(v,MotionEvent.ACTION_UP,end)
        assertEquals(listOf(1),swipes);assertNull(v.selectedAyah);assertEquals(0,options);assertEquals(0,taps)
        event(v,MotionEvent.ACTION_DOWN,end);idle(40);event(v,MotionEvent.ACTION_MOVE,p);event(v,MotionEvent.ACTION_UP,p)
        assertEquals(listOf(1,-1),swipes);assertEquals(0,taps)
    }
    @Test fun verticalMovementAndMotionAfterSelectionCancelOptionsWithoutNavigation()=withPage { v,p ->
        var swipes=0;var options=0;var taps=0
        v.onPageSwipe={swipes++};v.onAyahLongPress={options++};v.onReaderTap={taps++}
        val moved=PagePoint(p.x+5f,p.y+180f)
        event(v,MotionEvent.ACTION_DOWN,p);idle(500);assertNotNull(v.selectedAyah)
        event(v,MotionEvent.ACTION_MOVE,moved);idle(500);event(v,MotionEvent.ACTION_UP,moved)
        assertNull(v.selectedAyah);assertEquals(0,options);assertEquals(0,swipes);assertEquals(0,taps)
        // Missing/coalesced MOVE must not turn a large UP displacement into a tap.
        event(v,MotionEvent.ACTION_DOWN,p);event(v,MotionEvent.ACTION_UP,moved);assertEquals(0,taps)
    }
    @Test fun blankHoldAndExplicitCancellationNeverSelectOrOpenOptions()=withPage { v,p ->
        var options=0;var taps=0;v.onAyahLongPress={options++};v.onReaderTap={taps++}
        val blank=PagePoint(1f,1f)
        event(v,MotionEvent.ACTION_DOWN,blank);idle(1000);event(v,MotionEvent.ACTION_UP,blank)
        assertNull(v.selectedAyah);assertEquals(0,options);assertEquals(0,taps)
        event(v,MotionEvent.ACTION_DOWN,p);idle(200);event(v,MotionEvent.ACTION_CANCEL,p);idle(1000)
        assertNull(v.selectedAyah);assertEquals(0,options)
        event(v,MotionEvent.ACTION_DOWN,p);v.cancelPendingInteraction();idle(1000);assertNull(v.selectedAyah)
    }
    @Test fun localCoordinatesChangingDuringChromeLayoutDoNotCancelAStationaryFinger()=withPage { v,p ->
        var options:AyahId?=null;v.onAyahLongPress={options=it}
        event(v,MotionEvent.ACTION_DOWN,p);idle(500)
        // offsetLocation changes local coordinates while raw screen coordinates stay fixed.
        event(v,MotionEvent.ACTION_MOVE,p,80f);idle(400)
        assertEquals(AyahId(1,1),options);assertEquals(AyahId(1,1),v.selectedAyah)
        event(v,MotionEvent.ACTION_UP,p,80f)
    }
    @Test fun pinchAndTwoFingerMovementCannotScalePanSwipeOrFireSelection()=withPage { v,p ->
        var taps=0;var options=0;var swipes=0
        v.onReaderTap={taps++};v.onAyahLongPress={options++};v.onPageSwipe={swipes++}
        val before=v.screenPoint(PagePoint(90f,80f))
        event(v,MotionEvent.ACTION_DOWN,p)
        val properties=Array(2) { n -> MotionEvent.PointerProperties().apply { id=n;toolType=MotionEvent.TOOL_TYPE_FINGER } }
        fun multi(action:Int,spread:Float) {
            val coords=Array(2) { n -> MotionEvent.PointerCoords().apply { x=p.x+if(n==0)-spread else spread;y=p.y;pressure=1f;size=1f } }
            val now=SystemClock.uptimeMillis();val e=MotionEvent.obtain(now,now,action,2,properties,coords,0,0,1f,1f,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0)
            v.dispatchTouchEvent(e);e.recycle()
        }
        multi(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),30f)
        multi(MotionEvent.ACTION_MOVE,120f);idle(1000)
        multi(MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),120f)
        event(v,MotionEvent.ACTION_UP,PagePoint(p.x+180f,p.y))
        assertEquals(before,v.screenPoint(PagePoint(90f,80f)));assertNull(v.selectedAyah)
        assertEquals(0,taps);assertEquals(0,options);assertEquals(0,swipes)
    }
}
