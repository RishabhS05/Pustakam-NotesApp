package com.app.pustakam.data.localdb.database

import app.cash.sqldelight.db.SqlDriver
import com.app.pustakam.data.models.Tag
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.data.models.response.notes.Notes
import com.app.pustakam.database.NotesDatabase
import com.app.pustakam.util.ContentType
import com.app.pustakam.util.log_d
// 🔧 F4: coroutine imports removed — DAO writes are synchronous inside transactions now
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject

class NotesDao() : KoinComponent {

    private val database =  get<NotesDatabase>()

    private val queries = database.notesDatabaseQueries

    fun createTagOnDB(tag : Tag) : Tag?   {
       println("Create Tab On DB called with tag: $tag") // Debug log
        // The transaction block is synchronous: it completes before proceeding.
        database.transaction {
            queries.createTag(id= tag.id, label = tag.label, color= tag.color)
        }
        // This will only execute after the transaction above is finished.
        return getTag(tag.id)
    }
      fun getTag(id : String) : Tag? =  queries.getTag(id).executeAsOneOrNull()?.let {
            Tag(id = it.id, label = it.label, color= it.color)
        }
    fun updateTagOnDB(tag: Tag) : Tag? {
      database.transaction {
          queries.updateTag(color = tag.color, label = tag.label, id = tag.id)
      }
        return getTag(tag.id)
    }
    fun deleteTag(tagId  : String) : Boolean {
        queries.deleteTag(tagId)
        val tag  = getTag(tagId)
        return tag == null
    }

