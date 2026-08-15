package com.app.pustakam.android.screen.bookUIView

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

// 📖 15-Aug-2026: the ONE place a PDF page becomes a bitmap — memory-bounded LRU plus a render
//   gate, so inline sheets in a note cost no more than the standalone reader. Mirrors iOS
//   PdfSheetCache + PdfRenderGate. Cached bitmaps are shared, so callers must never recycle them.
object PdfPageRenderer {

    const val FULL_SCREEN_WIDTH_PX = 2048

    private const val MIN_WIDTH_PX = 320
    private const val MAX_SCALE = 2f

    private val gate = Semaphore(2)

    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 8).coerceIn(8L * 1024 * 1024, 64L * 1024 * 1024).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun pageCount(path: String): Int = try {
        val descriptor = ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
        PdfRenderer(descriptor).use { it.pageCount }
    } catch (e: Exception) {
        e.printStackTrace(); 0
    }

    fun cached(path: String, pageIndex: Int, targetWidthPx: Int): Bitmap? =
        cache.get(key(path, pageIndex, sanitize(targetWidthPx)))

    suspend fun render(path: String, pageIndex: Int, targetWidthPx: Int): Bitmap? {
        val width = sanitize(targetWidthPx)
        val key = key(path, pageIndex, width)
        cache.get(key)?.let { return it }
        // at most two pages decode at once — a fast scroll queues instead of thrashing memory
        return gate.withPermit {
            cache.get(key) ?: withContext(Dispatchers.IO) { draw(path, pageIndex, width) }
                ?.also { cache.put(key, it) }
        }
    }

    private fun sanitize(targetWidthPx: Int) = targetWidthPx.coerceIn(MIN_WIDTH_PX, FULL_SCREEN_WIDTH_PX)

    private fun key(path: String, pageIndex: Int, width: Int) = "$path#$pageIndex@$width"

    private fun draw(path: String, pageIndex: Int, targetWidthPx: Int): Bitmap? = try {
        val descriptor = ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
        PdfRenderer(descriptor).use { renderer ->
            if (pageIndex !in 0 until renderer.pageCount) null else renderer.openPage(pageIndex).use { page ->
                // never upscale past 2x: that is what the shipped full-screen reader already did
                val scale = min(MAX_SCALE, targetWidthPx / page.width.coerceAtLeast(1).toFloat())
                val bitmap = createBitmap(
                    (page.width * scale).toInt().coerceAtLeast(1),
                    (page.height * scale).toInt().coerceAtLeast(1),
                )
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }
        }
    } catch (e: Exception) {
        e.printStackTrace(); null
    }
}
