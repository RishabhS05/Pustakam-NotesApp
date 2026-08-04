package com.app.pustakam.android.screen.noteEditor

import com.app.pustakam.core.common.util.displayMessage
import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.viewModelScope
import com.app.pustakam.android.fileUtils.deleteFile
import com.app.pustakam.android.fileUtils.generateThumbnail
import com.app.pustakam.android.noteContentProvider.addContent
import com.app.pustakam.android.permission.NeededPermission
import com.app.pustakam.android.screen.NOTES_CODES
import com.app.pustakam.android.screen.NoteContentUiState
import com.app.pustakam.android.screen.NoteUIState
import com.app.pustakam.android.screen.TaskCode
import com.app.pustakam.android.screen.base.BaseViewModel
import com.app.pustakam.feature.notes.domain.usecase.CreateORUpdateNoteUseCase
import com.app.pustakam.feature.notes.domain.usecase.DeleteNoteContentUseCase
import com.app.pustakam.feature.notes.domain.usecase.DeleteNoteUseCase
import com.app.pustakam.feature.notes.domain.usecase.ReadNoteUseCase
import com.app.pustakam.core.model.models.BaseResponse
import com.app.pustakam.core.model.models.response.notes.Note
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import com.app.pustakam.core.model.models.response.notes.NoteContentObjectHelper
import com.app.pustakam.core.model.models.response.notes.TextBlockSplitter
import com.app.pustakam.feature.notes.domain.usecase.ClearSelectedNoteContentUseCase
import com.app.pustakam.feature.notes.domain.usecase.SetSelectedNoteContentUseCase
import com.app.pustakam.feature.notes.domain.usecase.UpdateSelectedMediaContentUseCase
import com.app.pustakam.core.common.extensions.isNotnull
import com.app.pustakam.core.common.util.ContentType
import com.app.pustakam.core.common.util.ContentType.AUDIO
import com.app.pustakam.core.common.util.ContentType.IMAGE
import com.app.pustakam.core.common.util.ContentType.LOCATION
import com.app.pustakam.core.common.util.ContentType.TEXT
import com.app.pustakam.core.common.util.ContentType.VIDEO
import com.app.pustakam.core.common.util.Error
import com.app.pustakam.core.common.util.NetworkError
import com.app.pustakam.core.common.util.log_d
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import com.app.pustakam.core.common.util.Result

