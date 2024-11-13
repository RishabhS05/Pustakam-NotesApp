package com.app.pustakam.android.screen.notes

import com.app.pustakam.android.usecases.BaseUseCase
import com.app.pustakam.data.models.request.NoteRequest
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.Notes
import com.app.pustakam.domain.repositories.BaseRepository

class CreateNoteUseCase : BaseUseCase() {
    suspend operator  fun invoke(note : NoteRequest) =
        getBaseApiCall{repository.addNewNote(note)}
}
class DeleteNoteUseCase : BaseUseCase() {
    suspend operator  fun invoke(noteId : String) = getBaseApiCall{
        repository.deleteNote( noteId) }
}
class UpdateNoteUseCase : BaseUseCase() {
    suspend operator  fun invoke(note : NoteRequest) = getBaseApiCall{
        repository.updateNote(note)
    }
}

class ReadNoteUseCase : BaseUseCase() {
    suspend operator  fun invoke(id : String ,callApi : Boolean) =
        getBaseApiCall{ repository.getNote(id) }
    operator  fun invoke(id : String ) = repository.getNoteById(id)
}

class GetNotesUseCase : BaseUseCase() {
    suspend operator fun invoke(page : Int) = getBaseApiCall{ repository.getNotesForUser(page) }

    operator fun invoke() : Notes? = repository.getNotes()
}