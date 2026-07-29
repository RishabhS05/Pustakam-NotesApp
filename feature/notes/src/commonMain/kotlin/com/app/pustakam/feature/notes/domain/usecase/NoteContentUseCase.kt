package com.app.pustakam.feature.notes.domain.usecase

import com.app.pustakam.core.model.models.response.notes.Note
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import com.app.pustakam.feature.notes.data.repositoryImpl.NoteContentRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// 🔧 F5: NoteContentRepository is not a BaseRepository, so it gets its own small
//       use-case base — mirror of NoteBaseUseCase's pattern (incl. the state accessor
//       like `notes`/`tags`). These are synchronous state ops: no Flow<Result> wrapping.
abstract class NoteContentBaseUseCase : KoinComponent {
    protected val noteContentRepository: NoteContentRepository by inject()

    /** Selected-note media stream — mirror of NoteBaseUseCase.notes / .tags */
    val selectedMediaContent get() = noteContentRepository.selectedNoteMediaContent
}

/** Load a note's media contents into the selected-note state (editor open). */
class SetSelectedNoteContentUseCase : NoteContentBaseUseCase() {
    operator fun invoke(note: Note) = noteContentRepository.addAllNoteContent(note)
}

/** Add/replace one media item in the selected-note state (capture, recording). */
class UpdateSelectedMediaContentUseCase : NoteContentBaseUseCase() {
    operator fun invoke(content: NoteContentModel.MediaContent) =
        noteContentRepository.updateNoteContent(content)
}

/** Index lookup used by media players (Android PlayerViewModel parity). */
class GetSelectedMediaIndexUseCase : NoteContentBaseUseCase() {
    operator fun invoke(id: String): Int = noteContentRepository.getIndexOfMedia(id)
}
