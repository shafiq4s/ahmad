package com.ahmad.quran.ui

import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.Handler
import android.os.Looper
import com.ahmad.quran.mushaf.svg.NativeSvgPageLoader
import com.ahmad.quran.mushaf.svg.NativeSvgPageView
import com.ahmad.quran.mushaf.svg.OnDemandPageAssets
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

/** Foreground loading and raw-file prefetch are independent; parsing retains only three documents. */
class MushafReaderController(
    private val context: Context,
    private val view: NativeSvgPageView,
    private val createdAt: Long,
    private val onDisplayed: (Int) -> Unit,
    private val onFailure: () -> Unit,
    private var pageAssets: OnDemandPageAssets? = null,
) : AutoCloseable {
    private val worker=Executors.newSingleThreadExecutor()
    private val downloads=Executors.newSingleThreadExecutor()
    private val handler=Handler(Looper.getMainLooper())
    @Volatile private var loader: NativeSvgPageLoader?=null
    @Volatile private var generation=0
    @Volatile private var closed=false
    private var requestedPage: Int?=null
    private val measurements=JSONArray()
    val parsedDocumentCount: Int get()=loader?.parsedDocumentCount ?: 0
    val retainedDocumentCount: Int get()=loader?.cache?.size() ?: 0
    var displaying=false
        private set
    var failed=false
        private set

    fun show(number: Int) {
        if (requestedPage==number) return
        requestedPage=number
        displaying=false
        failed=false
        view.release()
        val request=++generation
        val started=System.nanoTime()
        worker.execute {
            if (closed || request!=generation) return@execute
            try {
                val assets=pageAssets ?: OnDemandPageAssets(context.applicationContext).also { pageAssets=it }
                val current=loader ?: NativeSvgPageLoader(context.applicationContext,3,pageAssets=assets).also { loader=it }
                current.prepareCurrent(number,3)
                val cached=current.cache.snapshot().containsKey(number)
                val page=current.load(number) { !closed && request==generation }
                handler.post {
                    if (closed || request!=generation) return@post
                    view.display(page)
                    view.onNextRender={
                        displaying=true
                        val sample=if (com.ahmad.quran.BuildConfig.DEBUG) JSONObject().put("page",number).put("cacheHit",cached)
                            .put("requestToFirstDrawMs",(System.nanoTime()-started)/1e6)
                            .put("activityToFirstPageMs",if(measurements.length()==0)(System.nanoTime()-createdAt)/1e6 else JSONObject.NULL)
                            .put("parseMs",if(cached)0.0 else page.parseNanos/1e6)
                            .put("renderMs",view.lastRenderNanos/1e6)
                            .put("hardware",view.lastCanvasHardware).put("retainedDocuments",current.cache.size()) else null
                        if (sample!=null) {
                            // ponytail: retain last128 debug transitions; extend only for a requested long-session benchmark.
                            if(measurements.length()>=128) measurements.remove(0)
                            measurements.put(sample)
                        }
                        onDisplayed(number)
                        downloads.execute diskPrefetch@{
                            if (closed || request!=generation) return@diskPrefetch
                            for (neighbour in assets.neighbours(number)) {
                                if (closed || request!=generation) return@diskPrefetch
                                try { assets.ensurePage(neighbour) { !closed && request==generation } }
                                catch (_: Exception) { /* A missing neighbour never hides the current page. */ }
                            }
                            if (closed || request!=generation) return@diskPrefetch
                            try { worker.execute memoryPrefetch@{
                                if (closed || request!=generation) return@memoryPrefetch
                                try {
                                    current.prefetchNeighbours(number) { !closed && request==generation }
                                    // Process PSS, not document heap; measured after bounded prefetch.
                                    if (sample!=null) {
                                        val info=Debug.MemoryInfo();Debug.getMemoryInfo(info)
                                        handler.post {
                                            if (!closed && request==generation) sample
                                                .put("postPrefetchPssKiB",info.totalPss).put("retainedAfterPrefetch",current.cache.size())
                                        }
                                    }
                                } catch (_: Exception) { /* Optional prefetch failure must not hide the rendered page. */ }
                            } } catch (error: RejectedExecutionException) {
                                if (!closed) throw error
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                handler.post {
                    if (!closed && request==generation) {
                        failed=true
                        onFailure()
                    }
                }
            }
        }
    }
    fun retry(number: Int) {
        requestedPage=null
        show(number)
    }
    fun leave() {
        if (requestedPage==null) return
        requestedPage=null
        generation++
        displaying=false
        failed=false
        view.release()
        worker.execute { loader?.cache?.evictAll() }
    }
    /** Wait for cancelled foreground/prefetch writes before removing only downloaded artwork. */
    fun clearDownloadedPages(onFinished: (Boolean) -> Unit) {
        leave()
        worker.execute {
            if (closed) return@execute
            try {
                downloads.execute {
                    if (!closed) {
                        val cleared=runCatching { DownloadedPageCache(context).clear() }.getOrDefault(false)
                        handler.post { if(!closed) onFinished(cleared) }
                    }
                }
            } catch (error: RejectedExecutionException) { if(!closed) throw error }
        }
    }
    fun saveMeasurements() {
        if (!com.ahmad.quran.BuildConfig.DEBUG || measurements.length()==0) return
        File(context.filesDir,"mushaf-reading-measurements.json").writeText(JSONObject()
            .put("api",Build.VERSION.SDK_INT).put("model",Build.MODEL).put("transitions",measurements)
            .put("policy","Activity/request to View draw; excludes compositor. First activity time includes index dwell. PSS sampled after prefetch, not peak.")
            .toString(2))
    }
    override fun close() {
        if (closed) return
        closed=true
        generation++
        view.release()
        worker.execute { loader?.close() }
        worker.shutdown()
        downloads.shutdown()
    }
}
