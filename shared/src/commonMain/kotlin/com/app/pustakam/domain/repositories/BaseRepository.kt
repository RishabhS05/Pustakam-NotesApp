package com.app.pustakam.domain.repositories

import com.app.pustakam.data.localdb.preferences.BasePreferences
import com.app.pustakam.data.localdb.preferences.IAppPreferences
import com.app.pustakam.data.localdb.preferences.UserPreference
import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.request.Login
import com.app.pustakam.data.models.request.NoteRequest
import com.app.pustakam.data.models.request.RegisterReq
import com.app.pustakam.data.models.response.User
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.Notes
import com.app.pustakam.data.network.ApiCallClient
import com.app.pustakam.util.Error
import com.app.pustakam.util.Result
import com.app.pustakam.util.log_d
import com.app.pustakam.util.onSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent

open class BaseRepository(private val userPrefs: IAppPreferences) : IRemoteRepository, ILocalRepository, KoinComponent {

    private val apiClient: ApiCallClient = ApiCallClient(userPrefs)

    private val _userAuthState = (userPrefs as BasePreferences).userPreferencesFlow
    lateinit var prefs: UserPreference

    init {
        CoroutineScope(Dispatchers.IO).launch {
            _userAuthState.collect { pref ->
                prefs = pref
            }
        }
    }

    /** note crud apis*/
    override suspend fun getNotesForUser(page: Int): Result<BaseResponse<Notes>, Error> {
        return apiClient.getNotes(prefs.userId)
    }

    override suspend fun addNewNote(note: NoteRequest): Result<BaseResponse<Note>, Error> = apiClient.addNewNote(prefs.userId, note)

    override suspend fun updateNote(note: NoteRequest): Result<BaseResponse<Note>, Error> = apiClient.updateNote(prefs.userId, note)

    override suspend fun deleteNote(noteId: String): Result<BaseResponse<Note>, Error> = apiClient.deleteNote(prefs.userId, noteId)

    override suspend fun getNote(noteId: String): Result<BaseResponse<User>, Error> = apiClient.getNote(prefs.userId, noteId)

    override suspend fun loginUser(login: Login): Result<BaseResponse<User>, Error> = apiClient.login(login).onSuccess {
        it.data?._id?.let { it1 ->
            userPrefs.setUserId(it1)
            userPrefs.setAuth(true)
        }
    }

    override suspend fun registerUser(user: RegisterReq): Result<BaseResponse<User>, Error> = apiClient.register(user)
    
    /** user crud apis*/
    override suspend fun updateUser(user: User): Result<BaseResponse<User>, Error> = apiClient.updateUser(user)

    // pass empty string to get current user
    override suspend fun getUser(userId: String): Result<BaseResponse<User>, Error> {
        val id = userId.ifEmpty { prefs.userId }
        return apiClient.getUser(id)
    }

    override suspend fun deleteUser(): Result<BaseResponse<User>, Error> = apiClient.deleteUser(prefs.userId)

    override suspend fun profileImage(): Result<BaseResponse<User>, Error> = apiClient.profileImage()
}