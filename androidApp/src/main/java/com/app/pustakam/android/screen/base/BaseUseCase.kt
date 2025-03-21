package com.app.pustakam.android.screen.base

import com.app.pustakam.domain.repositories.base.BaseRepository
import com.app.pustakam.domain.repositories.noteRepository.NoteRepository
import com.app.pustakam.util.Error
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import  com.app.pustakam.util.Result
import kotlinx.coroutines.flow.flowOn
import org.koin.core.component.KoinComponent

import org.koin.core.component.get


abstract  class NoteBaseUseCase: BaseUseCase(){
   protected val noteRepository = repository as NoteRepository
    val notes =  noteRepository.notesState
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

 open fun setRepository(): BaseRepository = get<BaseRepository>()
    suspend fun logoutUser(){
        repository.userLogout()
    }
}
