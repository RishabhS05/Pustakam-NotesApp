package com.app.pustakam.android.screen.notes

import com.app.pustakam.android.screen.base.NoteBaseUseCase
import com.app.pustakam.data.models.response.notes.Note


class CreateORUpdateNoteUseCase : NoteBaseUseCase() {
    suspend operator fun invoke(note: Note) =
        getBaseApiCall { noteRepository.insertOrUpdateNote(note) }
}

class DeleteNoteUseCase : NoteBaseUseCase() {
    suspend operator fun invoke(noteId: String?) =
        getBaseApiCall { noteRepository.deleteNote(noteId ?: "") }
}

class ReadNoteUseCase : NoteBaseUseCase() {
    suspend operator fun invoke(id: String?) =
        getBaseApiCall { noteRepository.getANote(id) }
}

class GetNotesUseCase : NoteBaseUseCase() {
    suspend operator fun invoke(page: Int) =
        getBaseApiCall { noteRepository.getAllNotes(page) }
}

class DeleteNoteContentUseCase : NoteBaseUseCase() {
    suspend operator fun invoke(id: String?) =
        getBaseApiCall { noteRepository.deleteNoteContentFromDb(id ?: "") }
}