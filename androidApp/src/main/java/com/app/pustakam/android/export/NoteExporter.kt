package com.app.pustakam.android.export

// 🔧 20-Jul-2026: NEW FEATURE (export) — turns a note into a PDF / PNG / DOCX file. Walks the
//   SHARED NoteExportBuilder blocks; DOCX bytes come straight from the SHARED DocxExporter (only
//   image base64 is supplied here). PDF + PNG share ONE layout pass (buildItems) so text/image
//   placement isn't written twice.
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Base64
import com.app.pustakam.core.model.models.response.notes.Note
import com.app.pustakam.core.filesys.export.DocxExporter
import com.app.pustakam.core.filesys.export.ExportBlock
import com.app.pustakam.core.filesys.export.ExportBlockKind
import com.app.pustakam.core.filesys.export.ExportFormat
import com.app.pustakam.core.filesys.export.NoteExportBuilder
import com.app.pustakam.core.common.util.getCurrentTimestamp
import com.app.pustakam.core.filesys.path.PathPolicy
import java.io.File
import java.io.FileOutputStream

object NoteExporter {

    // A4 @ 72dpi and a long-image width; margins shared by both
    private const val PDF_WIDTH = 595
    private const val PDF_HEIGHT = 842
    private const val IMAGE_WIDTH = 1080
    private const val MARGIN = 40f
    private const val BLOCK_GAP = 14f

    // 🔧 20-Jul-2026: one measured element to place (text or a scaled bitmap)
    private sealed class Item(val height: Float) {
        class TextItem(val layout: StaticLayout, height: Float) : Item(height)
        class ImageItem(val bitmap: Bitmap, height: Float) : Item(height)
    }

    private fun bodyPaint() = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 30f }
    private fun titlePaint() = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 44f; isFakeBoldText = true }
    private fun captionPaint() = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 24f }
    private fun accentPaint() = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1A5276"); textSize = 28f }

    // 🔧 20-Jul-2026: PUBLIC entry — returns the written file (in filesDir/exports), or null
    fun export(context: Context, note: Note, format: ExportFormat): File? {
        val blocks = NoteExportBuilder.build(note)
// 🔧 30-Jul-2026 02:10 Phase 1 — folder + name policy moved to PathPolicy.exportPath.
        //   Same rule as before: "exports/<safeTitle>-<timestamp><ext>".
        val destination = PathPolicy.exportPath(note.title, format, getCurrentTimestamp())
        val dir = File(context.filesDir, destination.folder).apply { mkdirs() }
        val file = File(dir, destination.fileName)
        return try {
            when (format) {
                ExportFormat.PDF -> exportPdf(blocks, file)
                ExportFormat.IMAGE -> exportImage(blocks, file)
                ExportFormat.DOCX -> exportDocx(blocks, file)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace(); null
        }
    }

    // ---- DOCX: all logic is SHARED; here we only base64 the images + write bytes ----
    private fun exportDocx(blocks: List<ExportBlock>, file: File) {
        val images = NoteExportBuilder.imagePaths(blocks).mapNotNull { path ->
            val f = File(path)
            if (!f.exists()) null
            else path to Base64.encodeToString(f.readBytes(), Base64.NO_WRAP)
        }.toMap()
        val bytes = DocxExporter.export(blocks, images)
        FileOutputStream(file).use { it.write(bytes) }
    }

    // ---- shared layout pass used by BOTH pdf + image ----
    private fun buildItems(blocks: List<ExportBlock>, contentWidth: Int): List<Item> {
        val items = mutableListOf<Item>()
        for (block in blocks) {
            when (block.kind) {
                ExportBlockKind.TITLE -> items.add(textItem(block.text, titlePaint(), contentWidth))
                ExportBlockKind.PARAGRAPH -> items.add(textItem(block.text, bodyPaint(), contentWidth))
                ExportBlockKind.IMAGE -> {
                    val bmp = decodeScaled(block.path, contentWidth)
                    if (bmp != null) {
                        items.add(Item.ImageItem(bmp, bmp.height.toFloat()))
                        if (block.caption.isNotBlank()) items.add(textItem(block.caption, captionPaint(), contentWidth))
                    } else items.add(textItem("🖼 ${block.caption.ifBlank { "Image" }} (not available)", bodyPaint(), contentWidth))
                }
                ExportBlockKind.FILE -> items.add(textItem("📎 ${block.name}  (${block.typeLabel})", accentPaint(), contentWidth))
                ExportBlockKind.LINK -> items.add(textItem("🔗 ${block.url}", accentPaint(), contentWidth))
                ExportBlockKind.LOCATION -> items.add(textItem("📍 ${block.label}", accentPaint(), contentWidth))
            }
        }
        return items
    }

    private fun textItem(text: String, paint: TextPaint, width: Int): Item.TextItem {
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        return Item.TextItem(layout, layout.height.toFloat())
    }

    // ---- PDF: paginate items across A4 pages ----
    private fun exportPdf(blocks: List<ExportBlock>, file: File) {
        val contentWidth = (PDF_WIDTH - 2 * MARGIN).toInt()
        val items = buildItems(blocks, contentWidth)
        val document = PdfDocument()
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PDF_WIDTH, PDF_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = MARGIN
        val bottom = PDF_HEIGHT - MARGIN
        for (item in items) {
            // start a new page when the item won't fit (tall images just start fresh)
            if (y + item.height > bottom && y > MARGIN) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PDF_WIDTH, PDF_HEIGHT, pageNumber).create())
                canvas = page.canvas
                y = MARGIN
            }
            drawItem(canvas, item, MARGIN, y)
            y += item.height + BLOCK_GAP
        }
        document.finishPage(page)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
    }

    // ---- IMAGE: one tall PNG ----
    private fun exportImage(blocks: List<ExportBlock>, file: File) {
        val contentWidth = (IMAGE_WIDTH - 2 * MARGIN).toInt()
        val items = buildItems(blocks, contentWidth)
        val totalHeight = (2 * MARGIN + items.sumOf { (it.height + BLOCK_GAP).toDouble() }).toInt().coerceAtLeast(IMAGE_WIDTH)
        val bitmap = Bitmap.createBitmap(IMAGE_WIDTH, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        var y = MARGIN
        for (item in items) {
            drawItem(canvas, item, MARGIN, y)
            y += item.height + BLOCK_GAP
        }
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private fun drawItem(canvas: Canvas, item: Item, x: Float, y: Float) {
        when (item) {
            is Item.TextItem -> {
                canvas.save(); canvas.translate(x, y); item.layout.draw(canvas); canvas.restore()
            }
            is Item.ImageItem -> canvas.drawBitmap(item.bitmap, x, y, null)
        }
    }

    // 🔧 20-Jul-2026: decode an image down to the content width (never upscales)
    private fun decodeScaled(path: String, targetWidth: Int): Bitmap? {
        val file = File(path)
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= targetWidth) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = BitmapFactory.decodeFile(path, opts) ?: return null
        if (decoded.width <= targetWidth) return decoded
        val scale = targetWidth.toFloat() / decoded.width
        return Bitmap.createScaledBitmap(decoded, targetWidth, (decoded.height * scale).toInt().coerceAtLeast(1), true)
    }
}
