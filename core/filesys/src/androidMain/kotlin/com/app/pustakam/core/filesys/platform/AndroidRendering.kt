package com.app.pustakam.core.filesys.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.os.ParcelFileDescriptor
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.app.pustakam.core.common.util.ContentType
import com.app.pustakam.core.filesys.model.ThumbnailPolicy
import com.app.pustakam.core.filesys.model.ThumbnailRequest
import com.app.pustakam.core.filesys.model.ThumbnailResult
import java.io.FileOutputStream

// 🔧 30-Jul-2026 02:10 Phase 3 — Android bindings for the three framework-heavy seams.
//   Every algorithm here is PORTED, not rewritten: the decode/scale loops come from
//   FileOps.generateThumbnail, the page count from BookPageFactory.pdfSheets, and the text
//   measurement from NoteExporter.textItem. Sizes and quality now come from the shared
//   ThumbnailPolicy instead of local constants.

class AndroidThumbnailGenerator(private val context: Context) : ThumbnailGenerator {

    override fun generate(request: ThumbnailRequest): ThumbnailResult {
        if (!ThumbnailPolicy.isEligible(request.contentType)) return ThumbnailResult.NotSupported
        return try {
            val source = context.resolveInStorage(request.sourceRelativePath)
            if (!source.exists()) return ThumbnailResult.Failed("source missing")

            val bitmap: Bitmap = when (request.contentType) {
                ContentType.IMAGE, ContentType.GIF -> decodeDownsampled(source.absolutePath, request)
                ContentType.VIDEO -> decodeVideoFrame(source.absolutePath, request)
                else -> null
            } ?: return ThumbnailResult.Failed("decode failed")

            val destination = context.resolveInStorage(request.destinationRelativePath)
            destination.parentFile?.mkdirs()
            FileOutputStream(destination).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, request.quality, out)
            }
            ThumbnailResult.Generated(request.destinationRelativePath)
        } catch (e: Exception) {
            e.printStackTrace()
            ThumbnailResult.Failed(e.message ?: "unknown")
        }
    }

    // 🔧 30-Jul-2026 02:10 bounds-decode then inSampleSize — no full-size bitmap in memory
    private fun decodeDownsampled(path: String, request: ThumbnailRequest): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = ThumbnailPolicy.sampleSizeFor(
                bounds.outWidth, bounds.outHeight, request.maxDimensionPx,
            )
        }
        return BitmapFactory.decodeFile(path, options)
    }

    // 🔧 30-Jul-2026 02:10 frame at the policy's offset, falling back to frame 0
    private fun decodeVideoFrame(path: String, request: ThumbnailRequest): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            val frame = retriever.getFrameAtTime(request.videoFrameMicros)
                ?: retriever.getFrameAtTime(0)
            frame?.let { scaleDown(it, request.maxDimensionPx) }
        } finally {
            retriever.release()
        }
    }

    private fun scaleDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val scale = ThumbnailPolicy.scaleFactorFor(bitmap.width, bitmap.height, maxDimension)
        if (scale >= 1f) return bitmap
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    }
}

class AndroidDocumentRenderer(private val context: Context) : DocumentRenderer {

    // 🔧 30-Jul-2026 02:10 ported from BookPageFactory.pdfSheets — count only, no rendering
    override fun pageCount(relativePath: String): Int = try {
        val file = context.resolveInStorage(relativePath)
        if (!file.exists()) {
            0
        } else {
            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            PdfRenderer(descriptor).use { it.pageCount }
        }
    } catch (e: Exception) {
        e.printStackTrace(); 0
    }
}

// 🔧 30-Jul-2026 02:10 ported from NoteExporter.textItem/bodyPaint/titlePaint/captionPaint/accentPaint
class AndroidTextMeasurer : TextMeasurer {

    override fun measureHeight(text: String, style: TextStyle, width: Float): Float {
        if (width <= 0f) return 0f
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = style.sizePx
            isFakeBoldText = style.bold
        }
        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width.toInt().coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
            .height
            .toFloat()
    }
}
