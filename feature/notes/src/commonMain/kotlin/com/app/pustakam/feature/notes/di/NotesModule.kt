package com.app.pustakam.feature.notes.di

import com.app.pustakam.feature.notes.data.repositoryImpl.NoteContentRepository
import com.app.pustakam.feature.notes.data.repositoryImpl.NoteRepository
import com.app.pustakam.feature.notes.domain.repository.ILocalNotesRepository
import com.app.pustakam.feature.notes.domain.repository.INoteContentRepository
import com.app.pustakam.feature.notes.domain.repository.INoteRepository
import com.app.pustakam.feature.notes.domain.repository.IRemoteNoteRepository
import com.app.pustakam.feature.notes.domain.usecase.ClearSelectedNoteContentUseCase
import com.app.pustakam.feature.notes.domain.usecase.CreateORUpdateNoteUseCase
import com.app.pustakam.feature.notes.domain.usecase.CreateTagUseCase
import com.app.pustakam.feature.notes.domain.usecase.DeleteNoteContentUseCase
import com.app.pustakam.feature.notes.domain.usecase.DeleteNoteUseCase
import com.app.pustakam.feature.notes.domain.usecase.DeleteTagUseCase
import com.app.pustakam.feature.notes.domain.usecase.GetNoteSummariesUseCase
import com.app.pustakam.feature.notes.domain.usecase.GetNotesUseCase
import com.app.pustakam.feature.notes.domain.usecase.GetSelectedMediaIndexUseCase
import com.app.pustakam.feature.notes.domain.usecase.GetTagCase
import com.app.pustakam.feature.notes.domain.usecase.ReadNoteUseCase
import com.app.pustakam.feature.notes.domain.usecase.SearchNotesUseCase
import com.app.pustakam.feature.notes.domain.usecase.SetSelectedNoteContentUseCase
import com.app.pustakam.feature.notes.domain.usecase.ReadContentUseCase
import com.app.pustakam.feature.notes.domain.usecase.UpdateReadingProgressUseCase
import com.app.pustakam.feature.notes.domain.usecase.UpdateSelectedMediaContentUseCase
import com.app.pustakam.feature.notes.domain.usecase.UpdateTagUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

// 🔧 30-Jul-2026 02:10 — lifted VERBATIM out of :shared/koin/Koin.kt (was `repositoriesModules` + the notes half of `useCases`)
fun notesModule(): Module = module {
    single<INoteRepository> { NoteRepository() }
    single<ILocalNotesRepository> { get<INoteRepository>() }
    single<IRemoteNoteRepository> { get<INoteRepository>() }

    single<INoteContentRepository> { NoteContentRepository() }
    factory<CreateORUpdateNoteUseCase> { CreateORUpdateNoteUseCase() }
    factory<DeleteNoteUseCase> { DeleteNoteUseCase() }
    factory<ReadNoteUseCase> { ReadNoteUseCase() }
    factory<GetNotesUseCase> { GetNotesUseCase() }
    factory<GetNoteSummariesUseCase> { GetNoteSummariesUseCase() }
    factory<SearchNotesUseCase> { SearchNotesUseCase() }
    factory<DeleteNoteContentUseCase> { DeleteNoteContentUseCase() }
    factory<UpdateReadingProgressUseCase> { UpdateReadingProgressUseCase() }
    factory<ReadContentUseCase> { ReadContentUseCase() }
    factory<GetTagCase> { GetTagCase() }
    factory<CreateTagUseCase> { CreateTagUseCase() }
    factory<UpdateTagUseCase> { UpdateTagUseCase() }
    factory<DeleteTagUseCase> { DeleteTagUseCase() }
    factory<SetSelectedNoteContentUseCase> { SetSelectedNoteContentUseCase() }
    factory<UpdateSelectedMediaContentUseCase> { UpdateSelectedMediaContentUseCase() }
    factory<GetSelectedMediaIndexUseCase> { GetSelectedMediaIndexUseCase() }
    factory<ClearSelectedNoteContentUseCase> { ClearSelectedNoteContentUseCase() }
}
