package com.app.pustakam.domain.repositories

import com.app.pustakam.data.localdb.database.NotesDao
import com.app.pustakam.data.localdb.preferences.BasePreferences
import com.app.pustakam.data.localdb.preferences.IAppPreferences
import com.app.pustakam.data.localdb.preferences.UserPreference
import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.request.Login
import com.app.pustakam.data.models.request.RegisterReq
import com.app.pustakam.data.models.response.DeleteDataModel
import com.app.pustakam.data.models.response.User
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.Notes
import com.app.pustakam.data.network.ApiCallClient
import com.app.pustakam.extensions.isNotnull
import com.app.pustakam.util.Error
import com.app.pustakam.util.NetworkError
import com.app.pustakam.util.Result
import com.app.pustakam.util.UniqueIdGenerator
import com.app.pustakam.util.log_d
import com.app.pustakam.util.onError
import com.app.pustakam.util.onSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

open class BaseRepository(private val userPrefs: IAppPreferences) : IRemoteRepository, ILocalRepository, KoinComponent {

    private val apiClient: ApiCallClient = ApiCallClient(userPrefs)
    private val notesDao by inject<NotesDao>()
    val _userAuthState = (userPrefs as BasePreferences).userPreferencesFlow
    private lateinit var prefs: UserPreference

    init {
        CoroutineScope(Dispatchers.IO).launch {
            _userAuthState.collect { pref ->
                prefs = pref
            }
        }
    }
    override suspend fun loginUser(login: Login): Result<BaseResponse<User>, Error>
            = apiClient.login(login).onSuccess {
        it.data?._id?.let { it1 ->
            userPrefs.setUserId(it1)
            userPrefs.setAuth(true)
        }
    }

    override suspend fun registerUser(user: RegisterReq): Result<BaseResponse<User>, Error> = apiClient.register(user)

    /** user crud apis */
    override suspend fun updateUser(user: User): Result<BaseResponse<User>, Error> = apiClient.updateUser(user)

    // pass empty string to get current user
    override suspend fun getUser(userId: String): Result<BaseResponse<User>, Error> {
        val id = userId.ifEmpty { prefs.userId }
        return apiClient.getUser(id)
    }

    override suspend fun deleteUser(): Result<BaseResponse<User>, Error>
            = apiClient.deleteUser(prefs.userId)

    override suspend fun profileImage(): Result<BaseResponse<User>, Error>
            = apiClient.profileImage()

    override suspend fun userLogout() {
        _userAuthState.collect{
            it.copy(token =  "", userId = "", isAuthenticated = false)
        }
    }

    /**---------------- NOTES API SERVER CALL ------------*/
    /** get all notes from server api call */
    override suspend fun getNotesForUserApi(page: Int): Result<BaseResponse<Notes>, Error> {

        return apiClient.getNotes(prefs.userId).onSuccess {
            it.data?.let { it1 -> notesDao.insertNotes(it1) }
        }
    }
    /** update or insert note to server apis call */
    override suspend fun upsertNewNoteApi(note: Note): Result<BaseResponse<Note>, Error> {
        return  apiClient.addNewNote(prefs.userId, note).onSuccess {
            it.data?.let { it1 ->
                log_d("BaseRepo", "addNewNote: $it1")
                insertUpdateFromDb(it1)
            }
        }
    }
    /** update note apis call to server*/
    override suspend fun updateNoteApi(note: Note): Result<BaseResponse<Note>, Error> =
        apiClient.updateNote(prefs.userId, note).onSuccess {
        it.data?.let {
                it1 ->
            log_d("BaseRepo", "addNewNote: $it1")
            insertUpdateFromDb(it1)
        }
    }

    /** delete note apis call to server */
    override suspend fun deleteNoteApi(noteId: String): Result<BaseResponse<DeleteDataModel>, Error> =
        apiClient.deleteNote(prefs.userId, noteId).onSuccess {
            deleteNoteByIdFromDb(noteId)
    }
    /**  get a note apis call from server */
    override suspend fun getNoteApi(noteId: String): Result<BaseResponse<Note>, Error>
    = apiClient.getNote(prefs.userId, noteId)


