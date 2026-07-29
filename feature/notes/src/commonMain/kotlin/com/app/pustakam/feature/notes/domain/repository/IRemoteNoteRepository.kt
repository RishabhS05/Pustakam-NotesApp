package com.app.pustakam.feature.notes.domain.repository

import com.app.pustakam.core.model.models.BaseResponse
import com.app.pustakam.core.model.models.response.DeleteDataModel
import com.app.pustakam.core.model.models.response.notes.Note
import com.app.pustakam.core.model.models.response.notes.Notes
import com.app.pustakam.core.common.util.Error
import com.app.pustakam.core.common.util.Result

interface IRemoteNoteRepository {
    //notes apis
    suspend fun getNotesForUserApi(page : Int = 0) : Result<BaseResponse<Notes>, Error>
    suspend fun upsertNewNoteApi(note : Note) : Result<BaseResponse<Note>, Error>
    suspend fun updateNoteApi(note : Note) : Result<BaseResponse<Note>, Error>
    suspend fun getNoteApi(noteId : String) : Result<BaseResponse<Note>, Error>
    suspend fun deleteNoteApi(noteId : String ) : Result<BaseResponse<DeleteDataModel>, Error>
}