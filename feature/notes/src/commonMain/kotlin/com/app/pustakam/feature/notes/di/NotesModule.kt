package com.app.pustakam.feature.notes.di

import com.app.pustakam.core.data.base.BaseRepository
import com.app.pustakam.feature.notes.data.repositoryImpl.NoteContentRepository
import com.app.pustakam.feature.notes.data.repositoryImpl.NoteRepository
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
    single { NoteRepository() }
    // 🔧 30-Jul-2026 02:10 — this binding is what makes BaseUseCase.setRepository()'s get<BaseRepository>() return the SAME NoteRepository singleton as before
    single<BaseRepository> { get<NoteRepository>() }
    single<NoteContentRepository> { NoteContentRepository() }

    factory<CreateORUpdateNoteUseCase> { CreateORUpdateNoteUseCase() }
    factory<DeleteNoteUseCase> { DeleteNoteUseCase() }
    factory<ReadNoteUseCase> { ReadNoteUseCase() }
    factory<GetNotesUseCase> { GetNotesUseCase() }
    // 🔧 15-Jul-2026 Summary query: list-screen summaries
    factory<GetNoteSummariesUseCase> { GetNoteSummariesUseCase() }
    // 🔧 15-Jul-2026 Phase 2.2: full-text search
    factory<SearchNotesUseCase> { SearchNotesUseCase() }
    factory<DeleteNoteContentUseCase> { DeleteNoteContentUseCase() }
    // 📖 23-Jul-2026: reader progress persistence (mirror of DeleteNoteContentUseCase)
    factory<UpdateReadingProgressUseCase> { UpdateReadingProgressUseCase() }
    // 📖 01-Aug-2026: document reader loads ONE content row by id
    factory<ReadContentUseCase> { ReadContentUseCase() }
    factory<GetTagCase> { GetTagCase() }
    factory<CreateTagUseCase> { CreateTagUseCase() }
    factory<UpdateTagUseCase> { UpdateTagUseCase() }
    factory<DeleteTagUseCase> { DeleteTagUseCase() }
    // 🔧 F5: note-content state use cases (NoteContentBridge + future Android migration)
    factory<SetSelectedNoteContentUseCase> { SetSelectedNoteContentUseCase() }
    factory<UpdateSelectedMediaContentUseCase> { UpdateSelectedMediaContentUseCase() }
    factory<GetSelectedMediaIndexUseCase> { GetSelectedMediaIndexUseCase() }
}
