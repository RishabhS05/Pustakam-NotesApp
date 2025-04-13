package com.app.pustakam.android.noteContentProvider

import android.app.Activity
import android.content.Context
import com.app.pustakam.android.fileUtils.createFileWithFolders
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.data.models.response.notes.NoteContentObjectHelper
import com.app.pustakam.util.ContentType
import com.app.pustakam.util.ContentType.*
import com.app.pustakam.util.getCurrentTimestamp

//It decides and return a noteContent
//also provide create filepath and add if required

/**
 * Factory method  of note content */
fun addContent(context: Context,note : Note, contentType : ContentType) : NoteContentModel{
    val position: Long = note.contents?.count()?.toLong() ?: 0
    val noteId = note.id?: ""
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
        GIF, PDF, AUDIO , DOCX, VIDEO, IMAGE -> {
            val timeStamp = getCurrentTimestamp()
            val folderName = "${contentType.name.lowercase()}/${timeStamp}"
            val fileName = "${timeStamp}${contentType.getExt()}"
            val filePath = createFileWithFolders(context as Activity,folderName,fileName).absolutePath
            content = NoteContentObjectHelper.createMedia(positionedAt = position, timestamp = timeStamp.toString(),
                noteId = noteId, localPath = filePath ,  contentType = contentType).copy( title = "$contentType-$position",)
        }
    }
    return content
}