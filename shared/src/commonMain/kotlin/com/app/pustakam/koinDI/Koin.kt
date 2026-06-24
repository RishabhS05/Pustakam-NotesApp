package com.app.pustakam.koinDI

import app.cash.sqldelight.ColumnAdapter
import com.app.pustakam.data.localdb.database.RichTextMetadata
import com.app.pustakam.data.localdb.database.getDatabaseModule
import com.app.pustakam.data.localdb.preferences.getDataSourceFromPlatForm
import com.app.pustakam.data.network.ApiCallClient
import com.app.pustakam.database.NoteContent
import com.app.pustakam.database.NotesDatabase
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {

    appDeclaration()

    val databaseModule  = module {
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
    val networkModule = module {
        single<ApiCallClient> { ApiCallClient(get())  }
    }
    modules(getDataSourceFromPlatForm(), repositoriesModules(),networkModule, databaseModule, getDatabaseModule(), )

}


