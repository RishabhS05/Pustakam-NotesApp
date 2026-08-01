package com.app.pustakam.feature.notes.domain.usecase

import com.app.pustakam.core.model.models.response.notes.Note
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import com.app.pustakam.feature.notes.data.repositoryImpl.NoteContentRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

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

// 📖 23-Jul-2026: persist the reader's position onto a document's media row. Mirrors
//   DeleteNoteContentUseCase exactly (same base, same getBaseApiCall wrapper, same repo → DAO flow).
class UpdateReadingProgressUseCase : NoteBaseUseCase() {
    suspend operator fun invoke(contentId: String?, progressPage: Int, totalPages: Int) =
        getBaseApiCall { noteRepository.updateReadingProgressFromDb(contentId ?: "", progressPage, totalPages) }
}
class ReadContentUseCase : NoteBaseUseCase() {
    suspend operator fun invoke(contentId: String?) =
        getBaseApiCall { noteRepository.getNoteContentByIdFromDb(contentId) }
}
class DeleteNoteContentUseCase : NoteBaseUseCase() {
    suspend operator fun invoke(id: String?) =
        getBaseApiCall { noteRepository.deleteNoteContentFromDb(id ?: "") }
}