    fun getTagsFromDB() : List<Tag> = queries.getTags().executeAsList().map{
          Tag(id = it.id, label = it.label, color= it.color)
    }
   // 🔧 15-Jul-2026 Phase 0.1: paging is OPT-IN — `limit > 0 && page > 0` fetches ONE page of note
   //   ids (indexed, keyset-cheap) and maps each via the existing selectNoteById mapper. Legacy
   //   callers (limit = 0, e.g. the iOS bridge) keep the original load-everything behavior, so
   //   nothing truncates for platforms that don't page yet.
   fun selectAllNotesFromDb(limit : Int = 0, page : Int = 0): Notes {
      if (limit > 0 && page > 0) {
          val pagedOffset = ((page - 1) * limit).coerceAtLeast(0)
          val ids = queries.selectNoteIdsPage(limit.toLong(), pagedOffset.toLong()).executeAsList()
          val pagedNotes = arrayListOf<Note>()
          ids.forEach { id -> selectNoteById(id)?.let { pagedNotes.add(it) } }
          return Notes(notes = pagedNotes, count = pagedNotes.size, page = page)
      }
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
                  contents = rows.mapNotNull { row ->
                      if (row.contentId != null&& !row.type.isNullOrEmpty()) {
                          val type = ContentType.valueOf(row.type)
                          when (type) {
                              ContentType.TEXT ->
                                  NoteContentModel.TextContent(
                                      id = row.contentId,
                                      noteId = row.noteId,
                                      text =  row.text!!,
                                      position = row.position!!,
                                      // 🔧 timestamps stay String (C3 reverted per review)
                                      createdAt = row.contentCreatedAt,
                                      updatedAt = row.contentUpdatedAt,
                                      metadata =  row.metaData
                                  )
                              // 🔧 C1: PDF + GIF now round-trip like every other media format
                              ContentType.IMAGE, ContentType.DOCX,  ContentType.VIDEO, ContentType.AUDIO, ContentType.PDF, ContentType.GIF  ->
                                  // 🔧 S1: contentTitle = media's OWN column (row.title is the NOTE's title from the join)
                                  NoteContentModel.MediaContent(title = row.contentTitle?:"${row.type}-${row.position}",
                                  id = row.contentId,
                                  noteId = row.noteId,
                                  url = row.url!!,
                                  position = row.position!!,
                                  createdAt = row.contentCreatedAt,
                                  updatedAt = row.contentUpdatedAt,
                                  localPath = row.localPath, duration = row.duration?:0,
                                  type =  type,
                                  // 🔧 C1: media metadata columns
                                  mimeType = row.mimeType ?: "",
                                  sizeBytes = row.sizeBytes ?: 0,
                                  width = row.width?.toInt() ?: 0,
                                  height = row.height?.toInt() ?: 0,
                                  thumbnailPath = row.thumbnailPath,
                              )

                              ContentType.LINK -> NoteContentModel.Link(
                                  url = row.url!!,
                                  id = row.contentId,
                                  noteId = row.noteId,
                                  position = row.position!!,
                                  createdAt = row.contentCreatedAt,
                                  updatedAt = row.contentUpdatedAt,
                              )

                              ContentType.LOCATION -> NoteContentModel.Location(
                                  latitude = row.lat!!,
                                  longitude = row.long!!,
                                  address = row.address,
                                  position = row.position!!,
                                  id = row.contentId,
                                  noteId = row.noteId,
                                  createdAt = row.contentCreatedAt,
                                  updatedAt = row.contentUpdatedAt,
                              )
                          }
                      } else null
                  }.toMutableList() as ArrayList<NoteContentModel>
              )
          )
      }

        return Notes(
            notes = notesWithContent,
            count = notesWithContent.size, page = 0
        )
    }

    fun insertNotes(notes: Notes) {
        // 🔧 F4: synchronous writes inside the transaction — the old fire-and-forget
        //       CoroutineScope(...).launch escaped the transaction entirely:
        //       no atomicity, races, and the transaction could commit before any write ran
        database.transaction {
            notes.notes.forEach { note ->
                insertOrUpdateNoteFromDb(note)
            }
        }
        log_d("Note inserted count ", notes.notes.count())
   }
  private  fun insertOrUpdateNotesContent (noteContent: NoteContentModel) {
        var url = ""
        var text = ""
        var address: String? = null
        var duration : Long? = null
        var localPath: String? = null
        var long: Double? = null
        var lat: Double? = null
        // 🔧 metadata was `val ... = null` — NEVER assigned, rich-text metadata was silently dropped
        var metadata : RichTextMetadata? = null
        // 🔧 C1: media metadata + per-item title now persisted (S1)
        var title: String? = null
        var mimeType: String? = null
        var sizeBytes: Long? = null
        var width: Long? = null
        var height: Long? = null
        var thumbnailPath: String? = null
        when (noteContent) {
            // 🔧 sealed-type when (was switching on ContentType with fragile as? casts per branch)
            is NoteContentModel.TextContent -> {
                text = noteContent.text
                metadata = noteContent.metadata
            }
            // 🔧 C1: ONE media branch for ALL formats (image/video/audio/docx/pdf/gif/…)
            is NoteContentModel.MediaContent -> {
                url = noteContent.url
                localPath = noteContent.localPath
                duration = noteContent.duration
                title = noteContent.title
                mimeType = noteContent.mimeType
                sizeBytes = noteContent.sizeBytes
                width = noteContent.width.toLong()
                height = noteContent.height.toLong()
                thumbnailPath = noteContent.thumbnailPath
            }
            is NoteContentModel.Location -> {
                address = noteContent.address
                lat = noteContent.latitude
                long = noteContent.longitude
            }
            is NoteContentModel.Link -> {
                url = noteContent.url
            }
        }
      queries.insertNoteContentById(
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
            title = title,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            width = width,
            height = height,
            thumbnailPath = thumbnailPath,
            metaData =  metadata
        )
    }
    fun  deleteNoteContentById(id : String)= queries.deleteNoteContentById(id)

    suspend fun deleteByIdFromDb(id: String) : Boolean {
        queries.deleteById(id)
        val note  = selectNoteById(id)
        return note == null
    }
    // 🔧 15-Jul-2026 Phase 0.4: dirty-row saves — `dirtyContentIds` limits the write to the content
    //   rows that actually changed (a 5,000-block note no longer rewrites 5,000 rows per save).
    //   null = legacy full write (iOS bridge and sync paths are unchanged). The note HEADER row is
    //   always written — it is one cheap row and carries title/updatedAt.
    fun insertOrUpdateNoteFromDb(note: Note, dirtyContentIds: Set<String>? = null) : Note {
        log_d("NoteDao insert", note)
        queries.insertOrUpdateNote(
            id = note.id,
            title = note.title,
            updatedAt = note.updatedAt,
            createdAt = note.createdAt,
            categoryId = note.categoryId,
        )
           // 🔧 F4: contents written synchronously INSIDE the transaction — the function
           //       previously returned before contents were saved (fire-and-forget launch)
           database.transaction {
               val contentsToWrite =
                   if (dirtyContentIds == null) note.contents
                   else note.contents.filter { it.id in dirtyContentIds }
               contentsToWrite.forEach { content ->
                   insertOrUpdateNotesContent(content)
               }
           }
       log_d("NoteDao end ", note)
       return note
    }
    fun selectNoteById(id: String): Note? {
        val rows = queries.selectById(id).executeAsList()
        val noteWithContent = rows.firstOrNull()?.let { note ->
            Note(
                id = note.noteId,
                categoryId = note.categoryId,
                title = note.title,
                createdAt = note.noteCreatedAt,
                updatedAt = note.noteUpdatedAt,
                contents = rows.mapNotNull { row ->
                    if (row.contentId != null&&!row.type.isNullOrEmpty()) {
                        val type = ContentType.valueOf(row.type)
                        when (type) {
                            ContentType.TEXT ->
                                NoteContentModel.TextContent(
                                    id = row.contentId,
                                    noteId = row.noteId,
                                    text =  row.text!!,
                                    position = row.position!!,
                                    createdAt = row.contentCreatedAt,
                                    updatedAt = row.contentUpdatedAt,
                                    // 🔧 metadata was dropped by this mapper (drifted from the list mapper)
                                    metadata = row.metaData,
                                )

                            // 🔧 C1: PDF + GIF included; title from contentTitle (S1) —
                            //       also fixes the drifted "${position}-${type}" fallback (list mapper used type-position)
                            ContentType.IMAGE,ContentType.DOCX,
                            ContentType.VIDEO , ContentType.AUDIO, ContentType.PDF, ContentType.GIF -> NoteContentModel.MediaContent(
                                title = row.contentTitle?:"${row.type}-${row.position}",
                                id = row.contentId,
                                noteId = row.noteId,
                                url = row.url!!,
                                localPath = row.localPath,
                                position = row.position!!,
                                createdAt = row.contentCreatedAt,
                                updatedAt = row.contentUpdatedAt,
                                duration = row.duration?:0,
                                type = type,
                                mimeType = row.mimeType ?: "",
                                sizeBytes = row.sizeBytes ?: 0,
                                width = row.width?.toInt() ?: 0,
                                height = row.height?.toInt() ?: 0,
                                thumbnailPath = row.thumbnailPath,
                            )

                            ContentType.LINK-> NoteContentModel.Link(
                                url = row.url!!,
                                id = row.contentId,
                                noteId = row.noteId,
                                position = row.position!!,
                                createdAt = row.contentCreatedAt,
                                updatedAt = row.contentUpdatedAt,

                            )

                            ContentType.LOCATION -> NoteContentModel.Location(
                                latitude = row.lat!!,
                                longitude = row.long!!,
                                address = row.address,
                                position = row.position!!,
                                id = row.contentId,
                                noteId = row.noteId,
                                createdAt = row.contentCreatedAt,
                                updatedAt = row.contentUpdatedAt,
                            )
                        }
                    } else null
                }.toMutableList() as ArrayList<NoteContentModel>
            )
        }
        return noteWithContent
    }
}
