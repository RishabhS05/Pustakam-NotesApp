package com.app.pustakam.data.localdb.database

import app.cash.sqldelight.db.SqlDriver
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.data.models.response.notes.Notes
import com.app.pustakam.database.NoteContent
import com.app.pustakam.database.NotesDatabase
import com.app.pustakam.util.ContentType
import com.app.pustakam.util.log_d
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch




class NotesDao(private val sharedDb: SqlDriver) {
    private val database = NotesDatabase(sharedDb)
    private val queries = database.notesDatabaseQueries
  suspend fun selectAllNotesFromDb(): Notes {
      val rows  =  queries.selectWithAllContent().executeAsList()
        val notes: List<Note> = rows.groupBy { it.noteId }.map{(noteId, noteRows)->
            val noteMetadata = noteRows.first()
            val contents = noteRows.filter { it.contentId != null }.map { content ->
                when (content.type) {
                    ContentType.TEXT.name ->
                        NoteContentModel.Text(
                            id = content.contentId!!,
                            noteId = content.noteId!!,
                            text = content.text!!,
                            position = content.position!!,
                            createdAt = content.createdAt,
                            updatedAt = content.updatedAt
                        )

                    ContentType.IMAGE.name -> NoteContentModel.Image(
                        id = content.contentId!!,
                        noteId = content.noteId!!,
                        url = content.url!!,
                        position = content.position!!,
                        createdAt = content.createdAt,
                        updatedAt = content.updatedAt,
                        localPath = content.localPath
                    )

                    ContentType.VIDEO.name -> NoteContentModel.Video(
                        id = content.contentId!!,
                        noteId = content.noteId!!,
                        url = content.url!!,
                        localPath = content.localPath,
                        position = content.position!!,
                        createdAt = content.createdAt,
                        updatedAt = content.updatedAt,
                        duration = content.duration!!,
                    )

                    ContentType.AUDIO.name -> NoteContentModel.Audio(
                        id =content.contentId!!,
                        noteId = content.noteId!!,
                        url = content.url!!,
                        localPath = content.localPath,
                        position = content.position!!,
                        createdAt = content.createdAt,
                        updatedAt = content.updatedAt,
                        duration = content.duration!!
                    )

                    ContentType.DOCX.name -> NoteContentModel.Doc(
                        id = content.contentId!!,
                        noteId = content.noteId!!,
                        url = content.url!!,
                        localPath = content.localPath,
                        position = content.position!!,
                        createdAt = content.createdAt,
                        updatedAt = content.updatedAt,
                    )

                    ContentType.LINK.name -> NoteContentModel.Link(
                        url = content.url!!,
                        id = content.contentId!!,
                        noteId = content.noteId!!,
                        position = content.position!!,
                        createdAt = content.createdAt,
                        updatedAt = content.updatedAt,
                    )

                    ContentType.LOCATION.name -> NoteContentModel.Location(
                        latitude = content.lat!!,
                        longitude = content.long!!,
                        address = content.address,
                        position = content.position!!,
                        id = content.contentId!!,
                        noteId = content.noteId!!,
                        createdAt = content.createdAt,
                        updatedAt = content.updatedAt,
                    )

                    else -> throw IllegalArgumentException("Unknown content type: ${content.type}")
                }
            }
            Note(
                _id = noteId,
                title = noteMetadata.title ?: "",
                createdAt = noteMetadata.createdAt,
                updatedAt = noteMetadata.updatedAt,
                categoryId = noteMetadata.categoryId,
                content = contents
            )

        }
        val arrayListNotes: ArrayList<Note> = if (notes.isNotEmpty()) notes as ArrayList<Note> else ArrayList()
        arrayListNotes.forEach {
            log_d("Notes ", it)
        }
        return Notes(
            notes = arrayListNotes,
            count = notes.size, page = 0
        )
    }

