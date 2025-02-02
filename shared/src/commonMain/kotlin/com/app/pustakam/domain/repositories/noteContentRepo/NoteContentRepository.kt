package com.app.pustakam.domain.repositories.noteContentRepo

import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.util.ContentType

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class NoteContentRepository {
    private val _selectedNote = MutableStateFlow<MutableList<NoteContentModel>>(
        value = mutableListOf()
    )
    val selectedNote: SharedFlow<MutableList<NoteContentModel>> = _selectedNote.asSharedFlow()
    fun addNoteContent(note: NoteContentModel) {
        _selectedNote.value.add(note)
    }
    fun updateNoteContent(index : Int ,note: NoteContentModel){
        _selectedNote.apply {
            this.value.set(index,note)
        }
    }

    fun addAllNoteContent(note: Note){
        _selectedNote.value = note.content?.filter { it.type == ContentType.AUDIO || it.type == ContentType.VIDEO}
            ?.sortedBy { it.position }?.toMutableList()?: mutableListOf()
    }
    fun clear(){
        _selectedNote.value.clear()
    }
}