    /**-----------------------LOCAL DATABASE -------------*/

    /** insert or update a note data from local db */
    override suspend fun insertUpdateFromDb(note: Note): Result<BaseResponse<Note?>, Error>  {
       val newNote = notesDao.insertOrUpdateNoteFromDb(note)
        return if(newNote.isNotnull()) {
            val response = BaseResponse<Note?>(data = newNote , isSuccessful = true,
                isFromDb = true)
            return Result.Success(response)
        } else {
            Result.Error(error = NetworkError.NOT_FOUND)
        }
    }
    /** delete a note data from local db */
    override suspend fun deleteNoteByIdFromDb(id: String?): Result<BaseResponse<Boolean>, Error> {
        if (id.isNullOrEmpty()) Result.Error(error = NetworkError.NOT_FOUND)
       val success =  notesDao.deleteByIdFromDb(id!!)
      return  if(success){
            val response = BaseResponse(data = success, isSuccessful = true, isFromDb = true)
            Result.Success(response)
        }
        else  Result.Error(error = NetworkError.SERVER_ERROR)
    }

    /** get notes data from local db */
    override suspend fun getNotesFromDb( page: Int ): Result<BaseResponse<Notes?>, Error> {
         val notes  = notesDao.selectAllNotesFromDb()
        return if (notes.notes?.isNotEmpty() == true){
         val response = BaseResponse<Notes?>(data = notes ,
             isSuccessful = true,
             isFromDb = true)
            Result.Success(response)
        }else {
            Result.Error(error = NetworkError.SERVER_ERROR)
        }
    }
    /** get a note data from local db */
    override suspend fun getNoteByIdFromDb(id: String?): Result<BaseResponse<Note?>, Error> {
        if(id.isNullOrEmpty()) return Result.Error(error = NetworkError.NOT_FOUND)
         val note = notesDao.selectNoteById(id)
        return if(note.isNotnull()) {
            val response = BaseResponse<Note?>(data = note , isSuccessful = true,
                isFromDb = true)
            Result.Success(response)
        } else {
            Result.Error(error = NetworkError.NOT_FOUND)
        }
    }

    /** methods for decision logic
     * - call local db methods or
     * - call api for server
     * - insert or update note */
   /** // step 1 * check with local db
                 * data is present call update server apis
                 * else call create server apis

    // step 2 insert or update into local db
    // step 3 call api to upsert the data or sync with server
    // step 4 again update the local db with sync data.
    */
    suspend fun insertOrUpdateNote(note : Note ) : Result<BaseResponse<Note?>, Error>{
        val existingNote =  notesDao.selectNoteById(note._id!!)
          return insertUpdateFromDb(note).onSuccess {
              if(existingNote != null ) {
                  updateNoteApi(note)
              }else upsertNewNoteApi(note)
          }
    }
    /** method for decision logic
     * - delete from local db
     * - call delete api from server
     * */
    suspend fun deleteNote(id : String): Result<BaseResponse<Boolean>, Error> {
       return deleteNoteByIdFromDb(id).onSuccess {
           deleteNoteApi(id)
        }
    }
    /** method for decision logic (A note)
     * - read from local db
     * - call read api from server
     * */
    suspend fun getANote(id : String?): Result<BaseResponse<Note?>, Error> {
        if (id.isNullOrEmpty()){
            return Result.Success(BaseResponse(data = createNewEmptyNote(),
                isFromDb = false, isSuccessful = false ))}
       return getNoteByIdFromDb(id).onSuccess {
           getNoteApi(id)
       }
    }
    /** method for decision logic (\notes)
     * - read from local db
     * - call read api from server
     * */
    suspend fun getAllNotes(page: Int = 0 ): Result<BaseResponse<Notes?>, Error> {
        return getNotesFromDb(page).onSuccess {
            getNotesForUserApi(page)
        }.onError {
            getNotesForUserApi(page)
        }
    }

    private fun createNewEmptyNote(): Note  {
        val date = UniqueIdGenerator.getCurrentTimestamp().toString()
        val id = UniqueIdGenerator.generateUniqueId()
        return Note(_id = id, title = "", updatedAt = date, createdAt = date, categoryId = "" )
    }
}