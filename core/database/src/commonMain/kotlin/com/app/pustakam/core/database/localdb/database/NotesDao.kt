package com.app.pustakam.core.database.localdb.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.app.pustakam.core.model.models.Tag
import com.app.pustakam.core.model.models.response.notes.Note
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import com.app.pustakam.core.model.models.response.notes.Notes
import com.app.pustakam.core.database.NoteContent
import com.app.pustakam.core.database.NotesDatabase
import com.app.pustakam.core.common.util.ContentType
import com.app.pustakam.core.common.util.getCurrentTimestamp
import com.app.pustakam.core.common.util.log_d
// 🔧 F4: coroutine imports removed — DAO writes are synchronous inside transactions now
import org.koin.core.component.KoinComponent
import com.app.pustakam.core.model.models.RichTextMetadata
import org.koin.core.component.get

class NotesDao : KoinComponent {

    private val database =  get<NotesDatabase>()

    // 🔧 15-Jul-2026 CRASH FIX: raw driver access for the OPTIONAL runtime FTS5 index (see
    //   ensureFtsIndex below) — fts statements can't live in the managed schema anymore.
    private val driver = get<SqlDriver>()

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
   // 🔧 15-Jul-2026 Summary query: one page of LIST-SCREEN summaries — header + snippet + counts +
   //   thumbnail, computed in SQL (indexed subqueries). Contents are never loaded for the list.
   fun selectNoteSummariesPage(limit: Int, page: Int): List<com.app.pustakam.core.model.models.response.notes.NoteSummary> {
       val offset = ((page - 1) * limit).coerceAtLeast(0)
       return queries.selectNoteSummariesPage(limit.toLong(), offset.toLong()).executeAsList().map { row ->
           com.app.pustakam.core.model.models.response.notes.NoteSummary(
               id = row.id,
               title = row.title,
               categoryId = row.categoryId,
               createdAt = row.createdAt,
               updatedAt = row.updatedAt,
               snippet = row.snippet,
               contentCount = (row.contentCount ?: 0L).toInt(),
               imageCount = (row.imageCount ?: 0L).toInt(),
               videoCount = (row.videoCount ?: 0L).toInt(),
               audioCount = (row.audioCount ?: 0L).toInt(),
               docCount = (row.docCount ?: 0L).toInt(),
               // 🔧 15-Jul-2026 iOS MEDIA-LOST FIX: rebase onto the current container (UUID changes on iOS updates)
               thumbnailPath = com.app.pustakam.core.common.util.resolveLocalFilePath(row.thumbnailPath),
           )
       }
   }

   // 🔧 15-Jul-2026 CRASH FIX ("no such module: fts5") — FTS5 is OPTIONAL now. Some devices'
   //   framework SQLite lacks the fts5 module, so nothing in the managed schema/migrations may
   //   reference it. Instead, the index is created here AT RUNTIME, once, inside try/catch, and
   //   ONLY when search is first used — app startup can never touch this path. Devices without
   //   fts5 permanently fall back to the LIKE query (correct, just slower on huge notes).
   //   null = not yet probed; true/false = probe result for this process.
   private var ftsAvailable: Boolean? = null

   private fun ensureFtsIndex(): Boolean {
       ftsAvailable?.let { return it }
       val available = try {
           val existedBefore = ftsTableExists()
           driver.execute(null,
               "CREATE VIRTUAL TABLE IF NOT EXISTS NoteContentFts USING fts5(text, content='NoteContent', content_rowid='rowid')", 0).value
           driver.execute(null,
               "CREATE TRIGGER IF NOT EXISTS note_content_fts_insert AFTER INSERT ON NoteContent BEGIN " +
                       "INSERT INTO NoteContentFts(rowid, text) VALUES (new.rowid, new.text); END", 0).value
           driver.execute(null,
               "CREATE TRIGGER IF NOT EXISTS note_content_fts_delete AFTER DELETE ON NoteContent BEGIN " +
                       "INSERT INTO NoteContentFts(NoteContentFts, rowid, text) VALUES ('delete', old.rowid, old.text); END", 0).value
           driver.execute(null,
               "CREATE TRIGGER IF NOT EXISTS note_content_fts_update AFTER UPDATE ON NoteContent BEGIN " +
                       "INSERT INTO NoteContentFts(NoteContentFts, rowid, text) VALUES ('delete', old.rowid, old.text); " +
                       "INSERT INTO NoteContentFts(rowid, text) VALUES (new.rowid, new.text); END", 0).value
           // fresh index → build it from the existing content once ('rebuild' is idempotent)
           if (!existedBefore) {
               driver.execute(null, "INSERT INTO NoteContentFts(NoteContentFts) VALUES('rebuild')", 0).value
           }
           true
       } catch (t: Throwable) {
           log_d("NotesDao", "FTS5 unavailable on this device, using LIKE search: ${t.message}")
           false
       }
       ftsAvailable = available
       return available
   }

