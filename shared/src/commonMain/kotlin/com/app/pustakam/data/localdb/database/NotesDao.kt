package com.app.pustakam.data.localdb.database

import app.cash.sqldelight.db.SqlDriver
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.Notes

import com.app.pustakam.database.NotesDatabase

class NotesDao(private val sharedDb : SqlDriver) {
 private val database = NotesDatabase(sharedDb)
    private val queries = database.notesDatabaseQueries

    fun selectAllNotesFromDb() : Notes {
        val notes = LinkedHashSet(queries.selectAll().executeAsList()).toList() as ArrayList<Note>
        return Notes(notes=notes , count = notes.size, page = 0)
    }
    fun deleteByIdFromDb(id : String) {
        queries.deleteById(id)
    }
    fun insertOrUpdateNoteFromDb(note : Note) {
        queries.insertOrUpdateNote(
            id = note._id!!,
            userId = note.userId!!,
            title = note.title,
            description = note.description,
            updatedAt = note.updatedAt,
            createdAt = note.createdAt,
            categoryId = ""
            )
    }
}