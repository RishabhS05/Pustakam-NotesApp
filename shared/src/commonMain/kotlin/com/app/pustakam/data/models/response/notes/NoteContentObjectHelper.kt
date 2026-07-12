package com.app.pustakam.data.models.response.notes
import com.app.pustakam.util.ContentType
import com.app.pustakam.util.UniqueIdGenerator
import com.app.pustakam.util.getCurrentTimestamp

// 🔧 C6: this factory is the ONLY place ids/timestamps are generated.
//       id logic UNCHANGED: timestamp+UUID via UniqueIdGenerator — created once, survives sync round-trips.
//       Timestamps stay String (platform formats may differ). 🔧 C4: positions are Double.
object NoteContentObjectHelper {

    fun createText(noteId: String, positionedAt: Double, text: String = ""): NoteContentModel.TextContent {
        val now = "${getCurrentTimestamp()}"
        return NoteContentModel.TextContent(
            id = UniqueIdGenerator.generateUniqueId(),
            noteId = noteId,
            position = positionedAt,
            createdAt = now,
            updatedAt = now,
            text = text                       // 🔧 was silently ignored before (Piece-5 bug fixed)
        )
    }

    fun createMedia(
        contentType: ContentType, noteId: String, positionedAt: Double,
        localPath: String = "", url: String = "", duration: Long = 0,
        timestamp: String? = null,            // null/empty = "now" (original semantics kept)
        title: String = "",                   // 🔧 C1: persisted per-item title
        mimeType: String = "",                // 🔧 C1: media metadata passthrough
        sizeBytes: Long = 0, width: Int = 0, height: Int = 0,
        thumbnailPath: String? = null,
    ): NoteContentModel.MediaContent {
        val time = if (timestamp.isNullOrEmpty()) "${getCurrentTimestamp()}" else timestamp
        return NoteContentModel.MediaContent(
            id = UniqueIdGenerator.generateUniqueId(),
            type = contentType,
            position = positionedAt,
            duration = duration,
            createdAt = time,
            updatedAt = time,
            noteId = noteId,
            localPath = localPath,
            url = url,
            title = title,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            width = width,
            height = height,
            thumbnailPath = thumbnailPath,
        )
    }

    fun createHyperLink(link: String = "", noteId: String, positionedAt: Double): NoteContentModel.Link {
        val now = "${getCurrentTimestamp()}"
        return NoteContentModel.Link(
            id = UniqueIdGenerator.generateUniqueId(),
            position = positionedAt,
            noteId = noteId,
            url = link,
            createdAt = now,
            updatedAt = now,
        )
    }

    fun createLocation(noteId: String, positionedAt: Double, lat: Double = 0.0, long: Double = 0.0): NoteContentModel.Location {
        val now = "${getCurrentTimestamp()}"
        return NoteContentModel.Location(
            id = UniqueIdGenerator.generateUniqueId(),
            noteId = noteId,
            position = positionedAt,
            latitude = lat,
            longitude = long,
            createdAt = now,
            updatedAt = now,
        )
    }
}