   private fun ftsTableExists(): Boolean = driver.executeQuery(null,
       "SELECT name FROM sqlite_master WHERE type='table' AND name='NoteContentFts'",
       { cursor -> QueryResult.Value(cursor.next().value) }, 0).value

   // 🔧 15-Jul-2026 CRASH FIX: the MATCH query runs as a RAW statement (it can't live in the .sq
   //   file anymore — SQLDelight would require the fts table in the managed schema). Only reached
   //   when ensureFtsIndex() returned true.
   private fun searchContentViaFts(match: String): List<com.app.pustakam.core.model.models.response.notes.NoteSummary> {
       val results = mutableListOf<com.app.pustakam.core.model.models.response.notes.NoteSummary>()
       driver.executeQuery(null,
           "SELECT n.id, n.categoryId, n.title, n.createdAt, n.updatedAt, SUBSTR(c.text, 1, 200) " +
                   "FROM NoteContentFts " +
                   "JOIN NoteContent c ON c.rowid = NoteContentFts.rowid " +
                   "JOIN Notes n ON n.id = c.noteId " +
                   "WHERE NoteContentFts MATCH ? " +
                   "GROUP BY n.id ORDER BY n.updatedAt DESC LIMIT 50",
           { cursor ->
               while (cursor.next().value) {
                   results.add(
                       com.app.pustakam.core.model.models.response.notes.NoteSummary(
                           id = cursor.getString(0)!!,
                           categoryId = cursor.getString(1),
                           title = cursor.getString(2),
                           createdAt = cursor.getString(3),
                           updatedAt = cursor.getString(4),
                           snippet = cursor.getString(5),
                       )
                   )
               }
               QueryResult.Unit
           }, 1) { bindString(0, match) }.value
       return results
   }

