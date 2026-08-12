package com.app.pustakam.feature.notes.data.repositoryImpl

import com.app.pustakam.core.model.models.response.notes.Note
import com.app.pustakam.core.model.models.response.notes.NoteContentModel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.app.pustakam.feature.notes.domain.repository.INoteContentRepository
import org.koin.core.component.KoinComponent

internal class NoteContentRepository : KoinComponent, INoteContentRepository {
    private val _selectedNoteMediaContent =
        MutableStateFlow<List<NoteContentModel.MediaContent>>(
        value = mutableListOf()
    )
    override val selectedNoteMediaContent: SharedFlow<List<NoteContentModel.MediaContent>> = _selectedNoteMediaContent.asStateFlow()
    override fun getIndexOfMedia(id : String ): Int{
        return _selectedNoteMediaContent.value.indexOfFirst { id == it.id }
    }
    override fun updateNoteContent(note: NoteContentModel.MediaContent){
        _selectedNoteMediaContent.update { currentList ->
            val newList = currentList.toMutableList()
            val indexof = newList.indexOfFirst { it.id == note.id }
            if (indexof == -1) newList.add(note) else newList[indexof] = note
            newList.toList() // Ensure immutable list
        }
    }

    override fun addAllNoteContent(note: Note){
        _selectedNoteMediaContent.value = note.contents
            .filterIsInstance<NoteContentModel.MediaContent>()
            .sortedBy { it.position }
    }
    override fun clear(){
        _selectedNoteMediaContent.value = emptyList()
    }
}