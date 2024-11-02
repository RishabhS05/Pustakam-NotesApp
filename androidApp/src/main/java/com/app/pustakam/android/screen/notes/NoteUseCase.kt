package com.app.pustakam.android.screen.notes

import com.app.pustakam.android.usecases.BaseUseCase
import com.app.pustakam.data.models.request.NoteRequest
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.domain.repositories.BaseRepository

class CreateNoteUseCase : BaseUseCase() {
    suspend operator  fun invoke(note : NoteRequest) = getBaseApiCall{repository.addNewNote(getUserId(), note)}
}
class DeleteNoteUseCase : BaseUseCase() {
    suspend operator  fun invoke(noteId : String) = getBaseApiCall{repository.deleteNote(getUserId(), noteId)}
}
class UpdateNoteUseCase : BaseUseCase() {
    suspend operator  fun invoke(note : NoteRequest) = getBaseApiCall{repository.updateNote(getUserId(), note) }
}

class ReadNoteUseCase : BaseUseCase() {
    suspend operator  fun invoke(note : Note) = getBaseApiCall{ repository.getNote(getUserId(), note._id!!) }
}

class GetNotesUseCase : BaseUseCase() {
    suspend operator  fun invoke(page : Int ) = getBaseApiCall{ repository.getNotesForUser(getUserId(), page) }
}