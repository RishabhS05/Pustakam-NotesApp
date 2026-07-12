package com.app.pustakam.koinDI

import app.cash.sqldelight.ColumnAdapter
import com.app.pustakam.data.localdb.database.RichTextMetadata
import com.app.pustakam.data.localdb.database.getDatabaseModule
import com.app.pustakam.data.localdb.preferences.BasePreferences
import com.app.pustakam.data.localdb.preferences.IAppPreferences
import com.app.pustakam.data.localdb.preferences.getDataSourceFromPlatForm
import com.app.pustakam.data.network.ApiCallClient
import com.app.pustakam.database.NoteContent
import com.app.pustakam.database.NotesDatabase
import com.app.pustakam.domain.repositories.base.BaseRepository
import com.app.pustakam.domain.repositories.noteRepository.NoteContentRepository
import com.app.pustakam.domain.repositories.noteRepository.NoteRepository
import com.app.pustakam.domain.repositories.usecases.AppUserCase
import com.app.pustakam.domain.repositories.usecases.CreateORUpdateNoteUseCase
import com.app.pustakam.domain.repositories.usecases.CreateTagUseCase
import com.app.pustakam.domain.repositories.usecases.DeleteNoteContentUseCase
import com.app.pustakam.domain.repositories.usecases.DeleteNoteUseCase
import com.app.pustakam.domain.repositories.usecases.DeleteTagUseCase
import com.app.pustakam.domain.repositories.usecases.DeleteUserUseCase
import com.app.pustakam.domain.repositories.usecases.GetNotesUseCase
import com.app.pustakam.domain.repositories.usecases.GetTagCase
import com.app.pustakam.domain.repositories.usecases.LoginUseCase
import com.app.pustakam.domain.repositories.usecases.ReadNoteUseCase
import com.app.pustakam.domain.repositories.usecases.ReadUserUseCase
import com.app.pustakam.domain.repositories.usecases.SetSelectedNoteContentUseCase
import com.app.pustakam.domain.repositories.usecases.UpdateSelectedMediaContentUseCase
import com.app.pustakam.domain.repositories.usecases.GetSelectedMediaIndexUseCase
import com.app.pustakam.domain.repositories.usecases.SignUseCase
import com.app.pustakam.domain.repositories.usecases.UpdateTagUseCase
import com.app.pustakam.domain.repositories.usecases.UpdateUserUseCase
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.Module
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
    val networkModule = module { single<ApiCallClient> { ApiCallClient()  } }

    val sharedPrefModule = module{
            single { BasePreferences(get()) }
            single<IAppPreferences> { get<BasePreferences>() }
    }

    val   repositoriesModules : Module = module {
        single{ NoteRepository() }
        single<BaseRepository>{ get<NoteRepository>() }
        single <NoteContentRepository>{ NoteContentRepository()  }
    }
    /**
     *  use cases object
     * */
    val useCases : Module = module {
        factory <CreateORUpdateNoteUseCase>{ CreateORUpdateNoteUseCase() }
        factory <DeleteNoteUseCase>{ DeleteNoteUseCase() }
        factory <ReadNoteUseCase>{ ReadNoteUseCase() }
        factory <GetNotesUseCase>{ GetNotesUseCase() }
        factory <DeleteNoteContentUseCase>{ DeleteNoteContentUseCase() }
        factory <GetTagCase>{ GetTagCase() }
        factory <CreateTagUseCase>{ CreateTagUseCase() }
        factory <UpdateTagUseCase>{ UpdateTagUseCase() }
        factory <DeleteTagUseCase>{ DeleteTagUseCase() }
        factory <SignUseCase> { SignUseCase() }
        factory <LoginUseCase>{ LoginUseCase() }
        factory <AppUserCase>{ AppUserCase() }
        factory <DeleteUserUseCase>{ DeleteUserUseCase() }
        factory <UpdateUserUseCase>{ UpdateUserUseCase() }
        factory <ReadUserUseCase>{ ReadUserUseCase() }
        // 🔧 F5: note-content state use cases (NoteContentBridge + future Android migration)
        factory <SetSelectedNoteContentUseCase>{ SetSelectedNoteContentUseCase() }
        factory <UpdateSelectedMediaContentUseCase>{ UpdateSelectedMediaContentUseCase() }
        factory <GetSelectedMediaIndexUseCase>{ GetSelectedMediaIndexUseCase() }
    }
    modules(sharedPrefModule,getDataSourceFromPlatForm(),repositoriesModules,useCases, networkModule, databaseModule, getDatabaseModule())
}


