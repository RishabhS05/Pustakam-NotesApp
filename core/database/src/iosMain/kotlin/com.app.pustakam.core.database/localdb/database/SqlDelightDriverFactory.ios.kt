@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.app.pustakam.core.database.localdb.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.app.pustakam.core.database.NotesDatabase

actual class SqlDelightDriverFactory {
    actual fun createDriver(): SqlDriver {
       return  NativeSqliteDriver(schema = NotesDatabase.Schema, name = notesDb)
    }
}