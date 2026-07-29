package com.app.pustakam.export

// 🔧 20-Jul-2026: NEW (export feature) — builds a COMPLETE, valid .docx as a ByteArray in shared
//   code. Both platforms call this and just write the bytes; images are passed as base64 so no
//   ByteArray crosses the Kotlin/Swift boundary. Text + embedded images; other files → a
//   reference line (a pdf can't be embedded as a Word picture).
object DocxExporter {

    private const val EMU_PER_PX = 9525L          // 1 px = 9525 EMU
    private const val MAX_IMAGE_WIDTH_EMU = 5486400L  // ~6 inches page content width
    private const val DEFAULT_IMG_W_PX = 480
    private const val DEFAULT_IMG_H_PX = 360

    private class ImagePart(val rId: String, val fileName: String, val ext: String, val bytes: ByteArray, val wEmu: Long, val hEmu: Long)

    // 🔧 20-Jul-2026: blocks + (path → base64 image bytes) → .docx bytes
    fun export(blocks: List<ExportBlock>, imagesBase64: Map<String, String>): ByteArray {
        val imageParts = mutableMapOf<String, ImagePart>()  // keyed by path
        var imgIndex = 0

        // pre-resolve every embeddable image once
        blocks.filter { it.kind == ExportBlockKind.IMAGE }.forEach { img ->
            if (img.path.isBlank() || imageParts.containsKey(img.path)) return@forEach
            val base64 = imagesBase64[img.path] ?: return@forEach
            val bytes = Base64Codec.decode(base64)
            if (bytes.isEmpty()) return@forEach
            imgIndex++
            val ext = extensionFor(img.path)
            val (wEmu, hEmu) = scaledEmu(img.width, img.height)
            imageParts[img.path] = ImagePart("rIdImg$imgIndex", "image$imgIndex.$ext", ext, bytes, wEmu, hEmu)
        }

        val documentXml = buildDocumentXml(blocks, imageParts)
        val relsXml = buildDocumentRels(imageParts.values.toList())
        val contentTypesXml = buildContentTypes(imageParts.values.toList())

        val entries = mutableListOf(
            MiniZip.Entry("[Content_Types].xml", contentTypesXml.encodeToByteArray()),
            MiniZip.Entry("_rels/.rels", ROOT_RELS.encodeToByteArray()),
            MiniZip.Entry("word/document.xml", documentXml.encodeToByteArray()),
            MiniZip.Entry("word/_rels/document.xml.rels", relsXml.encodeToByteArray()),
        )
        imageParts.values.forEach { part ->
            entries.add(MiniZip.Entry("word/media/${part.fileName}", part.bytes))
        }
        return MiniZip.zip(entries)
    }

    // ---- document.xml ----------------------------------------------------

