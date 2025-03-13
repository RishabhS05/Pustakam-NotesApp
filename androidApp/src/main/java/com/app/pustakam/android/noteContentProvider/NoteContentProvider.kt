package com.app.pustakam.android.noteContentProvider

import android.app.Activity
import android.content.Context
import com.app.pustakam.android.fileUtils.createFileWithFolders
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.util.ContentType
import com.app.pustakam.util.ContentType.*
import com.app.pustakam.util.getCurrentTimestamp

//It decides and return a noteContent
//also provide create filepath and add if required
fun addContent(context: Context,note : Note, contentType : ContentType, ) : NoteContentModel{
    val position: Long = note.content?.count()?.toLong() ?: 0
    val noteId = note.id?: ""
    val content: NoteContentModel
    when (contentType) {
        TEXT -> {
            content = NoteContentModel.TextContent(position = position,
                noteId = noteId,)
        }
        LINK -> {
            content = NoteContentModel.Link(position = position,
                noteId = noteId, )
        }

        LOCATION -> {
            content = NoteContentModel.Location(position = position,
                noteId = noteId)
        }

        AUDIO , DOCX, VIDEO, IMAGE -> {
            val timeStamp = getCurrentTimestamp()
            val folderName = "${contentType.name.lowercase()}/${timeStamp}"
            val fileName = "${timeStamp}${contentType.getExt()}"
            val filePath = createFileWithFolders(context as Activity,folderName,fileName).absolutePath
            content = NoteContentModel.MediaContent(position = position,
                title = "$contentType-$position",
                noteId = noteId, localPath = filePath , type = contentType)
        }

        else -> {
            content = NoteContentModel.Location(position = position,
                noteId = noteId)
        }
    }
    return content
}