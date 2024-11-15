package com.app.pustakam.data.localdb.database

import app.cash.sqldelight.db.SqlDriver
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun getDatabaseModule(): Module = module {
    single <SqlDriver>{ SqlDelightDriverFactory().createDriver() }
    single<NotesDao> { NotesDao(get()) }
}