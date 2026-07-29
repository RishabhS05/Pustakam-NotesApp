package com.app.pustakam.feature.notes.domain

import com.app.pustakam.core.model.models.Tag
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import com.app.pustakam.core.model.models.response.notes.Notes
import com.app.pustakam.feature.notes.data.repositoryImpl.NoteContentRepository
import com.app.pustakam.feature.notes.data.repositoryImpl.NoteRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import com.app.pustakam.core.common.util.log_d


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
// 🔧 30-Jul-2026 02:10 was KoinHelper.get*Repository() (:shared) — both repos live in :feature:notes, so resolve them here and drop the :shared edge
object NoteRepositoryHelper : KoinComponent {
    private val noteRepository = get<NoteRepository>()
    private val noteContentRepository = get<NoteContentRepository>()

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
