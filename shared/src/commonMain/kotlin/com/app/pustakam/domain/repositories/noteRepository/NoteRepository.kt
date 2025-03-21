package com.app.pustakam.domain.repositories.noteRepository

import com.app.pustakam.data.localdb.preferences.IAppPreferences
import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.response.DeleteDataModel
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.Notes

import com.app.pustakam.domain.repositories.base.BaseRepository
import com.app.pustakam.extensions.isNotnull
import com.app.pustakam.util.Error
import com.app.pustakam.util.NetworkError
import com.app.pustakam.util.Result
import com.app.pustakam.util.UniqueIdGenerator
import com.app.pustakam.util.getCurrentTimestamp
import com.app.pustakam.util.log_d
import com.app.pustakam.util.onError
import com.app.pustakam.util.onSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.update

class NoteRepository(private val userPreference: IAppPreferences) : BaseRepository(userPreference),
    IRemoteNoteRepository, ILocalNotesRepository {
        private val _notes= MutableStateFlow(Notes())
       val notesState = _notes.asStateFlow()
    fun insertNotes(notes : Notes){
        _notes.update {
            val list : ArrayList<Note> = arrayListOf()
            list.addAll(it.notes)
            list.addAll(notes.notes)
            it.copy(notes = list, page =  notes.page)
        }
        log_d("Notes" , _notes.value.notes.count())
    }

    /**---------------- NOTES API SERVER CALL ------------*/
    /** get all notes from server api call */
    override suspend fun getNotesForUserApi(page: Int): Result<BaseResponse<Notes>, Error> {

        return apiClient.getNotes(prefs.userId).onSuccess {
            it.data?.let { it1 -> notesDao.insertNotes(it1)
            }
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
    override suspend fun insertUpdateFromDb(note: Note): Result<BaseResponse<Note?>, Error> {
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
        val notes  = notesDao.selectAllNotesFromDb(page)
        val response = BaseResponse<Notes?>(data = notes ,
            isSuccessful = true,
            isFromDb = true)
        return  Result.Success(response)

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

    /** methods for deleting note content from db
     * */
    override suspend fun deleteNoteContentFromDb(id: String?): Result<BaseResponse<Boolean>, Error> {
        if (id.isNullOrEmpty()) Result.Error(error = NetworkError.NOT_FOUND)
        notesDao.deleteNoteContentById(id!!)
        return Result.Success(BaseResponse(data = true, isSuccessful = true))
    }

    /** methods for decision logic
     * - call local db methods or
     * - call api for server
     * - insert or update note
    // step 1 * check with local db
     * data is present call update server apis
     * else call create server apis

    // step 2 insert or update into local db
    // step 3 call api to upsert the data or sync with server
    // step 4 again update the local db with sync data.
     */
    suspend fun insertOrUpdateNote(note : Note) : Result<BaseResponse<Note?>, Error> {
        val existingNote =  notesDao.selectNoteById(note.id!!)
        return insertUpdateFromDb(note).onSuccess {
            _notes.update { notes->
                val index = notes.notes.indexOfFirst {n -> note.id == n.id  }
                if(index!= -1) notes.notes.set(index,note) else
                    notes.notes.add(note)
                val newList  =ArrayList(notes.notes)
                notes.copy(notes = newList )
            }
//              if(existingNote != null ) {
//                  updateNoteApi(note)
//              }else upsertNewNoteApi(note)
        }
    }
    /** method for decision logic
     * - delete from local db
     * - call delete api from server
     * */
    suspend fun deleteNote(id : String): Result<BaseResponse<Boolean>, Error> {
        return deleteNoteByIdFromDb(id).onSuccess {
            //deleteNoteApi(id)
            _notes.update {
                val note = it.notes.find{ note-> id == note.id  }
                if (it.notes.remove(note)){
                    it.copy(notes =it.notes)
                } else it
            }
        }
    }
    /** method for decision logic (A note)
     * - read from local db
     * - call read api from server
     * */
    suspend fun getANote(id : String?): Result<BaseResponse<Note?>, Error> {
        if (id.isNullOrEmpty()){
            return Result.Success(
                BaseResponse(data = createNewEmptyNote(),
                isFromDb = false, isSuccessful = false )
            )
        }
        return getNoteByIdFromDb(id).onSuccess {
//           getNoteApi(id)
        }
    }
    /** method for decision logic (\notes)
     * - read from local db
     * - call read api from server
     * */
    suspend fun getAllNotes(page: Int = 0): Result<BaseResponse<Notes?>, Error> {
        return getNotesFromDb(page).onSuccess { notes->
            if(notes.data?.notes?.count()!! > 0){
              insertNotes(notes = notes.data)
            }
//            getNotesForUserApi(page)
        }.onError {
//            getNotesForUserApi(page)
        }
    }
    /** create an blank note
     */
    private fun createNewEmptyNote(): Note {
        val date = getCurrentTimestamp().toString()
        val id = UniqueIdGenerator.generateUniqueId()
        return Note(id = id, title = "", updatedAt = date, createdAt = date, categoryId = "" )
    }
}