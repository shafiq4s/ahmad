package com.ahmad.quran.mushaf.svg

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** Presentation only. Neither the source polygons nor hit testing consume this geometry. */
internal class AyahHighlight private constructor(
    private val original: Path,
    private val inkBounds: List<RectF>,
    private val uncertainty: Float,
) {
    var insetReduced=false
        private set
    val patchCount: Int get()=inkBounds.size

    fun path(sourcePixelsPerScreenPixel: Float, density: Float): Path {
        val inset=2.5f*density*sourcePixelsPerScreenPixel
        val radius=1.25f*density*sourcePixelsPerScreenPixel
        val safePadding=uncertainty+0.5f*density*sourcePixelsPerScreenPixel
        val outerPadding=uncertainty+1.25f*density*sourcePixelsPerScreenPixel
        val border=Path()
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style=Paint.Style.STROKE;strokeWidth=2*inset;strokeJoin=Paint.Join.ROUND
        }.getFillPath(original,border)
        val core=Path(original)
        if(!core.op(border,Path.Op.DIFFERENCE)) core.set(original)
        val result=Path()
        val protected=Path()
        for(bounds in inkBounds) {
            result.addPath(rounded(bounds,outerPadding,radius))
            protected.addPath(rounded(bounds,safePadding,min(radius,safePadding)))
        }
        // Compound winding paths union the rectangles without an expensive boolean op per patch.
        // Recover the ink envelope locally wherever the 2.5dp erosion would cut a mark.
        val fallback=Path(protected)
        fallback.op(core,Path.Op.DIFFERENCE)
        insetReduced=!fallback.isEmpty
        if(!result.op(core,Path.Op.INTERSECT)) result.set(original)
        result.op(protected,Path.Op.UNION)
        if(inkBounds.isEmpty()) result.set(original)
        result.op(original,Path.Op.INTERSECT)
        return result
    }

    private fun rounded(bounds: RectF, padding: Float, radius: Float): Path {
        val rect=RectF(bounds).apply { inset(-padding,-padding) }
        return Path().apply { addRoundRect(rect,radius,radius,Path.Direction.CW) }
    }

    companion object {
        private fun dilate(input: ByteArray,width: Int,height: Int,radius: Int,horizontal: Boolean): ByteArray {
            val result=ByteArray(input.size)
            val lines=if(horizontal)height else width
            val length=if(horizontal)width else height
            fun index(line: Int,position: Int)=if(horizontal)line*width+position else position*width+line
            for(line in 0 until lines) {
                var count=0
                for(i in 0..min(radius,length-1)) count+=input[index(line,i)].toInt()
                for(position in 0 until length) {
                    if(count>0) result[index(line,position)]=1
                    val remove=position-radius
                    val add=position+radius+1
                    if(remove>=0)count-=input[index(line,remove)].toInt()
                    if(add<length)count+=input[index(line,add)].toInt()
                }
            }
            return result
        }

        fun prepare(page: NativeSvgPage, region: AyahPolygonRegion): AyahHighlight {
            val original=region.path()
            val bounds=RectF();original.computeBounds(bounds,true)
            // ponytail: selected-ayah analysis only, <=1024px per edge; never retain a page raster.
            val scale=min(3f,1024f/max(bounds.width(),bounds.height()))
            val width=max(1,ceil(bounds.width()*scale).toInt())
            val height=max(1,ceil(bounds.height()*scale).toInt())
            val bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
            val pixels=IntArray(width*height)
            try {
                val canvas=Canvas(bitmap)
                canvas.scale(scale,scale);canvas.translate(-bounds.left,-bounds.top)
                canvas.clipPath(original)
                page.artwork.renderToCanvas(canvas,page.geometry.viewBox)
                bitmap.getPixels(pixels,0,width,0,0,width,height)
            } finally { bitmap.recycle() }
            val occupied=ByteArray(pixels.size)
            for(i in pixels.indices) {
                val color=pixels[i]
                if(Color.alpha(color)>0 && min(Color.red(color),min(Color.green(color),Color.blue(color)))<250) occupied[i]=1
            }
            // Join nearby ink locally, not by a full-width row projection or guessed line height.
            val expanded=dilate(dilate(occupied,width,height,max(1,ceil(scale*0.6f).toInt()),true),
                width,height,max(1,ceil(scale*0.35f).toInt()),false)
            val queue=IntArray(pixels.size)
            val bands=mutableListOf<RectF>()
            for(seed in expanded.indices) {
                if(expanded[seed].toInt()==0) continue
                var head=0;var tail=1;queue[0]=seed;expanded[seed]=0
                var left=width;var right=-1;var top=height;var bottom=-1
                while(head<tail) {
                    val cell=queue[head++];val x=cell%width;val y=cell/width
                    if(occupied[cell].toInt()!=0) {
                        left=min(left,x);right=max(right,x);top=min(top,y);bottom=max(bottom,y)
                    }
                    for(dy in -1..1) for(dx in -1..1) {
                        val nx=x+dx;val ny=y+dy
                        if(nx !in 0 until width || ny !in 0 until height) continue
                        val next=ny*width+nx
                        if(expanded[next].toInt()!=0) { expanded[next]=0;queue[tail++]=next }
                    }
                }
                if(right>=0) bands.add(RectF(bounds.left+left/scale,bounds.top+top/scale,
                    bounds.left+(right+1)/scale,bounds.top+(bottom+1)/scale))
            }
            return AyahHighlight(original,bands,2f/scale)
        }
    }
}
