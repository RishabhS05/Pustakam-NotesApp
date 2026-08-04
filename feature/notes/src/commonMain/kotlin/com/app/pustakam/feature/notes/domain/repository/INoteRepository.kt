package com.app.pustakam.feature.notes.domain.repository

import com.app.pustakam.core.common.util.Error
import com.app.pustakam.core.common.util.Result
import com.app.pustakam.core.model.models.BaseResponse
import com.app.pustakam.core.model.models.response.notes.Note
import com.app.pustakam.core.model.models.response.notes.NoteSummary
import com.app.pustakam.core.model.models.response.notes.Notes

interface INoteRepository : ILocalNotesRepository, IRemoteNoteRepository {
    suspend fun insertOrUpdateNote(
        note: Note,
        dirtyContentIds: Set<String>? = null
    ): Result<BaseResponse<Note>, Error>

    suspend fun deleteNote(id: String): Result<BaseResponse<Boolean>, Error>
    suspend fun getANote(id: String?): Result<BaseResponse<Note>, Error>
    suspend fun getNoteSummaries(page: Int, limit: Int): Result<BaseResponse<List<NoteSummary>>, Error>
    suspend fun searchNotes(query: String): Result<BaseResponse<List<NoteSummary>>, Error>
    suspend fun getAllNotes(page: Int = 0, limit: Int = 0): Result<BaseResponse<Notes>, Error>
}
