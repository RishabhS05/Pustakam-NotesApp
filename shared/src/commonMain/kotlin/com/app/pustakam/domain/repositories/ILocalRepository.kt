package com.app.pustakam.domain.repositories

import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.Notes
import com.app.pustakam.util.Result
import com.app.pustakam.util.Error
interface ILocalRepository {
    suspend  fun insertUpdateFromDb(note: Note) : Result<BaseResponse<Note?>,Error>
    suspend  fun deleteNoteByIdFromDb(id : String?) : Result<BaseResponse<Boolean>, Error>
    suspend  fun getNotesFromDb(page: Int) : Result<BaseResponse<Notes?>, Error>
    suspend fun getNoteByIdFromDb(id :String?) : Result<BaseResponse<Note?>, Error>
}