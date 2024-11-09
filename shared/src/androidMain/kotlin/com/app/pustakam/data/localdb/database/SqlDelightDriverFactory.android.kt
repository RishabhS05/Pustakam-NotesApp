package com.app.pustakam.data.localdb.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.app.pustakam.database.NotesDatabase
actual class SqlDelightDriverFactory(val context : Context){
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(NotesDatabase.Schema, context, notesDb)
    }
}