class NoteEditorViewModel : BaseViewModel() {
    private val setSelectedNoteContentUseCase by inject<SetSelectedNoteContentUseCase>()
    private val updateSelectedMediaContentUseCase by inject<UpdateSelectedMediaContentUseCase>()
    private val clearSelectedNoteContentUseCase by inject<ClearSelectedNoteContentUseCase>()
    private val readNoteUseCase by inject<ReadNoteUseCase>()
    private val deleteNoteUseCase by inject<DeleteNoteUseCase>()
    private val deleteNoteContentUseCase by inject<DeleteNoteContentUseCase>()
    private val createUpdateNoteUseCase by inject<CreateORUpdateNoteUseCase>()
    private val _noteUiState = MutableStateFlow(NoteUIState(isLoading = false))
    val noteUIState: StateFlow<NoteUIState> = _noteUiState.asStateFlow()
    private val _noteContentUiState = MutableStateFlow(NoteContentUiState())
    val noteContentUiState: StateFlow<NoteContentUiState> = _noteContentUiState.asStateFlow()
    private val dirtyContentIds = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            setSelectedNoteContentUseCase.selectedMediaContent.collect { media ->
                media.forEach { applyExternalContentUpdate(it) }
            }
        }
    }

    private fun applyExternalContentUpdate(updated: NoteContentModel) {
        _noteContentUiState.update { state ->
            val index = state.contents.indexOfFirst { it.id == updated.id }
            if (index == -1) return@update state
            if (state.contents[index].updatedAt == updated.updatedAt) return@update state
            state.contents[index] = updated
            val updatedNote = state.note?.let { n ->
                n.withContents(n.contents.map { if (it.id == updated.id) updated else it })
            }
            state.copy(note = updatedNote, contents = state.contents)
        }
    }

    private var lastSavedDirtyIds: Set<String> = emptySet()


    //default methods
    override fun onLoading(taskCode: TaskCode) {
        _noteUiState.update {
            it.copy(isLoading = true, successMessage = "")
        }
    }

    override fun onSuccess(taskCode: TaskCode, result: Result.Success<BaseResponse<*>>) {
        when (taskCode) {
            NOTES_CODES.INSERT -> {
                log_d("Loading", "Getting Update data")
                val note = result.data.data as Note
                _noteUiState.update {
                    val noteStatus = when (it.noteStatus) {
                        NoteStatus.onBackPress, NoteStatus.onSaveCompletedExit -> NoteStatus.onSaveCompletedExit
                        else -> NoteStatus.onSaveCompleted
                    }
                    it.copy(noteStatus = noteStatus, isLoading = false)
                }

                _noteContentUiState.update { it.copy(note = note,
                    isAllSetupDone = true) }

                dirtyContentIds.removeAll(lastSavedDirtyIds)
                lastSavedDirtyIds = emptySet()
                consumePendingMediaPaths()
            }

            NOTES_CODES.READ -> {
                val note = result.data.data as Note
                if (!noteContentUiState.value.isAllSetupDone) _noteContentUiState.update {
                    it.titleTextState.value = note.title ?: ""
                    it.copy(
                        titleTextState = it.titleTextState, note = note,
                        isAllSetupDone = true,

                        contents = mutableStateListOf(*note.contents.sortedBy { c -> c.position }.toTypedArray())
                    )
                }

                setSelectedNoteContentUseCase(_noteContentUiState.value.note ?: note)

                consumePendingMediaPaths()
            }

            NOTES_CODES.DELETE -> {
                _noteUiState.update {
                    it.copy(
                        isLoading = false, noteStatus = NoteStatus.exit
                    )
                }
            }
        }
    }

    override fun onFailure(taskCode: TaskCode, error: Error) {
        when (taskCode) {
            NOTES_CODES.READ -> {
                _noteUiState.update {
                    it.copy(
                        isLoading = false, error = error.displayMessage(), noteStatus = NoteStatus.onSaveCompletedExit
                    )
                }
            }

            else -> _noteUiState.update {
                it.copy(isLoading = false, error = error.displayMessage(), noteStatus = null)
            }
        }
    }


    override fun clearError() {
        _noteUiState.update {
            it.copy(
                error = null, successMessage = null, isLoading = false
            )
        }
    }

    /** CRUD operations on Notes */

    // call make a wish api
    fun isNoteValid(): Boolean =
        if (_noteContentUiState.value.titleTextState.value.isEmpty()) {
            if (_noteContentUiState.value.note?.contents?.isEmpty() == true) {
                 false
            } else  true
        } else true

    fun createOrUpdateNote() {
        if(!isNoteValid()) {
                changeNoteStatus(NoteStatus.exit)
                return
        }
        updateNoteObject()
        // 🔧 15-Jul-2026 Phase 0.4: snapshot the dirty ids for THIS save; cleared on INSERT success.
        lastSavedDirtyIds = dirtyContentIds.toSet()
        makeAWish(NOTES_CODES.INSERT) {
            createUpdateNoteUseCase.invoke(_noteContentUiState.value.note!!, lastSavedDirtyIds)
        }
    }
    fun saveThenOpen(onSaved: () -> Unit) {
        updateNoteObject()
        val note = _noteContentUiState.value.note ?: run { onSaved(); return }
        val dirty = dirtyContentIds.toSet()
        viewModelScope.launch(Dispatchers.IO) {
            createUpdateNoteUseCase.invoke(note, dirty).collect { result ->
                if (result is Result.Success || result is Result.Error) {
                    dirtyContentIds.removeAll(dirty)
                    withContext(Dispatchers.Main) { onSaved() }
                }
            }
        }
    }

    fun readFromDataBase(id: String?) {
        if (!id.isNullOrEmpty()) _noteUiState.update {
            it.copy(showDeleteButton = true)
        }
        makeAWish(NOTES_CODES.READ) {
            readNoteUseCase.invoke(id)
        }
    }

    fun refreshOnResume(id: String?) {
        if (dirtyContentIds.isNotEmpty()) return   // don't overwrite unsaved edits
        readFromDataBase(id)
    }

    fun deleteNote(noteId: String) {
        makeAWish(NOTES_CODES.DELETE) {
            deleteNoteUseCase.invoke(noteId)
        }
    }

    private fun updateNoteObject() {
        TextBlockSplitter.splitOversized(_noteContentUiState.value.contents.toList())?.let { split ->
            dirtyContentIds.addAll(split.changedIds)
            val live = _noteContentUiState.value.contents
            live.clear()
            live.addAll(split.contents)
        }
        _noteContentUiState.update {
            val updatedNote = it.note?.withTitleAndContents(
                newTitle = it.titleTextState.value,
                newContents = it.contents.toList()
            )
            it.copy(note = updatedNote)
        }
    }


    //action status methods
    fun changeNoteStatus(status: NoteStatus?) {
        _noteUiState.update {
            it.copy(
                noteStatus = status, isLoading = false
            )
        }
    }

    private fun setContentType(contentType: ContentType) {
        _noteUiState.update {
            it.copy(contentType = contentType, isLoading = true)
        }
    }

    //dialog trigger methods
    fun showDeleteAlertBox(value: Boolean, deleteNoteContentId: String? = null) {
        // some time it doesn't update a single value
        _noteUiState.update { it.copy(showDeleteAlert = value, deleteNoteContentId = deleteNoteContentId, isLoading = false) }
    }

    fun showPermissionAlert(value: Boolean?) {
        _noteUiState.update {
            it.copy(showPermissionAlert = value)
        }
    }

    //hardware permission logic
    @SuppressLint("NewApi")
    private fun getPermissions(contentType: ContentType?) = when (contentType) {
        VIDEO -> listOf(NeededPermission.CAMERA, NeededPermission.RECORD_AUDIO)
        AUDIO -> listOf(NeededPermission.RECORD_AUDIO)
        IMAGE -> listOf(NeededPermission.CAMERA)
        LOCATION -> listOf(NeededPermission.COARSE_LOCATION, NeededPermission.FINE_LOCATION,
            NeededPermission.BACKGROUND_LOCATION)
        else -> listOf(NeededPermission.POST_NOTIFICATIONS)
    }

   /**permission dialog setup*/
    fun preparePermissionDialog(contentType: ContentType? = null) {
        val permission = getPermissions(contentType)
        _noteUiState.update {
            it.copy(permissions = permission, contentType = contentType, showPermissionAlert = true, isLoading = false)
        }
    }
      fun addNewText() {
          val note = _noteContentUiState.value.note!!
          if (note.isNotnull()) {
              val textContent =
                  NoteContentObjectHelper.createText(
                      noteId = note.id,
                      positionedAt = note.contents.count().toDouble()   // 🔧 C4
                  )
              setContentType(TEXT)
              updateContent(content = textContent)
          }
      }

    fun locationState(value: Boolean ) {
        _noteUiState.update { it.copy(LocationState = value) }
    }
    fun updateContent(index: Int = -1, content: NoteContentModel) {
        dirtyContentIds.add(content.id)   // 🔧 15-Jul-2026 Phase 0.4: touched → will be saved
        if(index== -1) {
            addContentData(content)
        }else{
            _noteContentUiState.update {
                it.contents[index] = content
                // immutable Note: upsert by id via copy (was: in-place add — also fixed
                // the old bug of ADDING a duplicate on update instead of replacing)
                val updatedNote = it.note?.let { n ->
                    val newContents = n.contents.toMutableList()
                    val i = newContents.indexOfFirst { c -> c.id == content.id }
                    if (i != -1) newContents[i] = content else newContents.add(content)
                    n.withContents(newContents)
                }
                it.copy(note = updatedNote, contents = it.contents, isAllSetupDone = true)
            }
        }
        if (content.isPlayingMedia())
            updateSelectedMediaContentUseCase(content as NoteContentModel.MediaContent)
    }
    /**content logic
     * Add new content to the note content list
     * by selecting it type on the bases of user selection
     * */
    fun addNewContent(context: Context, contentType: ContentType): NoteContentModel {
        setContentType(contentType)
        val content = addContent( context = context, note = _noteContentUiState.value.note!!, contentType = contentType)
        return content
    }
    fun addContentData(content: NoteContentModel){
        dirtyContentIds.add(content.id)

        val liveContents = _noteContentUiState.value.contents
        val existingIndex = liveContents.indexOfFirst { it.id == content.id }
        if (existingIndex != -1) liveContents[existingIndex] = content else liveContents.add(content)
        _noteContentUiState.update {
            val updatedNote = it.note?.let { n ->
                val newContents = n.contents.toMutableList()
                val i = newContents.indexOfFirst { c -> c.id == content.id }
                if (i != -1) newContents[i] = content else newContents.add(content)
                n.withContents(newContents)
            }
            it.copy(note = updatedNote, contents = it.contents, isAllSetupDone = true)
        }
    }

    fun startStopAudioRecording(value: Boolean = true) {
        _noteUiState.update { it.copy(isLoading = false, showAudioRecorder = value) }
    }
