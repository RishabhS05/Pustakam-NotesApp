package com.app.pustakam.core.database.di

import app.cash.sqldelight.ColumnAdapter
import com.app.pustakam.core.database.NoteContent
import com.app.pustakam.core.database.NotesDatabase
import com.app.pustakam.core.database.localdb.preferences.BasePreferences
import com.app.pustakam.core.database.localdb.preferences.IAppPreferences
import com.app.pustakam.core.model.models.RichTextMetadata
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module

// 🔧 30-Jul-2026 02:10 — lifted VERBATIM out of :shared/koin/Koin.kt so the module that owns NotesDatabase also owns its registration
fun databaseModule(): Module = module {
    val richTextAdapter = object : ColumnAdapter<RichTextMetadata, String> {
        override fun decode(databaseValue: String): RichTextMetadata =
            Json.decodeFromString(databaseValue)

        override fun encode(value: RichTextMetadata): String =
            Json.encodeToString(value)
    }

    single<NotesDatabase> {
        NotesDatabase(
            driver = get(),
            NoteContentAdapter = NoteContent.Adapter(
                metaDataAdapter = richTextAdapter
            )
        )
    }
}

// 🔧 30-Jul-2026 02:10 — lifted VERBATIM out of :shared/koin/Koin.kt (was `sharedPrefModule`)
fun preferencesModule(): Module = module {
    single { BasePreferences(get()) }
    single<IAppPreferences> { get<BasePreferences>() }
}
