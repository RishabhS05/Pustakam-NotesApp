package com.app.pustakam.domain.repositories.noteRepository

import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.Tag
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.Notes

interface ILocalNotesRepository {
    suspend fun insertUpdateFromDb(note: Note) : Result<BaseResponse<Note>, Error>
    suspend fun deleteNoteByIdFromDb(id : String?) : Result<BaseResponse<Boolean>, Error>
    // 🔧 15-Jul-2026 Phase 0.1: limit added — 0 = load everything (legacy), > 0 = one page
    suspend fun getNotesFromDb(page: Int, limit: Int = 0) : Result<BaseResponse<Notes>, Error>
    suspend fun getNoteByIdFromDb(id :String?) : Result<BaseResponse<Note>, Error>
    suspend fun deleteNoteContentFromDb(id :String? ) : Result<BaseResponse<Boolean>, Error>
    // 📖 23-Jul-2026: persist reading progress onto a document's media row (mirror of delete flow)
    suspend fun updateReadingProgressFromDb(contentId : String, progressPage : Int, totalPages : Int) : Result<BaseResponse<Boolean>, Error>
    suspend fun createTagOnDB(tag : Tag): Result<BaseResponse<Tag>, Error>
    suspend fun updateTagOnDB(tag : Tag): Result<BaseResponse<Tag>, Error>
    suspend fun deleteTagOnDB(tag : String?): Result<BaseResponse<Boolean>, Error>
    suspend fun getTagsFromDB(): Result<BaseResponse<List<Tag>>, Error>
}