/**
 * Remove a note content for note
 * */
    fun removeContent(value: String) {
        dirtyContentIds.remove(value)   // 🔧 15-Jul-2026 Phase 0.4: deleted → nothing to save
        val find = _noteContentUiState.value.note?.contents?.find { value == it.id }
        viewModelScope.launch(Dispatchers.IO) {
            var rowDeleted = false
            deleteNoteContentUseCase.invoke(value).collect { result ->
                when (result) {
                    is Result.Success -> rowDeleted = true
                    is Result.Error -> log_d("NoteEditor", "content delete failed: ${result.error}")
                    else -> Unit
                }
            }

            if (rowDeleted && find?.isMediaFile() == true) {
                find as NoteContentModel.MediaContent
                find.localPath?.let { deleteFile(filePath = it) }
                // 🔧 15-Jul-2026 (iOS-parity cleanup): the media's thumbnail file goes with it
                find.thumbnailPath?.let { deleteFile(filePath = it) }
            }
        }
        _noteContentUiState.update {
            val indexContent = it.contents.indexOf(find)
            if (indexContent != -1) it.contents.removeAt(indexContent)
            // immutable Note: rebuild contents without the removed item.
            // (old code removed from a discarded copy — note.contents was never
            //  actually updated; this also fixes that silent bug)
            val updatedNote = it.note?.let { n ->
                n.withContents(n.contents.filterNot { c -> c.id == value })
            }
            it.copy(note = updatedNote, contents = it.contents)
        }
        showDeleteAlertBox(false, null)
    }


 /**
  * Share note with others
  */
    fun shareNote(){}

    override fun onCleared() {
        super.onCleared()
        clearSelectedNoteContentUseCase()
    }

    // 🔧 14-Jul-2026: CRASH FIX + PERF — this used to run inside composition and mutate
    //   the snapshot list INSIDE StateFlow.update{} (which can re-run its lambda), producing
    //   duplicate items and, with several images at once, a crash. Now it is pure: we build
    //   ALL new MediaContent items first, mutate the SnapshotStateList exactly once (outside
    //   the update lambda), then do a single state copy. Callers invoke it from a LaunchedEffect.
    //
    // 🔧 14-Jul-2026: FIX (I-1, video lost after Stop→Back) — the old `note ?: return` silently
    //   DROPPED the captured paths when the note hadn't loaded yet (the View clears them right
    //   after this call). Paths arriving too early are now stashed in `pendingMediaPaths` and
    //   consumed as soon as READ/INSERT delivers the note — nothing is ever lost.
    private var pendingMediaPaths: List<Pair<String, ContentType>> = emptyList()

    private fun consumePendingMediaPaths() {
        if (pendingMediaPaths.isEmpty()) return
        val pending = pendingMediaPaths
        pendingMediaPaths = emptyList()
        getMediaData(pending)
    }

    // 🔧 15-Jul-2026 Phase 2.3: application context for async thumbnail generation (safe to hold —
    //   never an Activity). Set by getMediaData; used again when pending paths are consumed.
    private var appContext: Context? = null

    fun getMediaData(list: List<Pair<String, ContentType>>, context: Context? = null) {
        if (context != null) appContext = context.applicationContext
        if (list.isEmpty()) return
        val currentState = _noteContentUiState.value
        val note = currentState.note ?: run {
            pendingMediaPaths = pendingMediaPaths + list   // 🔧 stash instead of dropping
            return
        }
        var position: Double = note.contents.count().toDouble()   // 🔧 C4
        val newItems = list.map { path ->
            val content = NoteContentObjectHelper.createMedia(
                positionedAt = position,
                noteId = note.id,
                localPath = path.first,
                contentType = path.second
            ).copy(title = "${path.second}-$position")
            position += 1.0
            content
        }
        // Single mutation of the observed list (safe: called off the composition pass).
        currentState.contents.addAll(newItems)
        _noteContentUiState.update {
            it.copy(
                note = note.withContents(note.contents + newItems),
                contents = it.contents,
                isAllSetupDone = true
            )
        }
        newItems.filter { it.isPlayingMedia() }.forEach { updateSelectedMediaContentUseCase(it) }

        dirtyContentIds.addAll(newItems.map { it.id })
        generateThumbnailsFor(newItems)
    }

    fun importDeviceFiles(context: Context, uris: List<android.net.Uri>) {
        if (uris.isEmpty()) return
        appContext = context.applicationContext
        val note = _noteContentUiState.value.note ?: run {
            _noteUiState.update { it.copy(error = "Note is still loading. Try again.") }; return
        }
        _noteUiState.update { it.copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val items = com.app.pustakam.android.fileimport.FileImportManager.importUris(
                context.applicationContext, note.id, note.contents.count().toDouble(), uris
            )
           withContext(Dispatchers.Main) {
                _noteUiState.update { it.copy(isLoading = false) }
                if (items.isEmpty()) _noteUiState.update { it.copy(error = "Couldn't import the selected files.") }
                else addImportedContents(items)
            }
        }
    }


    fun importFromLink(context: Context, url: String) {
        if (url.isBlank()) return
        appContext = context.applicationContext
        val note = _noteContentUiState.value.note ?: run {
            _noteUiState.update { it.copy(error = "Note is still loading. Try again.") }; return
        }
        _noteUiState.update { it.copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val result = com.app.pustakam.android.fileimport.FileImportManager.importFromUrl(
                context.applicationContext, note.id, note.contents.count().toDouble(), url
            )
            withContext(Dispatchers.Main) {
                _noteUiState.update { it.copy(isLoading = false) }
                when (result) {
                    is com.app.pustakam.android.fileimport.ImportResult.Success -> addImportedContents(result.contents)
                    is com.app.pustakam.android.fileimport.ImportResult.NoFileFound ->
                        _noteUiState.update { it.copy(error = "No file found at this link.") }
                    is com.app.pustakam.android.fileimport.ImportResult.Failed ->
                        _noteUiState.update { it.copy(error = result.message) }
                }
            }
        }
    }

    // 🔧 18-Jul-2026: same safe mutation pattern as getMediaData — list touched ONCE outside update{}
    private fun addImportedContents(items: List<NoteContentModel.MediaContent>) {
        if (items.isEmpty()) return
        val currentState = _noteContentUiState.value
        val note = currentState.note ?: return
        currentState.contents.addAll(items)
        _noteContentUiState.update {
            it.copy(note = note.withContents(note.contents + items), contents = it.contents, isAllSetupDone = true)
        }
        items.filter { it.isPlayingMedia() }.forEach { updateSelectedMediaContentUseCase(it) }
        dirtyContentIds.addAll(items.map { it.id })   // 🔧 imported blocks are new rows → saved next save
        generateThumbnailsFor(items)
    }
    private fun generateThumbnailsFor(items: List<NoteContentModel.MediaContent>) {
        val context = appContext ?: return
        items.filter {
            it.thumbnailPath.isNullOrEmpty() && !it.localPath.isNullOrEmpty() &&
                    (it.type == VIDEO || it.type == IMAGE || it.type == ContentType.GIF)
        }.forEach { media ->
            viewModelScope.launch(Dispatchers.IO) {
                val thumb = generateThumbnail(context, media.localPath!!, media.type) ?: return@launch
                val latest = _noteContentUiState.value.contents
                    .filterIsInstance<NoteContentModel.MediaContent>()
                    .firstOrNull { it.id == media.id } ?: media
                updateContent(content = latest.copy(thumbnailPath = thumb))
            }
        }
    }
}