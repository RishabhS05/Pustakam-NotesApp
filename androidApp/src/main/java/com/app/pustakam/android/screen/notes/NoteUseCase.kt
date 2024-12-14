package com.app.pustakam.android.screen.notes

import com.app.pustakam.android.screen.base.BaseUseCase
import com.app.pustakam.data.models.response.notes.Note

class CreateORUpdateNoteUseCase : BaseUseCase() {
    suspend operator  fun invoke(note : Note) =
        getBaseApiCall{repository.upsertNewNoteApi(note)}
}
class DeleteNoteUseCase : BaseUseCase() {
    suspend operator  fun invoke(noteId : String?) = getBaseApiCall{
        repository.deleteNoteApi(noteId)
    }
}

class ReadNoteUseCase : BaseUseCase() {
    suspend operator  fun invoke(id : String) =
        getBaseApiCall{ repository.getNoteApi(id) }
}

class GetNotesUseCase : BaseUseCase() {
    suspend operator fun invoke(page : Int) = getBaseApiCall{ repository.getNotesForUserApi(page) }
}