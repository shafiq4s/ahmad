package com.ahmad.quran

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import com.ahmad.quran.mushaf.svg.NativeSvgPageView
import com.ahmad.quran.mushaf.svg.OnDemandPageAssets
import com.ahmad.quran.mushaf.svg.PagePoint
import com.ahmad.quran.mushaf.svg.QuranSvgEdition
import com.ahmad.quran.data.AyahId
import com.ahmad.quran.ui.MushafReaderController
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[23,36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PageAssetsTest {
    private val app get()=RuntimeEnvironment.getApplication()
    private val seeds=(1..5).toSet()+300+(600..604).toSet()
    private class Server(private val events:MutableList<String>, private val body:(Int)->ByteArray={ SourcePageFixtures.original("%03d.svg".format(java.util.Locale.ROOT,it)).readBytes() },
        private val hold119:CountDownLatch?=null) : AutoCloseable {
        private val executor=Executors.newCachedThreadPool()
        private val socket=ServerSocket(0,50,InetAddress.getByName("127.0.0.1"))
        init {
            executor.execute {
                while(!socket.isClosed) {
                    val client=try { socket.accept() } catch (_:IOException) { break }
                    executor.execute {
                        try {
                            client.use {
                                val input=it.getInputStream().bufferedReader()
                                val path=input.readLine().split(' ')[1]
                                while(!input.readLine().isNullOrEmpty()) { /* Read the small HTTP request headers. */ }
                                val n=path.substringAfterLast('/').removeSuffix(".svg").toInt()
                                events.add("GET:$n")
                                val data=body(n);val output=it.getOutputStream()
                                output.write("HTTP/1.1 200 OK\r\nContent-Length: ${data.size}\r\nConnection: close\r\n\r\n".toByteArray(Charsets.US_ASCII))
                                if(n==119 && hold119!=null) {
                                    output.write(data,0,1);output.flush()
                                    hold119.await(20,TimeUnit.SECONDS)
                                    output.write(data,1,data.size-1)
                                } else output.write(data)
                            }
                        } catch (_:IOException) { /* Request cancellation deliberately closes the socket. */ }
                    }
                }
            }
        }
        val url get()="http://127.0.0.1:${socket.localPort}"
        override fun close() { hold119?.countDown();socket.close();executor.shutdownNow() }
    }

    private fun provider(url:String="")=OnDemandPageAssets(app,url,seeds,allowLocalHttp=true)
    private fun await(condition:()->Boolean) {
        val deadline=System.nanoTime()+20_000_000_000L
        while(!condition() && System.nanoTime()<deadline) { shadowOf(Looper.getMainLooper()).idle();Thread.sleep(10) }
        assertTrue("On-demand operation did not complete",condition())
    }
    private fun draw(v:NativeSvgPageView) {
        v.layout(0,0,360,640)
        val bitmap=Bitmap.createBitmap(360,640,Bitmap.Config.ARGB_8888)
        v.draw(Canvas(bitmap));bitmap.recycle();shadowOf(Looper.getMainLooper()).idle()
    }
    private fun assertOriginal(cache:File,n:Int) {
        assertArrayEquals(SourcePageFixtures.original("%03d.svg".format(java.util.Locale.ROOT,n)).readBytes(),File(cache,"%03d.svg".format(java.util.Locale.ROOT,n)).readBytes())
    }
    @Test fun bundledPagesAndBoundsNeedNoRequest() {
        val events=CopyOnWriteArrayList<String>()
        Server(events).use { server ->
            val assets=provider(server.url)
            for(n in listOf(1,300,604)) assets.openPage(n).use { assertTrue(it.read()>=0) }
            assertEquals(listOf(2,3),assets.neighbours(1));assertEquals(listOf(602,603),assets.neighbours(604))
            assets.neighbours(604).forEach { assets.ensurePage(it) }
            assertTrue(events.isEmpty());assertFalse(assets.cacheDirectory.exists())
            assertThrows(IllegalArgumentException::class.java) { assets.openPage(605) }
        }
    }
    @Test fun current121RendersBeforeFourDiskNeighboursAndMemoryRetainsOnlyThree() {
        val events=CopyOnWriteArrayList<String>();val assets:OnDemandPageAssets
        Server(events).use { server ->
            assets=provider(server.url)
            val v=NativeSvgPageView(app)
            val reader=MushafReaderController(app,v,System.nanoTime(),{ events.add("DISPLAY:$it") },{ fail("Current page failed") },assets)
            try {
                reader.show(121);await { v.page?.geometry?.pageNumber==121 }
                assertEquals(listOf("GET:121"),events.toList());assertFalse(reader.displaying)
                draw(v);assertTrue(reader.displaying)
                val point=v.screenPoint(PagePoint(120f,18f))
                assertEquals(AyahId(5,78),v.hitTest(point.x,point.y))
                await { listOf(119,120,121,122,123).all { File(assets.cacheDirectory,"%03d.svg".format(java.util.Locale.ROOT,it)).isFile } && reader.retainedDocumentCount==3 }
                assertEquals(listOf("GET:121","DISPLAY:121","GET:119","GET:120","GET:122","GET:123"),events.toList())
                assertEquals(3,reader.parsedDocumentCount)
                for(n in 119..123) assertOriginal(assets.cacheDirectory,n)
                val count=events.size
                assets.openPage(121).use { assertTrue(it.read()>=0) };assertEquals(count,events.size)
            } finally { reader.close() }
        }
        // Fresh provider, stopped HTTP server: disk files survive the parser/controller lifetime.
        val offline=provider()
        offline.openPage(121).use { assertTrue(it.read()>=0) };assertOriginal(offline.cacheDirectory,121)
    }
    @Test fun farForegroundRequestDoesNotWaitForAnOldNeighbourTransfer() {
        val events=CopyOnWriteArrayList<String>();val hold=CountDownLatch(1)
        Server(events,hold119=hold).use { server ->
            val assets=provider(server.url);val v=NativeSvgPageView(app)
            val reader=MushafReaderController(app,v,System.nanoTime(),{ events.add("DISPLAY:$it") },{ fail("Foreground failed") },assets)
            try {
                reader.show(121);await { v.page?.geometry?.pageNumber==121 };draw(v)
                await { events.contains("GET:119") }
                reader.show(400);await { v.page?.geometry?.pageNumber==400 };draw(v)
                assertTrue(events.contains("DISPLAY:400"));assertEquals(1L,hold.count)
                assertEquals(1,events.count { it=="GET:400" })
                assertTrue(reader.retainedDocumentCount<=3)
            } finally { reader.close();hold.countDown() }
        }
    }
    @Test fun wrongHashSameSizeIsRejectedAfterTwoAttemptsAndNoPartialIsAdmitted() {
        val events=CopyOnWriteArrayList<String>()
        Server(events,body={ n -> SourcePageFixtures.original("%03d.svg".format(java.util.Locale.ROOT,n)).readBytes().apply { this[100]=(this[100].toInt() xor 1).toByte() } }).use { server ->
            val assets=provider(server.url)
            assertThrows(IOException::class.java) { assets.openPage(121) }
            assertEquals(listOf("GET:121","GET:121"),events.toList())
            assertTrue(assets.cacheDirectory.listFiles()!!.isEmpty())
        }
    }
    @Test fun truncatedResponseIsRejectedAndRetryCanThenRecover() {
        val events=CopyOnWriteArrayList<String>()
        Server(events,body={n -> SourcePageFixtures.original("%03d.svg".format(java.util.Locale.ROOT,n)).readBytes().copyOf(50) }).use { server ->
            assertThrows(IOException::class.java) { provider(server.url).openPage(121) }
        }
        Server(events).use { server ->
            val assets=provider(server.url);assets.openPage(121).close()
            assertOriginal(assets.cacheDirectory,121)
            assertEquals(3,events.count { it=="GET:121" })
            assertFalse(assets.cacheDirectory.listFiles()!!.any { it.name.endsWith(".part") })
        }
    }
    @Test fun corruptDiskFileIsDiscardedAndDownloadedOnceThenWorksOffline() {
        val events=CopyOnWriteArrayList<String>()
        Server(events).use { server ->
            val assets=provider(server.url);assets.cacheDirectory.mkdirs()
            File(assets.cacheDirectory,"121.svg").writeText("corrupt")
            assets.openPage(121).close();assertOriginal(assets.cacheDirectory,121)
            assertEquals(listOf("GET:121"),events.toList())
        }
        provider().openPage(121).close()
    }
    @Test fun concurrentSamePageRequestsCoalesceAndCancellationLeavesNoCacheFile() {
        val events=CopyOnWriteArrayList<String>();val executor=Executors.newFixedThreadPool(2)
        try {
            Server(events).use { server ->
                val assets=provider(server.url)
                val jobs=(1..2).map { executor.submit { assets.openPage(121).close() } }
                jobs.forEach { it.get(20,TimeUnit.SECONDS) };assertEquals(listOf("GET:121"),events.toList())
                assertThrows(IOException::class.java) { assets.ensurePage(120) { false } }
                assertFalse(File(assets.cacheDirectory,"120.svg").exists())
            }
        } finally { executor.shutdownNow() }
    }
    @Test fun stalledBodyTimesOutWithBoundedRetriesAndCannotEnterCache() {
        val events=CopyOnWriteArrayList<String>();val hold=CountDownLatch(1)
        Server(events,hold119=hold).use { server ->
            val assets=provider(server.url)
            assertThrows(IOException::class.java) { assets.openPage(119) }
            assertEquals(listOf("GET:119","GET:119"),events.toList())
            assertTrue(assets.cacheDirectory.listFiles()!!.isEmpty())
        }
    }
    @Test fun otherEditionFilesAndIncompletePartFilesAreNeverUsed() {
        val events=CopyOnWriteArrayList<String>()
        val other=File(app.cacheDir,"mushaf-pages/another-edition").apply { mkdirs() }
        File(other,"121.svg").writeText("different edition")
        Server(events).use { server ->
            val assets=provider(server.url);assets.cacheDirectory.mkdirs()
            File(assets.cacheDirectory,".121-orphan.part").writeText("unfinished")
            assets.openPage(121).close();assertOriginal(assets.cacheDirectory,121)
            assertEquals(listOf("GET:121"),events.toList())
            assertEquals(QuranSvgEdition.ID,assets.cacheDirectory.name)
            assertTrue(File(other,"121.svg").exists())
        }
    }
    @Test fun emptyProductionBaseAndUnsafeUrlsNeverContactAnyServer() {
        assertThrows(IOException::class.java) { provider().openPage(121) }
        for(url in listOf("http://example.com","https://user:password@example.com","https://example.com/?token=value")) {
            assertThrows(IllegalArgumentException::class.java) { provider(url) }
        }
        assertThrows(IllegalArgumentException::class.java) { OnDemandPageAssets(app,"http://127.0.0.1:8773",allowLocalHttp=false) }
    }
}
