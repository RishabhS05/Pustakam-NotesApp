// 🔧 F1: package now matches folder (bridge/) — was bridge.notes, IDE/package mismatch
package com.app.pustakam.feature.notes.domain.bridge

import com.app.pustakam.core.model.models.Tag
import com.app.pustakam.core.model.models.response.notes.Note
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import com.app.pustakam.core.model.models.response.notes.NoteSummary
import com.app.pustakam.core.model.models.response.notes.Notes
import com.app.pustakam.feature.notes.domain.usecase.CreateORUpdateNoteUseCase
import com.app.pustakam.feature.notes.domain.usecase.CreateTagUseCase
import com.app.pustakam.feature.notes.domain.usecase.DeleteNoteContentUseCase
import com.app.pustakam.feature.notes.domain.usecase.DeleteNoteUseCase
import com.app.pustakam.feature.notes.domain.usecase.DeleteTagUseCase
import com.app.pustakam.feature.notes.domain.usecase.GetNoteSummariesUseCase
import com.app.pustakam.feature.notes.domain.usecase.GetNotesUseCase
import com.app.pustakam.feature.notes.domain.usecase.GetTagCase
import com.app.pustakam.feature.notes.domain.usecase.ReadContentUseCase
import com.app.pustakam.feature.notes.domain.usecase.ReadNoteUseCase
import com.app.pustakam.feature.notes.domain.usecase.SearchNotesUseCase
import com.app.pustakam.feature.notes.domain.usecase.UpdateTagUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.app.pustakam.core.common.bridge.BridgeError
import com.app.pustakam.core.common.bridge.Closeable
import com.app.pustakam.core.data.bridge.subscribeTo
import com.app.pustakam.core.data.bridge.watch

/**
 * The ONLY Notes entry point for iOS. One instance per Swift ViewModel.
 *
 * - Use cases arrive via Koin `inject()` (factory scope, lazy → no pre-initKoin crash).
 * - Own scope on Dispatchers.Main → every callback lands on the main thread.
 *   (Heavy work still runs on IO: getBaseApiCall() does flowOn(Dispatchers.IO).)
 * - Every function returns a Closeable; dispose() cancels everything at once.
 */
class NotesBridge : KoinComponent {

    /** Reads + observers: cancelled by dispose() when the screen dies. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private companion object {
        private val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }

    // ---- injected use cases (resolved lazily, per-bridge instances) ----
    private val getNotesUseCase: GetNotesUseCase by inject()
    // 🔧 15-Jul-2026 iOS parity: list summaries + full-text search
    private val getNoteSummariesUseCase: GetNoteSummariesUseCase by inject()
    private val searchNotesUseCase: SearchNotesUseCase by inject()
    private val readNoteUseCase: ReadNoteUseCase by inject()
    private val readContentUseCase: ReadContentUseCase by inject()
    private val upsertNoteUseCase: CreateORUpdateNoteUseCase by inject()
    private val deleteNoteUseCase: DeleteNoteUseCase by inject()
    private val deleteNoteContentUseCase: DeleteNoteContentUseCase by inject()
    private val getTagsUseCase: GetTagCase by inject()
    private val createTagUseCase: CreateTagUseCase by inject()
    private val updateTagUseCase: UpdateTagUseCase by inject()
    private val deleteTagUseCase: DeleteTagUseCase by inject()

    /* ============================ NOTES — one-shot CRUD ============================ */

    /** Page of notes from local DB. List content itself arrives via observeNotes. */
    fun getNotes(
        page: Int,
        onLoading: () -> Unit,
        onSuccess: (Notes?) -> Unit,
        onError: (BridgeError) -> Unit
    ): Closeable = subscribeTo(scope, { getNotesUseCase(page) }, onLoading, onSuccess, onError)

    /**
     * Read one note. Pass null/absent id → repository returns a NEW empty Note
     * (pre-generated id + timestamps) — this is the "create note" entry too.
     */
    fun readNote(
        noteId: String?,
        onLoading: () -> Unit,
        onSuccess: (Note?) -> Unit,
        onError: (BridgeError) -> Unit
    ): Closeable = subscribeTo(scope, { readNoteUseCase(noteId) }, onLoading, onSuccess, onError)

    /** ONE content row by id — the document reader needs the row, not the whole note. */
    fun readContent(
        contentId: String?,
        onLoading: () -> Unit,
        onSuccess: (NoteContentModel?) -> Unit,
        onError: (BridgeError) -> Unit
    ): Closeable = subscribeTo(scope, { readContentUseCase(contentId) }, onLoading, onSuccess, onError)

    // 🔧 15-Jul-2026 iOS parity: one page of LIST-SCREEN summaries (title/snippet/counts/thumbnail)
    //   — contents never load for the list. Content arrives via observeNoteSummaries.
    fun getNoteSummaries(
        page: Int,
        limit: Int,
        onLoading: () -> Unit,
        onSuccess: (List<NoteSummary>?) -> Unit,
        onError: (BridgeError) -> Unit
    ): Closeable = subscribeTo(scope, { getNoteSummariesUseCase(page, limit) }, onLoading, onSuccess, onError)

