package com.app.pustakam.domain.repositories.noteContentRepo

import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.util.ContentType

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update

class NoteContentRepository {
    private val _selectedNote = MutableStateFlow<List<NoteContentModel.MediaContent>>(
        value = mutableListOf()
    )
    val selectedNote: SharedFlow<List<NoteContentModel.MediaContent>> = _selectedNote.asSharedFlow()
    fun getIndexOfMedia(id : String ): Int{
        return _selectedNote.value.indexOfFirst { id == it.id }
    }
    fun updateNoteContent(note: NoteContentModel.MediaContent){
        _selectedNote.update { currentList ->
            val newList = currentList.toMutableList()
            val indexof = newList.indexOf(note)
            if (indexof == -1) newList.add(note) else newList[indexof] = note
            newList.toList() // Ensure immutable list
        }
    }

    fun addAllNoteContent(note: Note){
        _selectedNote.value = note.content?.filterIsInstance<NoteContentModel.MediaContent>()
            ?.sortedBy { it.position }?.toMutableList()?: mutableListOf()
    }
    fun clear(){
        _selectedNote.value.toMutableList().clear()
    }
}