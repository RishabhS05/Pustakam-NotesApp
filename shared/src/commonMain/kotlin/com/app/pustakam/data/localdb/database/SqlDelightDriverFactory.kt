package com.app.pustakam.data.localdb.database

import app.cash.sqldelight.db.SqlDriver
import com.app.pustakam.database.NotesDatabase

const val notesDb = "Notes.db"
expect class SqlDelightDriverFactory {
   fun createDriver() : SqlDriver
}