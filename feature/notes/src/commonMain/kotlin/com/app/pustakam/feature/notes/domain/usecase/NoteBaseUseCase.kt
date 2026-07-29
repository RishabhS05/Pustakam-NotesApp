
package com.app.pustakam.feature.notes.domain.usecase
import com.app.pustakam.core.usecases.BaseUseCase
import com.app.pustakam.feature.notes.data.repositoryImpl.NoteRepository
import org.koin.core.component.get

abstract class NoteBaseUseCase: BaseUseCase(){
   protected val noteRepository = repository as NoteRepository
    val notes =  noteRepository.notesState
    val tags = noteRepository.tagState
    // 🔧 15-Jul-2026 Summary query: list-screen summaries stream
    val noteSummaries = noteRepository.noteSummariesState
    override fun setRepository(): NoteRepository {
        return get<NoteRepository>()
    }
}