   // 🔧 15-Jul-2026 Phase 2.2: full-text search — content matches (FTS5 where the device supports
   //   it, LIKE fallback everywhere else — never crashes either way) merged with title matches.
   //   Deduped by note id (content match wins: it carries the snippet), newest first. Raw input is
   //   wrapped as a quoted prefix phrase so FTS5 operators in user input can't break MATCH syntax.
   fun searchNotes(rawQuery: String): List<com.app.pustakam.core.model.models.response.notes.NoteSummary> {
       val trimmed = rawQuery.trim()
       if (trimmed.isEmpty()) return emptyList()
       val merged = LinkedHashMap<String, com.app.pustakam.core.model.models.response.notes.NoteSummary>()
       val contentMatches = try {
           if (ensureFtsIndex()) {
               searchContentViaFts("\"" + trimmed.replace("\"", "") + "\"*")
           } else {
               queries.searchContentTextLike(trimmed).executeAsList().map { row ->
                   com.app.pustakam.core.model.models.response.notes.NoteSummary(
                       id = row.id, title = row.title, categoryId = row.categoryId,
                       createdAt = row.createdAt, updatedAt = row.updatedAt, snippet = row.snippet,
                   )
               }
           }
       } catch (t: Throwable) {
           // belt-and-braces: a search must never crash the app — worst case, no content matches
           log_d("NotesDao", "content search failed: ${t.message}")
           emptyList()
       }
       contentMatches.forEach { merged[it.id] = it }
       queries.searchTitles(trimmed).executeAsList().forEach { row ->
           if (!merged.containsKey(row.id)) merged[row.id] = com.app.pustakam.core.model.models.response.notes.NoteSummary(
               id = row.id, title = row.title, categoryId = row.categoryId,
               createdAt = row.createdAt, updatedAt = row.updatedAt,
           )
       }
       return merged.values.sortedByDescending { it.updatedAt ?: "" }
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
                  // 🔧 21-Jul-2026 databasev2.md §2.4: hydrate offline-first sync fields from the row
                  ownerId = note.ownerId,
                  version = note.version,
                  syncStatus = note.syncStatus,
                  deleted = note.deleted == 1L,
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
                              // 🔧 18-Jul-2026: file-import — TXT/MD/EPUB/OTHER round-trip as media rows too
                              ContentType.IMAGE, ContentType.DOCX,  ContentType.VIDEO, ContentType.AUDIO, ContentType.PDF, ContentType.GIF,
                              ContentType.TXT, ContentType.MD, ContentType.EPUB, ContentType.OTHER  ->
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
                                  // 📖 23-Jul-2026: reading progress travels with the media row
                                  totalPages = (row.totalPages ?: 0L).toInt(),
                                  progressPage = (row.progressPage ?: 0L).toInt(),
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
        // 📖 23-Jul-2026: reading progress persisted with the media row
        var totalPages: Long = 0
        var progressPage: Long = 0
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
                totalPages = noteContent.totalPages.toLong()
                progressPage = noteContent.progressPage.toLong()
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
            totalPages = totalPages,
            progressPage = progressPage,
            metaData =  metadata
        )
    }

    fun updateReadingProgress(contentId: String, progressPage: Int, totalPages: Int) {
        queries.updateReadingProgress(
            progressPage = progressPage.toLong(),
            totalPages = totalPages.toLong(),
            updatedAt = "${getCurrentTimestamp()}",
            id = contentId
        )
    }
    fun  deleteNoteContentById(id : String) = queries.deleteNoteContentById(id)

    // 📖 01-Aug-2026: returns the DOMAIN model, not the raw row — the document reader consumes this
    fun getNoteContentById(id: String): NoteContentModel? =
        queries.selectNoteContentById(id).executeAsOneOrNull()?.toNoteContentModel()

    // 📖 01-Aug-2026: single row -> domain mapper (same field mapping the note query already uses)
    private fun NoteContent.toNoteContentModel(): NoteContentModel? {
        if (type.isEmpty()) return null
        return when (val contentType = ContentType.valueOf(type)) {
            ContentType.TEXT -> NoteContentModel.TextContent(
                id = id, noteId = noteId, text = text ?: "", position = position ?: 0.0,
                createdAt = createdAt, updatedAt = updatedAt, metadata = metaData,
            )

            ContentType.IMAGE, ContentType.DOCX, ContentType.VIDEO, ContentType.AUDIO,
            ContentType.PDF, ContentType.GIF, ContentType.TXT, ContentType.MD,
            ContentType.EPUB, ContentType.OTHER -> NoteContentModel.MediaContent(
                title = title ?: "$type-$position",
                id = id, noteId = noteId, url = url ?: "", position = position ?: 0.0,
                createdAt = createdAt, updatedAt = updatedAt,
                localPath = localPath, duration = duration ?: 0, type = contentType,
                mimeType = mimeType ?: "", sizeBytes = sizeBytes ?: 0,
                width = width?.toInt() ?: 0, height = height?.toInt() ?: 0,
                thumbnailPath = thumbnailPath,
                totalPages = totalPages.toInt(), progressPage = progressPage.toInt(),
            )

            ContentType.LINK -> NoteContentModel.Link(
                url = url ?: "", id = id, noteId = noteId, position = position ?: 0.0,
                createdAt = createdAt, updatedAt = updatedAt,
            )

            ContentType.LOCATION -> NoteContentModel.Location(
                latitude = lat ?: 0.0, longitude = long ?: 0.0, address = address,
                position = position ?: 0.0, id = id, noteId = noteId,
                createdAt = createdAt, updatedAt = updatedAt,
            )
        }
    }
    suspend fun deleteNoteByIdFromDb(id: String) : Boolean {
        queries.deleteNoteById(id)
        val note  = selectNoteById(id)
        return note == null
    }
    fun insertOrUpdateNoteFromDb(note: Note, dirtyContentIds: Set<String>? = null) : Note {
        log_d("NoteDao insert", note)
        queries.insertOrUpdateNote(
            id = note.id,
            title = note.title,
            updatedAt = note.updatedAt,
            createdAt = note.createdAt,
            categoryId = note.categoryId,
            // 🔧 21-Jul-2026 databasev2.md §2.4: persist offline-first sync fields
            ownerId = note.ownerId,
            version = note.version,
            syncStatus = note.syncStatus,
            deleted = if (note.deleted) 1L else 0L,
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
                // 🔧 21-Jul-2026 databasev2.md §2.4: hydrate offline-first sync fields from the row
                ownerId = note.ownerId,
                version = note.version,
                syncStatus = note.syncStatus,
                deleted = note.deleted == 1L,
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
                            // 🔧 18-Jul-2026: file-import — TXT/MD/EPUB/OTHER round-trip as media rows too
                            ContentType.IMAGE,ContentType.DOCX,
                            ContentType.VIDEO , ContentType.AUDIO, ContentType.PDF, ContentType.GIF,
                            ContentType.TXT, ContentType.MD, ContentType.EPUB, ContentType.OTHER -> NoteContentModel.MediaContent(
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
                                // 📖 23-Jul-2026: reading progress travels with the media row
                                totalPages = (row.totalPages ?: 0L).toInt(),
                                progressPage = (row.progressPage ?: 0L).toInt(),
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
