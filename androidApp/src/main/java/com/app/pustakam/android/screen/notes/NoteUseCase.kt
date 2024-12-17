package com.app.pustakam.android.screen.notes

import com.app.pustakam.android.screen.base.BaseUseCase
import com.app.pustakam.data.models.response.notes.Note

class CreateORUpdateNoteUseCase : BaseUseCase() {
    suspend operator  fun invoke(note : Note) =
        getBaseApiCall{repository.insertOrUpdateNote(note)}
}
class DeleteNoteUseCase : BaseUseCase() {
    suspend operator  fun invoke(noteId : String?)
    = getBaseApiCall{ repository.deleteNote(noteId ?: "") }
}

class ReadNoteUseCase : BaseUseCase() {
    suspend operator  fun invoke(id : String?) =
        getBaseApiCall{ repository.getANote(id) }
}

class GetNotesUseCase : BaseUseCase() {
    suspend operator fun invoke(page : Int) =
        getBaseApiCall{ repository.getAllNotes(page) }
}