    private fun buildDocumentXml(blocks: List<ExportBlock>, imageParts: Map<String, ImagePart>): String {
        val body = StringBuilder()
        for (block in blocks) {
            when (block.kind) {
                ExportBlockKind.TITLE -> body.append(titleParagraph(block.text))
                ExportBlockKind.PARAGRAPH -> body.append(textParagraph(block.text))
                ExportBlockKind.IMAGE -> {
                    val part = imageParts[block.path]
                    if (part != null) {
                        body.append(imageParagraph(part))
                        if (block.caption.isNotBlank()) body.append(captionParagraph(block.caption))
                    } else {
                        body.append(textParagraph("🖼 ${block.caption.ifBlank { "Image" }} (not available)"))
                    }
                }
                ExportBlockKind.FILE -> body.append(textParagraph("📎 ${block.name}  (${block.typeLabel})"))
                ExportBlockKind.LINK -> body.append(textParagraph("🔗 ${block.url}"))
                ExportBlockKind.LOCATION -> body.append(textParagraph("📍 ${block.label}"))
            }
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
<w:body>$body<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/></w:sectPr></w:body>
</w:document>"""
    }

    private fun titleParagraph(text: String): String =
        "<w:p><w:pPr><w:jc w:val=\"center\"/></w:pPr><w:r><w:rPr><w:b/><w:sz w:val=\"48\"/></w:rPr><w:t xml:space=\"preserve\">${escape(text)}</w:t></w:r></w:p>"

    // 🔧 20-Jul-2026: newlines inside a text block become <w:br/> within one paragraph
    private fun textParagraph(text: String): String {
        val runs = text.split("\n").joinToString("<w:br/>") {
            "<w:t xml:space=\"preserve\">${escape(it)}</w:t>"
        }
        return "<w:p><w:r>$runs</w:r></w:p>"
    }

    private fun captionParagraph(text: String): String =
        "<w:p><w:pPr><w:jc w:val=\"center\"/></w:pPr><w:r><w:rPr><w:i/><w:sz w:val=\"18\"/></w:rPr><w:t xml:space=\"preserve\">${escape(text)}</w:t></w:r></w:p>"

    private fun imageParagraph(part: ImagePart): String =
        "<w:p><w:pPr><w:jc w:val=\"center\"/></w:pPr><w:r><w:drawing>" +
            "<wp:inline distT=\"0\" distB=\"0\" distL=\"0\" distR=\"0\">" +
            "<wp:extent cx=\"${part.wEmu}\" cy=\"${part.hEmu}\"/>" +
            "<wp:docPr id=\"${part.rId.removePrefix("rIdImg")}\" name=\"${part.fileName}\"/>" +
            "<a:graphic><a:graphicData uri=\"http://schemas.openxmlformats.org/drawingml/2006/picture\">" +
            "<pic:pic><pic:nvPicPr><pic:cNvPr id=\"0\" name=\"${part.fileName}\"/><pic:cNvPicPr/></pic:nvPicPr>" +
            "<pic:blipFill><a:blip r:embed=\"${part.rId}\"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>" +
            "<pic:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"${part.wEmu}\" cy=\"${part.hEmu}\"/></a:xfrm>" +
            "<a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></pic:spPr>" +
            "</pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>"

    // ---- other parts -----------------------------------------------------

    private fun buildDocumentRels(images: List<ImagePart>): String {
        val rels = images.joinToString("") { part ->
            "<Relationship Id=\"${part.rId}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"media/${part.fileName}\"/>"
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">$rels</Relationships>"""
    }

    private fun buildContentTypes(images: List<ImagePart>): String {
        val exts = images.map { it.ext.lowercase() }.toSet()
        val defaults = StringBuilder(
            "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
        )
        if (exts.contains("png")) defaults.append("<Default Extension=\"png\" ContentType=\"image/png\"/>")
        if (exts.contains("jpg") || exts.contains("jpeg")) {
            defaults.append("<Default Extension=\"jpg\" ContentType=\"image/jpeg\"/>")
            defaults.append("<Default Extension=\"jpeg\" ContentType=\"image/jpeg\"/>")
        }
        if (exts.contains("gif")) defaults.append("<Default Extension=\"gif\" ContentType=\"image/gif\"/>")
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">$defaults<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>"""
    }

    private val ROOT_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>"""

    // ---- helpers ---------------------------------------------------------

    private fun scaledEmu(wPx: Int, hPx: Int): Pair<Long, Long> {
        val w = if (wPx > 0) wPx else DEFAULT_IMG_W_PX
        val h = if (hPx > 0) hPx else DEFAULT_IMG_H_PX
        var cx = w * EMU_PER_PX
        var cy = h * EMU_PER_PX
        if (cx > MAX_IMAGE_WIDTH_EMU) {
            cy = cy * MAX_IMAGE_WIDTH_EMU / cx
            cx = MAX_IMAGE_WIDTH_EMU
        }
        return cx to cy
    }

    private fun extensionFor(path: String): String {
        val ext = path.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "jpg", "jpeg" -> "jpg"
            "gif" -> "gif"
            else -> "png"
        }
    }

    private fun escape(text: String): String = buildString {
        for (ch in text) when (ch) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&apos;")
            '\t' -> append("    ")
            else -> if (ch.code >= 0x20 || ch == '\n') append(ch)
        }
    }
}
