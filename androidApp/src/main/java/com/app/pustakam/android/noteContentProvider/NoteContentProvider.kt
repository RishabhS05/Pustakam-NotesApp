package com.app.pustakam.android.noteContentProvider

import android.app.Activity
import android.content.Context
import com.app.pustakam.android.fileUtils.createFileWithFolders
import com.app.pustakam.core.model.models.response.notes.Note
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import com.app.pustakam.core.model.models.response.notes.NoteContentObjectHelper
import com.app.pustakam.core.common.util.ContentType
import com.app.pustakam.core.common.util.ContentType.*
import com.app.pustakam.core.common.util.getCurrentTimestamp
import com.app.pustakam.core.filesys.path.PathPolicy

//It decides and return a noteContent
//also provide create filepath and add if required

/**
 * Factory method  of note content */
fun addContent(context: Context,note : Note, contentType : ContentType) : NoteContentModel{
    // 🔧 C4: position is Double (fractional ordering)
    val position: Double = note.contents.count().toDouble()
    val noteId = note.id
    val content: NoteContentModel
    when (contentType) {
        TEXT -> {
            content = NoteContentObjectHelper.createText(positionedAt = position,
                noteId = noteId)
        }
        LINK -> {
            content = NoteContentObjectHelper.createHyperLink(positionedAt = position,
                noteId = noteId,)
        }

        LOCATION -> {
            content = NoteContentObjectHelper.createLocation(positionedAt = position,
                noteId = noteId)
        }
        // 🔧 18-Jul-2026: file-import types — same media path (keeps `val content` exhaustive)
        GIF, PDF, AUDIO , DOCX, VIDEO, IMAGE, TXT, MD, EPUB, OTHER -> {
            val timeStamp = getCurrentTimestamp()
            val destination = PathPolicy.capturePath(contentType, noteId, timeStamp)
            val filePath = createFileWithFolders(
                context as Activity, destination.folder, destination.fileName
            ).absolutePath
            content = NoteContentObjectHelper.createMedia(positionedAt = position, timestamp = timeStamp.toString(),
                noteId = noteId, localPath = filePath ,  contentType = contentType).copy( title = "$contentType-$position",)
        }
        else ->   content = NoteContentObjectHelper.createText(positionedAt = position,
            noteId = noteId)
    }
    return content
}