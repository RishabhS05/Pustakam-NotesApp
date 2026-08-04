package com.app.pustakam.feature.notes.domain.repository

import com.app.pustakam.core.model.models.response.notes.Note
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import kotlinx.coroutines.flow.SharedFlow

interface INoteContentRepository {
    val selectedNoteMediaContent: SharedFlow<List<NoteContentModel.MediaContent>>
    fun getIndexOfMedia(id: String): Int
    fun updateNoteContent(note: NoteContentModel.MediaContent)
    fun addAllNoteContent(note: Note)
    fun clear()
}
