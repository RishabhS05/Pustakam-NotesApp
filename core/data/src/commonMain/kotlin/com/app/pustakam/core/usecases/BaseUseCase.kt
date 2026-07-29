package com.app.pustakam.core.usecases

import com.app.pustakam.core.data.base.BaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.flowOn
import org.koin.core.component.KoinComponent
import com.app.pustakam.core.common.util.Error
import com.app.pustakam.core.common.util.Result
import org.koin.core.component.get


abstract class BaseUseCase : KoinComponent {
   protected val repository  : BaseRepository by lazy { setRepository() }
     fun <T> getBaseApiCall(
        apiCall: suspend () -> Result<T, Error>
    ): Flow<Result<T , Error>> = flow {
         emit(Result.Loading)
         emit(apiCall())
    }.flowOn(Dispatchers.IO)

 // 🔧 30-Jul-2026 02:10 was get<NoteRepository>() -> cycle :core:data->:feature:notes. Koin binds single<BaseRepository>{ get<NoteRepository>() }, so this resolves to the SAME instance
 open fun setRepository(): BaseRepository = get<BaseRepository>()
    suspend fun logoutUser(){
        repository.userLogout()
    }
}
