package com.app.pustakam.android.screen.bookUIView

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import kotlin.math.min

// 📖 01-Aug-2026: shared PDF helpers for the inline document block. The renderer is opened and
//   closed per page, so a 1000-page file never holds more than one page of bitmap at a time.
internal fun pdfPageCount(path: String?): Int {
    val file = path?.takeIf { it.isNotEmpty() }?.let { File(it) } ?: return 0
    if (!file.exists()) return 0
    return try {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { it.pageCount }
        }
    } catch (e: Exception) {
        e.printStackTrace(); 0
    }
}

internal fun renderPdfPageAt(path: String?, index: Int): Bitmap? {
    val file = path?.takeIf { it.isNotEmpty() }?.let { File(it) } ?: return null
    if (!file.exists()) return null
    return try {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                if (index !in 0 until renderer.pageCount) return null
                renderer.openPage(index).use { page ->
                    // cap the raster at 1440px wide — enough for a phone, cheap on memory
                    val scale = min(2f, 1440f / maxOf(page.width, 1))
                    val bitmap = Bitmap.createBitmap(
                        (page.width * scale).toInt().coerceAtLeast(1),
                        (page.height * scale).toInt().coerceAtLeast(1),
                        Bitmap.Config.ARGB_8888,
                    )
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace(); null
    }
}