    // 🔧 15-Jul-2026 iOS parity: FTS5 search across note text + titles (results carry a snippet).
    fun searchNotes(
        query: String,
        onLoading: () -> Unit,
        onSuccess: (List<NoteSummary>?) -> Unit,
        onError: (BridgeError) -> Unit
    ): Closeable = subscribeTo(scope, { searchNotesUseCase(query) }, onLoading, onSuccess, onError)

    /** Insert or update (repo decides by id) + pushes into notesState → list auto-refreshes. */
    fun createOrUpdateNote(
        note: Note,
        onLoading: () -> Unit,
        onSuccess: (Note?) -> Unit,
        onError: (BridgeError) -> Unit
    ): Closeable = subscribeTo(writeScope, { upsertNoteUseCase(note) }, onLoading, onSuccess, onError) // 🔧 write survives dispose()

    // 🔧 15-Jul-2026 iOS parity (dirty-row saves): writes ONLY the given content rows + the note
    //   header, instead of rewriting every row on each save. Same overload Android uses.
    fun createOrUpdateNote(
        note: Note,
        dirtyContentIds: Set<String>,
        onLoading: () -> Unit,
        onSuccess: (Note?) -> Unit,
        onError: (BridgeError) -> Unit
    ): Closeable = subscribeTo(writeScope, { upsertNoteUseCase(note, dirtyContentIds) }, onLoading, onSuccess, onError)

    /** Delete by id. Success payload is Boolean → Swift sees KotlinBoolean?. */
    fun deleteNote(
        noteId: String?,
        onLoading: () -> Unit,
        onSuccess: (Boolean?) -> Unit,
        onError: (BridgeError) -> Unit
    ): Closeable = subscribeTo(writeScope, { deleteNoteUseCase(noteId) }, onLoading, onSuccess, onError) // 🔧 write survives dispose()

    /** Delete a single content block of a note (note itself stays). */
    fun deleteNoteContent(
        contentId: String?,
        onLoading: () -> Unit,
        onSuccess: (Boolean?) -> Unit,
        onError: (BridgeError) -> Unit
    ): Closeable = subscribeTo(writeScope, { deleteNoteContentUseCase(contentId) }, onLoading, onSuccess, onError) // 🔧 write survives dispose()

    /* ============================ TAGS — one-shot CRUD ============================ */

    /** All tags. NOTE: empty DB currently yields NOT_FOUND error (repo behavior kept as-is). */
    fun getTags(
        onLoading: () -> Unit,
        onSuccess: (List<Tag>?) -> Unit,
        onError: (BridgeError) -> Unit
    ): Closeable = subscribeTo(scope, { getTagsUseCase() }, onLoading, onSuccess, onError)

    fun createTag(
        tag: Tag,
        onLoading: () -> Unit,
        onSuccess: (Tag?) -> Unit,
        onError: (BridgeError) -> Unit
    ): Closeable = subscribeTo(writeScope, { createTagUseCase(tag) }, onLoading, onSuccess, onError) // 🔧 write survives dispose()

    fun updateTag(
        tag: Tag,
        onLoading: () -> Unit,
        onSuccess: (Tag?) -> Unit,
        onError: (BridgeError) -> Unit
    ): Closeable = subscribeTo(writeScope, { updateTagUseCase(tag) }, onLoading, onSuccess, onError) // 🔧 write survives dispose()

    fun deleteTag(
        tagId: String?,
        onLoading: () -> Unit,
        onSuccess: (Boolean?) -> Unit,
        onError: (BridgeError) -> Unit
    ): Closeable = subscribeTo(writeScope, { deleteTagUseCase(tagId) }, onLoading, onSuccess, onError) // 🔧 write survives dispose()

    /* ===================== REACTIVE STATE (replaces NoteRepositoryHelper) ===================== */

    /** Live notes list — fires on every insert/update/delete anywhere in the app. */
    fun observeNotes(onChange: (Notes) -> Unit): Closeable =
        getNotesUseCase.notes.watch(scope) { onChange(it) }

    fun observeNoteSummaries(onChange: (List<NoteSummary>) -> Unit): Closeable =
        getNoteSummariesUseCase.noteSummaries.watch(scope) { onChange(it) }

    /** Live tag list — requires the 2-line `tags` accessor from §1. */
    fun observeTags(onChange: (List<Tag>) -> Unit): Closeable =
        getTagsUseCase.tags.watch(scope){onChange(it)}

    /* ======================================================================================== */

    /** Cancel every observer + in-flight call started by this bridge. Swift: call from deinit. */
    fun dispose() = scope.cancel()
}