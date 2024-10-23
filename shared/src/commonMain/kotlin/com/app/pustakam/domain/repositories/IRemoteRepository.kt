package com.app.pustakam.domain.repositories

import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.request.Login
import com.app.pustakam.data.models.request.NoteRequest
import com.app.pustakam.data.models.request.RegisterReq
import com.app.pustakam.data.models.response.User
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.Notes
import com.app.pustakam.util.Error
import com.app.pustakam.util.Result

interface IRemoteRepository {
    suspend fun loginUser(login : Login) : Result<BaseResponse<User>, Error>
    suspend fun registerUser(user  : RegisterReq) : Result<BaseResponse<User>, Error>
    suspend fun getNotesForUser(userId : String, page : Int = 0) : Result<BaseResponse<Notes>, Error>
    suspend fun addNewNote(userId: String, note : NoteRequest) : Result<BaseResponse<Note>, Error>
    suspend fun updateNote(userId: String,note : NoteRequest) : Result<BaseResponse<Note>, Error>
    suspend fun updateUser(user : User) : Result<BaseResponse<User>, Error>
    suspend fun getUser(userId : String) : Result<BaseResponse<User>, Error>
    suspend fun getNote(userId : String, noteId : String) : Result<BaseResponse<User>, Error>
    suspend fun deleteUser(userId : String) : Result<BaseResponse<User>, Error>
    suspend fun deleteNote(userId: String,noteId : String ) : Result<BaseResponse<Note>, Error>
    suspend fun profileImage() : Result<BaseResponse<User>,Error>
}