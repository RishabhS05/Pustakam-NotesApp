package com.app.pustakam.data.localdb.database

import app.cash.sqldelight.db.SqlDriver
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.data.models.response.notes.Notes
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
  suspend fun selectAllNotesFromDb(limit : Int = 10, page : Int = 0): Notes {
//      val offset = (page - 1) * limit
//      limit.toLong(), offset.toLong()
      val notesWithContent = arrayListOf<Note>()
      val results  =  queries.selectWithAllContent().executeAsList()
      val grouped = results.groupBy { it.noteId }
      grouped.forEach { (_, rows) ->
          val note = rows.first()
          notesWithContent.add(
              Note(
                  id = note.noteId,
                  categoryId = note.categoryId,
                  title = note.title,
                  createdAt = note.noteCreatedAt,
                  updatedAt = note.noteUpdatedAt,
                  content = rows.mapNotNull { row ->
                      if (row.contentId != null) {
                          when (row.type) {
                              ContentType.TEXT.name ->
                                  NoteContentModel.TextContent(
                                      id = row.contentId,
                                      noteId = row.noteId,
                                      text =  row.text!!,
                                      position = row.position!!,
                                      createdAt = row.contentCreatedAt,
                                      updatedAt = row.contentUpdatedAt,
                                  )

                              ContentType.IMAGE.name -> NoteContentModel.ImageContent(
                                  id = row.contentId,
                                  noteId = row.noteId,
                                  url = row.url!!,
                                  position = row.position!!,
                                  createdAt = row.contentCreatedAt,
                                  updatedAt = row.contentUpdatedAt,
                                  localPath = row.localPath
                              )

                              ContentType.VIDEO.name -> NoteContentModel.VideoContent(
                                  id = row.contentId,
                                  noteId = row.noteId,
                                  url = row.url!!,
                                  localPath = row.localPath,
                                  position = row.position!!,
                                  createdAt = row.contentCreatedAt,
                                  updatedAt = row.contentUpdatedAt,
                                  duration = row.duration!!,
                              )

                              ContentType.AUDIO.name -> NoteContentModel.AudioContent(
                                  id  = row.contentId,
                                  noteId = row.noteId,
                                  url = row.url!!,
                                  localPath = row.localPath,
                                  position = row.position!!,
                                  createdAt = row.contentCreatedAt,
                                  updatedAt = row.contentUpdatedAt,
                                  duration = row.duration!!
                              )

                              ContentType.DOCX.name -> NoteContentModel.DocContent(
                                  id = row.contentId,
                                  noteId = row.noteId,
                                  url = row.url!!,
                                  localPath = row.localPath,
                                  position = row.position!!,
                                  createdAt = row.contentCreatedAt,
                                  updatedAt = row.contentUpdatedAt,
                              )

                              ContentType.LINK.name -> NoteContentModel.Link(
                                  url = row.url!!,
                                  id = row.contentId,
                                  noteId = row.noteId,
                                  position = row.position!!,
                                  createdAt = row.contentCreatedAt,
                                  updatedAt = row.contentUpdatedAt,
                              )

                              ContentType.LOCATION.name -> NoteContentModel.Location(
                                  latitude = row.lat!!,
                                  longitude = row.long!!,
                                  address = row.address,
                                  position = row.position!!,
                                  id = row.contentId,
                                  noteId = row.noteId,
                                  createdAt = row.contentCreatedAt,
                                  updatedAt = row.contentUpdatedAt,
                              )
                              else -> null
                          }
                      } else null
                  }.toMutableList()
              )
          )
      }

        return Notes(
            notes = notesWithContent,
            count = notesWithContent.size, page = 0
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
             text = (noteContent as? NoteContentModel.TextContent)?.text ?: ""
            }
            ContentType.AUDIO -> {
               val content =  (noteContent as? NoteContentModel.AudioContent)
                if (content != null) {
                    url = content.url
                    localPath = content.localPath
                    duration = content.duration
                }
            }
            ContentType.VIDEO -> {
               val content =  (noteContent as? NoteContentModel.VideoContent)
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
             val content  =  (noteContent as? NoteContentModel.ImageContent)
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
             val content =   (noteContent as? NoteContentModel.DocContent)
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
        return note== null
    }

   suspend fun insertOrUpdateNoteFromDb(note: Note) : Note {
        log_d("insert", note)
        queries.insertOrUpdateNote(
            id = note.id!!,
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
        val rows = queries.selectById(id).executeAsList()
        val noteWithContent = rows.firstOrNull()?.let { note ->
            Note(
                id = note.noteId,
                categoryId = note.categoryId,
                title = note.title,
                createdAt = note.noteCreatedAt,
                updatedAt = note.noteUpdatedAt,
                content = rows.mapNotNull { row ->
                    if (row.contentId != null) {
                        when (row.type) {
                            ContentType.TEXT.name ->
                                NoteContentModel.TextContent(
                                    id = row.contentId,
                                    noteId = row.noteId,
                                    text =  row.text!!,
                                    position = row.position!!,
                                    createdAt = row.contentCreatedAt,
                                    updatedAt = row.contentUpdatedAt,
                                )

                            ContentType.IMAGE.name -> NoteContentModel.ImageContent(
                                id = row.contentId,
                                noteId = row.noteId,
                                url = row.url!!,
                                position = row.position!!,
                                createdAt = row.contentCreatedAt,
                                updatedAt = row.contentUpdatedAt,
                                localPath = row.localPath
                            )

                            ContentType.VIDEO.name -> NoteContentModel.VideoContent(
                                id = row.contentId,
                                noteId = row.noteId,
                                url = row.url!!,
                                localPath = row.localPath,
                                position = row.position!!,
                                createdAt = row.contentCreatedAt,
                                updatedAt = row.contentUpdatedAt,
                                duration = row.duration!!,
                            )

                            ContentType.AUDIO.name -> NoteContentModel.AudioContent(
                                id  = row.contentId,
                                noteId = row.noteId,
                                url = row.url!!,
                                localPath = row.localPath,
                                position = row.position!!,
                                createdAt = row.contentCreatedAt,
                                updatedAt = row.contentUpdatedAt,
                                duration = row.duration!!
                            )

                            ContentType.DOCX.name -> NoteContentModel.DocContent(
                                id = row.contentId,
                                noteId = row.noteId,
                                url = row.url!!,
                                localPath = row.localPath,
                                position = row.position!!,
                                createdAt = row.contentCreatedAt,
                                updatedAt = row.contentUpdatedAt,
                            )

                            ContentType.LINK.name -> NoteContentModel.Link(
                                url = row.url!!,
                                id = row.contentId,
                                noteId = row.noteId,
                                position = row.position!!,
                                createdAt = row.contentCreatedAt,
                                updatedAt = row.contentUpdatedAt,
                            )

                            ContentType.LOCATION.name -> NoteContentModel.Location(
                                latitude = row.lat!!,
                                longitude = row.long!!,
                                address = row.address,
                                position = row.position!!,
                                id = row.contentId,
                                noteId = row.noteId,
                                createdAt = row.contentCreatedAt,
                                updatedAt = row.contentUpdatedAt,
                            )
                            else -> null
                        }
                    } else null
                }.toMutableList()
            )
        }
        return noteWithContent
    }
}

