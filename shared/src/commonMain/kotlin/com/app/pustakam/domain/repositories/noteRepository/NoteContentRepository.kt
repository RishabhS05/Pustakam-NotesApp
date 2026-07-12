package com.app.pustakam.domain.repositories.noteRepository

import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.NoteContentModel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent

class NoteContentRepository  : KoinComponent{
    private val _selectedNoteMediaContent =
        MutableStateFlow<List<NoteContentModel.MediaContent>>(
        value = mutableListOf()
    )
    val selectedNoteMediaContent: SharedFlow<List<NoteContentModel.MediaContent>> = _selectedNoteMediaContent.asStateFlow()
    fun getIndexOfMedia(id : String ): Int{
        return _selectedNoteMediaContent.value.indexOfFirst { id == it.id }
    }
    fun updateNoteContent(note: NoteContentModel.MediaContent){
        _selectedNoteMediaContent.update { currentList ->
            val newList = currentList.toMutableList()
            val indexof = newList.indexOf(note)
            if (indexof == -1) newList.add(note) else newList[indexof] = note
            newList.toList() // Ensure immutable list
        }
    }

    fun addAllNoteContent(note: Note){
        _selectedNoteMediaContent.value = note.contents
            .filterIsInstance<NoteContentModel.MediaContent>()
            .sortedBy { it.position }
    }
    fun clear(){
        _selectedNoteMediaContent.value.toMutableList().clear()
    }
}