package com.app.pustakam.data.localdb.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.app.pustakam.database.NotesDatabase


actual class SqlDelightDriverFactory {
    actual fun createDriver(): SqlDriver {
       return  NativeSqliteDriver(NotesDatabase.Schema, notesDb)
    }
}