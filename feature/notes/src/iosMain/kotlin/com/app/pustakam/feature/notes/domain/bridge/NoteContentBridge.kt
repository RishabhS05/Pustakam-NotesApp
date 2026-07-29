package com.app.pustakam.bridge

import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.domain.repositories.usecases.SetSelectedNoteContentUseCase
import com.app.pustakam.domain.repositories.usecases.UpdateSelectedMediaContentUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// 🔧 F5: use-case-backed — NO repository import anywhere in iosMain anymore.
//       Public API unchanged (setSelectedNote / updateMediaContent / observeSelectedMedia /
//       dispose) → zero Swift-side impact.
/** Selected-note media state for iOS — routed through use cases (use-cases-only rule). */
class NoteContentBridge : KoinComponent {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val setSelectedNoteUseCase: SetSelectedNoteContentUseCase by inject()
    private val updateMediaUseCase: UpdateSelectedMediaContentUseCase by inject()

    fun setSelectedNote(note: Note) = setSelectedNoteUseCase(note)

    fun updateMediaContent(content: NoteContentModel.MediaContent) =
        updateMediaUseCase(content)

    /** Live media list of the selected note (players/visualizers). */
    fun observeSelectedMedia(onChange: (List<NoteContentModel.MediaContent>) -> Unit): Closeable =
        setSelectedNoteUseCase.selectedMediaContent.watch(scope) { onChange(it) }

    fun dispose() = scope.cancel()
}
