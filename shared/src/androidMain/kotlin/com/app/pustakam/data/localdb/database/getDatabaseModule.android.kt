package com.app.pustakam.data.localdb.database

import app.cash.sqldelight.db.SqlDriver
import com.app.pustakam.data.models.response.notes.Note
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun getDatabaseModule(): Module = module {
    single <SqlDriver>{ SqlDelightDriverFactory(context = androidContext()).createDriver() }
    single<NotesDao> { NotesDao(get()) }
}