   suspend fun insertNotes(notes: Notes) {
        database.transaction {
            notes.notes?.forEach{ note ->
                CoroutineScope(Dispatchers.IO).launch {
                    insertOrUpdateNoteFromDb(note)
                }
            }.apply {
                log_d("Note ", this.toString())
            }
        }
   }
  private suspend fun insertOrUpdateNotesContent (noteContent: NoteContentModel) {
        var url = ""
        var text = ""
        var address: String? = null
        var duration : Long? = null
        var localPath: String? = null
        var long: Double? = null
        var lat: Double? = null
        when (noteContent.type) {

            ContentType.TEXT -> {
             text = (noteContent as? NoteContentModel.Text)?.text ?: ""
            }
            ContentType.AUDIO -> {
               val content =  (noteContent as? NoteContentModel.Audio)
                if (content != null) {
                    url = content.url
                    localPath = content.localPath
                    duration = content.duration
                }
            }
            ContentType.VIDEO -> {
               val content =  (noteContent as? NoteContentModel.Video)
                if (content != null) {
                    url = content.url
                    localPath = content.localPath
                    duration = content.duration
                }
            }
           ContentType.LOCATION->{
              val content =  (noteContent as? NoteContentModel.Location)
               if (content != null) {
                   address = content.address
                   lat = content.latitude
                   long = content.longitude
               }
           }
           ContentType.IMAGE -> {
             val content  =  (noteContent as? NoteContentModel.Image)
               if (content != null) {
                   url = content.url
                   localPath = content.localPath
               }
           }
           ContentType.LINK -> {
              val content =  (noteContent as? NoteContentModel.Link)
               if (content != null) {
                   url = content.url
               }
           }
            ContentType.DOCX -> {
             val content =   (noteContent as? NoteContentModel.Doc)
                if (content != null) {
                    url = content.url
                    localPath = content.localPath
                }
            }
            else -> {}
        }
      val insertedRow  = queries.insertNoteContentById(
            id = noteContent.id,
            noteId = noteContent.noteId,
            createdAt = noteContent.createdAt,
            updatedAt = noteContent.updatedAt,
            position = noteContent.position,
            type = noteContent.type.name,
            text = text,
            duration = duration,
            url = url,
            localPath = localPath,
            long = long,
            lat = lat,
            address = address,
        )

    }

    suspend fun deleteByIdFromDb(id: String) : Boolean {
        queries.deleteById(id)
        val note  = selectNoteById(id)
        return note==null
    }

   suspend fun insertOrUpdateNoteFromDb(note: Note) : Note {
        log_d("insert", note)
        queries.insertOrUpdateNote(
            id = note._id!!,
            title = note.title,
            updatedAt = note.updatedAt,
            createdAt = note.createdAt,
            categoryId = note.categoryId,
        )
           database.transaction {
               note.content?.forEach { content ->
                   CoroutineScope(Dispatchers.IO).launch {
                       insertOrUpdateNotesContent(content)
                   }
               }.apply {
                   log_d("Note Content ", this.toString())
               }
           }
       return note
    }
    suspend fun selectNoteById(id: String): Note? {
        val noteRow = queries.selectById(id).executeAsOneOrNull() ?: return null
        val contents = queries.selectAllNotesContent(id).executeAsList()
        return noteMapper(noteRow, contents)
    }
    private fun noteMapper(
        note: com.app.pustakam.database.Notes,
        contentRows: List<NoteContent>?
    ): Note {
        val contents = contentRows?.map { content ->
            when (content.type) {
                ContentType.TEXT.name ->
                    NoteContentModel.Text(
                        id = content.id,
                        noteId = content.noteId,
                        text = content.text!!,
                        position = content.position!!,
                        createdAt = content.createdAt,
                        updatedAt = content.updatedAt
                    )

                ContentType.IMAGE.name -> NoteContentModel.Image(
                    id = content.id,
                    noteId = content.noteId,
                    url = content.url!!,
                    position = content.position!!,
                    createdAt = content.createdAt,
                    updatedAt = content.updatedAt,
                    localPath = content.localPath
                )

                ContentType.VIDEO.name -> NoteContentModel.Video(
                    id = content.id,
                    noteId = content.noteId,
                    url = content.url!!,
                    localPath = content.localPath,
                    position = content.position!!,
                    createdAt = content.createdAt,
                    updatedAt = content.updatedAt,
                    duration = content.duration!!,
                )

                ContentType.AUDIO.name -> NoteContentModel.Audio(
                    id = content.id,
                    noteId = content.noteId,
                    url = content.url!!,
                    localPath = content.localPath,
                    position = content.position!!,
                    createdAt = content.createdAt,
                    updatedAt = content.updatedAt,
                    duration = content.duration!!
                )

                ContentType.DOCX.name -> NoteContentModel.Doc(
                    id = content.id,
                    noteId = content.noteId,
                    url = content.url!!,
                    localPath = content.localPath,
                    position = content.position!!,
                    createdAt = content.createdAt,
                    updatedAt = content.updatedAt,
                )

                ContentType.LINK.name -> NoteContentModel.Link(
                    url = content.url!!,
                    id = content.id,
                    noteId = content.noteId,
                    position = content.position!!,
                    createdAt = content.createdAt,
                    updatedAt = content.updatedAt,
                )

                ContentType.LOCATION.name -> NoteContentModel.Location(
                    latitude = content.lat!!,
                    longitude = content.long!!,
                    address = content.address,
                    position = content.position!!,
                    id = content.id,
                    noteId = content.noteId,
                    createdAt = content.createdAt,
                    updatedAt = content.updatedAt,
                )
                else -> throw IllegalArgumentException("Unknown content type: ${content.type}")
            }
        }

        return Note(
            _id = note.id,
            title = note.title,
            createdAt = note.createdAt,
            updatedAt = note.updatedAt,
            categoryId = note.categoryId,
            content = contents ?: emptyList()
        )
    }
}

