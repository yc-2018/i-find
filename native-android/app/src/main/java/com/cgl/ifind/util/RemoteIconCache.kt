package com.cgl.ifind.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

object RemoteIconCache {
  private val executor = Executors.newFixedThreadPool(2) { runnable ->
    Thread(runnable, "ifind-icon-loader").apply { isDaemon = true }
  }
  private val mainHandler = Handler(Looper.getMainLooper())
  private val memoryCache = object : LruCache<String, Bitmap>(MEMORY_CACHE_KB) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
  }
  private val inFlight = ConcurrentHashMap<String, CopyOnWriteArrayList<(Bitmap?) -> Unit>>()

  fun load(context: Context, rawUrl: String, callback: (Bitmap?) -> Unit) {
    val url = rawUrl.trim()
    if (!isSupportedUrl(url)) {
      callback(null)
      return
    }

    memoryCache.get(url)?.let {
      callback(it)
      return
    }

    val callbacks = CopyOnWriteArrayList<(Bitmap?) -> Unit>().apply { add(callback) }
    val existingCallbacks = inFlight.putIfAbsent(url, callbacks)
    if (existingCallbacks != null) {
      existingCallbacks.add(callback)
      return
    }

    val appContext = context.applicationContext
    executor.execute {
      val bitmap = runCatching { loadOrDownload(appContext, url) }.getOrNull()
      if (bitmap != null) memoryCache.put(url, bitmap)
      val pendingCallbacks = inFlight.remove(url).orEmpty()
      mainHandler.post {
        pendingCallbacks.forEach { it(bitmap) }
      }
    }
  }

  fun isSupportedUrl(rawUrl: String): Boolean {
    val protocol = runCatching { URL(rawUrl.trim()).protocol.lowercase() }.getOrNull()
    return protocol == "http" || protocol == "https"
  }

  private fun loadOrDownload(context: Context, url: String): Bitmap? {
    val cacheFile = cacheFile(context, url)
    decodeSampledBitmap(cacheFile)?.let { return it }
    cacheFile.delete()

    val temporaryFile = File(cacheFile.parentFile, "${cacheFile.name}.download")
    temporaryFile.delete()
    val downloaded = download(url, temporaryFile)
    if (!downloaded) {
      temporaryFile.delete()
      return null
    }

    val bitmap = decodeSampledBitmap(temporaryFile)
    if (bitmap == null) {
      temporaryFile.delete()
      return null
    }

    if (!temporaryFile.renameTo(cacheFile)) {
      temporaryFile.copyTo(cacheFile, overwrite = true)
      temporaryFile.delete()
    }
    return bitmap
  }

  private fun download(rawUrl: String, targetFile: File): Boolean {
    val connection = runCatching {
      (URL(rawUrl).openConnection() as HttpURLConnection).apply {
        connectTimeout = CONNECT_TIMEOUT_MS
        readTimeout = READ_TIMEOUT_MS
        instanceFollowRedirects = true
        useCaches = true
        setRequestProperty("Accept", "image/*")
        setRequestProperty("User-Agent", "I-find-Android")
      }
    }.getOrNull() ?: return false

    return try {
      val responseCode = connection.responseCode
      if (responseCode !in 200..299) return false
      if (!isSupportedUrl(connection.url.toString())) return false
      val contentLength = connection.contentLengthLong
      if (contentLength > MAX_DOWNLOAD_BYTES) return false

      connection.inputStream.buffered().use { input ->
        targetFile.outputStream().buffered().use { output ->
          val buffer = ByteArray(BUFFER_SIZE)
          var totalBytes = 0L
          while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            totalBytes += read
            if (totalBytes > MAX_DOWNLOAD_BYTES) return false
            output.write(buffer, 0, read)
          }
        }
      }
      targetFile.length() > 0L
    } catch (_: Exception) {
      false
    } finally {
      connection.disconnect()
    }
  }

  private fun cacheFile(context: Context, url: String): File {
    val directory = File(context.filesDir, "icons/remote").apply { mkdirs() }
    return File(directory, "${sha256(url)}.img")
  }

  private fun sha256(value: String): String {
    return MessageDigest.getInstance("SHA-256")
      .digest(value.toByteArray(Charsets.UTF_8))
      .joinToString("") { "%02x".format(it) }
  }

  private fun decodeSampledBitmap(file: File): Bitmap? {
    if (!file.isFile || file.length() <= 0L) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (
      bounds.outWidth / sampleSize > MAX_BITMAP_SIZE * 2 ||
      bounds.outHeight / sampleSize > MAX_BITMAP_SIZE * 2
    ) {
      sampleSize *= 2
    }
    return BitmapFactory.decodeFile(
      file.absolutePath,
      BitmapFactory.Options().apply { inSampleSize = sampleSize }
    )
  }

  private const val CONNECT_TIMEOUT_MS = 4_000
  private const val READ_TIMEOUT_MS = 6_000
  private const val MAX_DOWNLOAD_BYTES = 2_500_000L
  private const val BUFFER_SIZE = 8 * 1024
  private const val MAX_BITMAP_SIZE = 256
  private const val MEMORY_CACHE_KB = 4 * 1024
}
