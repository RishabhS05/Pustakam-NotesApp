package com.app.pustakam.feature.notes.domain.usecase

import com.app.pustakam.core.model.models.response.notes.Note
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import com.app.pustakam.feature.notes.domain.repository.INoteContentRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class NoteContentBaseUseCase : KoinComponent {
    protected val noteContentRepository: INoteContentRepository by inject()

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
