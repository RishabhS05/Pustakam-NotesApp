package com.app.pustakam.domain.repositories.noteRepository

import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.Notes
import com.app.pustakam.domain.repositories.base.ILocalRepository

import com.app.pustakam.util.Error
import com.app.pustakam.util.Result

interface ILocalNotesRepository {
    suspend  fun insertUpdateFromDb(note: Note) : Result<BaseResponse<Note?>, Error>
    suspend  fun deleteNoteByIdFromDb(id : String?) : Result<BaseResponse<Boolean>, Error>
    suspend  fun getNotesFromDb(page: Int) : Result<BaseResponse<Notes?>, Error>
    suspend fun getNoteByIdFromDb(id :String?) : Result<BaseResponse<Note?>, Error>
    suspend fun deleteNoteContentFromDb(id :String? ) : Result<BaseResponse<Boolean>, Error>
}