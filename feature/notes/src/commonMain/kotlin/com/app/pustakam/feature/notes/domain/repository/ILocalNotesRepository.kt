package com.app.pustakam.feature.notes.domain.repository

import com.app.pustakam.core.model.models.BaseResponse
import com.app.pustakam.core.model.models.Tag
import com.app.pustakam.core.model.models.response.notes.Note
import com.app.pustakam.core.model.models.response.notes.Notes
import com.app.pustakam.core.common.util.Error
import com.app.pustakam.core.common.util.Result
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import com.app.pustakam.core.model.models.response.notes.NoteSummary
import kotlinx.coroutines.flow.StateFlow

interface ILocalNotesRepository {
    val notesState: StateFlow<Notes>
    val tagState: StateFlow<List<Tag>>
    val noteSummariesState: StateFlow<List<NoteSummary>>

    suspend fun insertUpdateFromDb(note: Note) : Result<BaseResponse<Note>, Error>
    suspend fun deleteNoteByIdFromDb(id : String?) : Result<BaseResponse<Boolean>, Error>
    suspend fun getNotesFromDb(page: Int, limit: Int = 0) : Result<BaseResponse<Notes>, Error>
    suspend fun getNoteByIdFromDb(id :String?) : Result<BaseResponse<Note>, Error>
    suspend fun deleteNoteContentFromDb(id :String? ) : Result<BaseResponse<Boolean>, Error>
    suspend fun getNoteContentByIdFromDb(id :String?) : Result<BaseResponse<NoteContentModel>, Error>
    suspend fun updateReadingProgressFromDb(contentId : String, progressPage : Int, totalPages : Int) : Result<BaseResponse<Boolean>, Error>
    suspend fun createTagOnDB(tag : Tag): Result<BaseResponse<Tag>, Error>
    suspend fun updateTagOnDB(tag : Tag): Result<BaseResponse<Tag>, Error>
    suspend fun deleteTagOnDB(tag : String?): Result<BaseResponse<Boolean>, Error>
    suspend fun getTagsFromDB(): Result<BaseResponse<List<Tag>>, Error>
}