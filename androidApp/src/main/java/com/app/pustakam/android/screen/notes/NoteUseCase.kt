package com.app.pustakam.android.screen.notes

import com.app.pustakam.android.usecases.BaseUseCase
import com.app.pustakam.data.models.request.NoteRequest
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.domain.repositories.BaseRepository

class CreateNoteUseCase : BaseUseCase() {
    suspend operator  fun invoke(note : NoteRequest) = getBaseApiCall{repository.addNewNote("", note)}
}
class DeleteNoteUseCase : BaseUseCase() {
    suspend operator  fun invoke(noteId : String) = getBaseApiCall{repository.deleteNote("", noteId)}
}
class UpdateNoteUseCase : BaseUseCase() {
    suspend operator  fun invoke(note : NoteRequest) = getBaseApiCall{repository.updateNote("", note) }
}

class ReadNoteUseCase : BaseUseCase() {
    suspend operator  fun invoke(note : Note) = getBaseApiCall{ repository.getNote("", note._id!!) }
}

class GetNotesUseCase : BaseUseCase() {
    suspend operator  fun invoke(page : Int ) = getBaseApiCall{ repository.getNotesForUser(BaseRepository.userID, page) }
}