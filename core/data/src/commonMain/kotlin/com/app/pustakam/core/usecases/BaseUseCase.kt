package com.app.pustakam.core.usecases

import com.app.pustakam.domain.repositories.base.BaseRepository
import com.app.pustakam.domain.repositories.noteRepository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.flowOn
import org.koin.core.component.KoinComponent


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
abstract class BaseUseCase : KoinComponent {
   protected val repository  : BaseRepository by lazy { setRepository() }
     fun <T> getBaseApiCall(
        apiCall: suspend () -> Result<T, Error>
    ): Flow<Result<T , Error>> = flow {
         emit(Result.Loading)
         emit(apiCall())
    }.flowOn(Dispatchers.IO)

 open fun setRepository(): BaseRepository = get<NoteRepository>()
    suspend fun logoutUser(){
        repository.userLogout()
    }
}
