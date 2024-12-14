package com.app.pustakam.domain.repositories

import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.request.Login
import com.app.pustakam.data.models.request.RegisterReq
import com.app.pustakam.data.models.response.DeleteDataModel
import com.app.pustakam.data.models.response.User
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.Notes
import com.app.pustakam.util.Error
import com.app.pustakam.util.Result

interface IRemoteRepository {
    //user Apis
    suspend fun loginUser(login : Login) : Result<BaseResponse<User>, Error>
    suspend fun registerUser(user  : RegisterReq) : Result<BaseResponse<User>, Error>
    suspend fun updateUser(user : User) : Result<BaseResponse<User>, Error>
    suspend fun getUser(userId : String) : Result<BaseResponse<User>, Error>
    suspend fun deleteUser() : Result<BaseResponse<User>, Error>
    suspend fun userLogout()
    suspend fun profileImage() : Result<BaseResponse<User>,Error>
    //notes apis
    suspend fun getNotesForUserApi(page : Int = 0) : Result<BaseResponse<Notes>, Error>
    suspend fun upsertNewNoteApi(note : Note) : Result<BaseResponse<Note>, Error>
    suspend fun updateNoteApi(note : Note) : Result<BaseResponse<Note>, Error>
    suspend fun getNoteApi(noteId : String) : Result<BaseResponse<Note>, Error>
    suspend fun deleteNoteApi(noteId : String ) : Result<BaseResponse<DeleteDataModel>, Error>


}