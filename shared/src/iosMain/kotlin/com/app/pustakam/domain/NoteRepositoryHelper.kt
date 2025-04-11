package com.app.pustakam.domain

import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.data.models.response.notes.Notes
import com.app.pustakam.koinDI.KoinHelper
import com.app.pustakam.util.log_d
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object NoteRepositoryHelper {
    private val noteRepository = KoinHelper.getNoteRepository()
    private val noteContentRepository = KoinHelper.getNoteContentRepository()
    fun noteListStateHelper(callback : (Notes) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            noteRepository.notesState.collectLatest {
                log_d("notes list ",it.notes)
                callback(it)

            }
        }
    }

    fun noteContentRepository(callback: (List<NoteContentModel.MediaContent>) -> Unit){
        CoroutineScope(Dispatchers.Main).launch {
            noteContentRepository.selectedNote.collectLatest {
                callback(it)
            }
        }
    }
}