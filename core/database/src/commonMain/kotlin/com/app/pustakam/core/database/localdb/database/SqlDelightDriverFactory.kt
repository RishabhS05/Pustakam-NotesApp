@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.app.pustakam.core.database.localdb.database

import app.cash.sqldelight.db.SqlDriver
import com.app.pustakam.core.database.NotesDatabase

const val notesDb = "Notes.db"
expect class SqlDelightDriverFactory {
   fun createDriver() : SqlDriver
}