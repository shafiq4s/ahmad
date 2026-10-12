package com.ahmad.quran.mushaf.svg

import android.content.Context
import com.ahmad.quran.mushaf.MushafPageAssetProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InterruptedIOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

/** Verified raw files only. This cache never owns parsed SVG documents. Worker-thread callers only. */
class OnDemandPageAssets(
    private val context: Context,
    baseUrl: String? = null,
    seedPages: Set<Int>? = null,
    val cacheDirectory: File = File(context.cacheDir, "mushaf-pages/${QuranSvgEdition.ID}"),
    allowLocalHttp: Boolean? = null,
) : MushafPageAssetProvider {
    private val config = context.assets.open("mushaf-svg/page-assets-config.json").bufferedReader().use { JSONObject(it.readText()) }
    private val manifest = context.assets.open("mushaf-svg/source-manifest.json").bufferedReader().use { JSONObject(it.readText()) }
    override val editionId = manifest.getString("editionId")
    override val availablePages = (1..604).toList()
    private val seeds = seedPages ?: config.getJSONArray("seedPages").let { a -> (0 until a.length()).map { a.getInt(it) }.toSet() }
    private val remoteBase = (baseUrl ?: config.getString("baseUrl")).trimEnd('/')
    private val localHttp = allowLocalHttp ?: config.getBoolean("allowLocalHttp")
    private data class Expected(val bytes: Long, val sha256: String)
    private val expected = manifest.getJSONArray("files").let { rows ->
        (0 until rows.length()).map { rows.getJSONObject(it) }.filter { it.getString("filename").endsWith(".svg") }
            .associate { it.getString("filename").removeSuffix(".svg").toInt() to Expected(it.getLong("bytes"), it.getString("sha256")) }
    }
    // Two workers (foreground and raw-file prefetch) coalesce the same page's in-flight transfer.
    private val pageLocks = Array(605) { Any() }

    init {
        require(editionId == QuranSvgEdition.ID && manifest.getString("commit") == QuranSvgEdition.COMMIT)
        require(expected.keys == availablePages.toSet() && seeds.all { it in availablePages })
        require(expected.values.all { it.bytes > 0 && it.sha256.matches(Regex("[0-9a-f]{64}")) })
        if (remoteBase.isNotEmpty()) {
            val url = URL(remoteBase)
            require(url.userInfo == null && url.query == null && url.ref == null && url.host.isNotEmpty())
            require(url.protocol == "https" || (localHttp && url.protocol == "http" && url.host in setOf("localhost", "127.0.0.1", "10.0.2.2")))
        }
    }

    override fun openPage(pageNumber: Int): InputStream = openPage(pageNumber) { true }

    fun openPage(pageNumber: Int, shouldContinue: () -> Boolean): InputStream {
        val file = ensurePage(pageNumber, shouldContinue)
        return file?.inputStream() ?: context.assets.open("mushaf-svg/${filename(pageNumber)}")
    }

    fun isLocal(pageNumber: Int): Boolean {
        require(pageNumber in availablePages)
        return pageNumber in seeds || synchronized(pageLocks[pageNumber]) { validCache(pageNumber) != null }
    }

    /** Null means bundled. A final cache file exists only after exact length/hash verification. */
    fun ensurePage(pageNumber: Int, shouldContinue: () -> Boolean = { true }): File? {
        require(pageNumber in availablePages)
        checkActive(shouldContinue)
        if (pageNumber in seeds) return null
        return synchronized(pageLocks[pageNumber]) {
            checkActive(shouldContinue)
            validCache(pageNumber)?.let { return@synchronized it }
            if (remoteBase.isEmpty()) throw IOException("Page source is not configured")
            if (!cacheDirectory.isDirectory && !cacheDirectory.mkdirs()) throw IOException("Cannot create page cache")
            var failure: IOException? = null
            repeat(2) {
                checkActive(shouldContinue)
                try { return@synchronized download(pageNumber, shouldContinue) }
                catch (error: IOException) { failure = error }
            }
            throw checkNotNull(failure)
        }
    }

    fun neighbours(pageNumber: Int): List<Int> {
        require(pageNumber in availablePages)
        return ((pageNumber - 2)..(pageNumber + 2)).filter { it in 1..604 && it != pageNumber }
    }

    private fun validCache(number: Int): File? {
        val file = File(cacheDirectory, filename(number))
        if (!file.exists()) return null
        val pinned = expected.getValue(number)
        val valid = try {
            file.isFile && file.length() == pinned.bytes && file.inputStream().use { input ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(65536)
                var count = input.read(buffer)
                while (count >= 0) { digest.update(buffer, 0, count); count = input.read(buffer) }
                hex(digest.digest()) == pinned.sha256
            }
        } catch (_: IOException) { false }
        if (valid) return file
        if (!file.delete()) throw IOException("Cannot discard invalid cached page")
        return null
    }

    private fun download(number: Int, shouldContinue: () -> Boolean): File {
        val pinned = expected.getValue(number)
        val temp = File.createTempFile(".${number}-", ".part", cacheDirectory)
        val connection = URL("$remoteBase/$editionId/${filename(number)}").openConnection() as HttpURLConnection
        val deadline = System.nanoTime() + 30_000_000_000L
        try {
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept-Encoding", "identity")
            checkActive(shouldContinue)
            if (connection.responseCode != HttpURLConnection.HTTP_OK) throw IOException("Page HTTP response rejected")
            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            connection.inputStream.use { input ->
                FileOutputStream(temp).use { output ->
                    val buffer = ByteArray(65536)
                    while (true) {
                        checkActive(shouldContinue)
                        if (System.nanoTime() >= deadline) throw SocketTimeoutException("Page download deadline exceeded")
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > pinned.bytes) throw IOException("Page exceeds pinned size")
                        output.write(buffer, 0, count); digest.update(buffer, 0, count)
                    }
                    output.fd.sync()
                }
            }
            checkActive(shouldContinue)
            if (total != pinned.bytes || hex(digest.digest()) != pinned.sha256) throw IOException("Page SHA-256/size mismatch")
            val target = File(cacheDirectory, filename(number))
            // Same-directory POSIX rename is atomic on Android, including API23.
            if (!temp.renameTo(target)) throw IOException("Cannot commit verified page")
            return target
        } finally {
            connection.disconnect()
            temp.delete()
        }
    }

    private fun checkActive(shouldContinue: () -> Boolean) {
        if (!shouldContinue() || Thread.currentThread().isInterrupted) throw InterruptedIOException("Page request superseded")
    }
    private fun filename(number: Int) = "%03d.svg".format(Locale.ROOT, number)
    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }
}
