package com.app.pustakam.domain

import com.app.pustakam.data.models.Tag
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.data.models.response.notes.Notes
import com.app.pustakam.koinDI.KoinHelper
import com.app.pustakam.util.log_d


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

import kotlinx.coroutines.launch

// 🔧 P0/4: superseded by NotesBridge (observeNotes/observeTags) and
//   NoteContentBridge (observeSelectedMedia) — cancellable observers, use-case-backed.
//   This helper leaked never-cancelled collectors. Zero Swift callers remain.
//   Kept until you approve deletion.
@Deprecated("Use NotesBridge / NoteContentBridge observers instead")
object NoteRepositoryHelper {
    @Suppress("DEPRECATION")
    private val noteRepository = KoinHelper.getNoteRepository()
    @Suppress("DEPRECATION")
    private val noteContentRepository = KoinHelper.getNoteContentRepository()

    fun <T> Flow<T>.collectObserver (callback :(T)-> Unit) {
        CoroutineScope(Dispatchers.Main).launch{
        collect {
            callback(it)
        }
        }
    }
    fun <T> Flow<T>.collectLatestObserver (callback :(T)-> Unit) {
        CoroutineScope(Dispatchers.Main).launch{
            collectLatest {
                callback(it)
            }
        }
    }
    fun noteListStateHelper(callback : (Notes) -> Unit) {
//        observerCallback<Notes>(noteRepository.notesState, callback = callback)
        CoroutineScope(Dispatchers.Main).launch {
            noteRepository.notesState.collectLatest{
                log_d("NoteRepositoryHelper notes list ",it.notes)
                log_d("NoteRepositoryHelper notes count ",it.notes.size)
                callback(it)
            }
        }
    }
    fun noteContentMediaList(callback: (List<NoteContentModel.MediaContent>) -> Unit){
//        observerCallback<List<NoteContentModel.MediaContent>>(noteContentRepository.selectedNoteMediaContent, callback = callback)
        CoroutineScope(Dispatchers.Main).launch {
            noteContentRepository.selectedNoteMediaContent.collectLatest{
                callback(it)
            }
        }
    }
        fun getTagsHelper(callback: (List<Tag>) -> Unit) {
//            observerCallback<List<Tag>>(noteRepository.tagState, callback = callback)
            CoroutineScope(Dispatchers.Main).launch {
                noteRepository.tagState.collectLatest{
                    callback(it)
                }
            }
        }
    }
