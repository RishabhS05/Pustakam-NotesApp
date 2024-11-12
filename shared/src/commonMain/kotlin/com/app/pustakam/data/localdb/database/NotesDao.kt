package com.app.pustakam.data.localdb.database

import app.cash.sqldelight.db.SqlDriver
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.Notes

import com.app.pustakam.database.NotesDatabase

class NotesDao(private val sharedDb: SqlDriver) {
    private val database = NotesDatabase(sharedDb)
    private val queries = database.notesDatabaseQueries
    fun selectAllNotesFromDb(): Notes {
        val notes : List<Note> = LinkedHashSet(queries.selectAll(::noteMapper).executeAsList()).toList()
        val arrayListNotes : ArrayList<Note> = if (notes.isNotEmpty()) notes as ArrayList<Note> else ArrayList()
        return Notes(notes = arrayListNotes , count = notes?.size, page = 0)
    }
    fun insertNotes(notes : Notes){
        database.transaction{
            notes.notes?.forEach(::insertOrUpdateNoteFromDb)
        }
     }
    fun deleteByIdFromDb(id: String) {
        queries.deleteById(id)
    }

    fun insertOrUpdateNoteFromDb(note: Note) {
        queries.insertOrUpdateNote(
            id = note._id!!, userId = note.userId!!,
            title = note.title,
            description = note.description, updatedAt = note.updatedAt,
            createdAt = note.createdAt, categoryId = ""
        )
    }

    private fun noteMapper(
        id: String?, userId: String?, categoryId: String?, title: String?, description: String?, createdAt: String?, updatedAt: String?
    ): Note {
        return Note(
            _id = id, title = title, userId = userId, createdAt = createdAt, updatedAt = updatedAt, url = "", description = description
        )
    }

    fun selectNoteById(id: String): Note? = queries.selectById(id, ::noteMapper).executeAsOneOrNull()
}