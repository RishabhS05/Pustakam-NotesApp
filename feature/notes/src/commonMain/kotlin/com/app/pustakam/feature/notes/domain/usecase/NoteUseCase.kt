package com.app.pustakam.feature.notes.domain.usecase

import com.app.pustakam.core.model.models.Tag
import com.app.pustakam.core.model.models.response.notes.Note


class CreateORUpdateNoteUseCase : NoteBaseUseCase() {
     operator fun invoke(note: Note) =
        getBaseApiCall { noteRepository.insertOrUpdateNote(note) }
     operator fun invoke(note: Note, dirtyContentIds: Set<String>) =
        getBaseApiCall { noteRepository.insertOrUpdateNote(note, dirtyContentIds) }
}

class DeleteNoteUseCase : NoteBaseUseCase() {
     operator fun invoke(noteId: String?) =
        getBaseApiCall { noteRepository.deleteNote(noteId ?: "") }
}

class ReadNoteUseCase : NoteBaseUseCase() {
     operator fun invoke(id: String?) =
        getBaseApiCall { noteRepository.getANote(id) }
}
class GetNotesUseCase : NoteBaseUseCase() {
     operator fun invoke(page: Int) =
        getBaseApiCall { noteRepository.getAllNotes(page) }

     operator fun invoke(page: Int, limit: Int) =
        getBaseApiCall { noteRepository.getAllNotes(page, limit) }
}

class GetNoteSummariesUseCase : NoteBaseUseCase() {
     operator fun invoke(page: Int, limit: Int) =
        getBaseApiCall { noteRepository.getNoteSummaries(page, limit) }
}

class SearchNotesUseCase : NoteBaseUseCase() {
     operator fun invoke(query: String) =
        getBaseApiCall { noteRepository.searchNotes(query) }
}

class GetTagCase : NoteBaseUseCase() {
     operator fun invoke() =
        getBaseApiCall { noteRepository.getTagsFromDB() }
}
class  CreateTagUseCase : NoteBaseUseCase() {
     operator fun invoke(tag: Tag) =
        getBaseApiCall { noteRepository.createTagOnDB(tag) }
}
class UpdateTagUseCase : NoteBaseUseCase() {
     operator fun invoke(tag: Tag) =
        getBaseApiCall { noteRepository.updateTagOnDB(tag) }
}
class DeleteTagUseCase : NoteBaseUseCase() {
     operator fun invoke(tagId: String?) =
        getBaseApiCall { noteRepository.deleteTagOnDB(tagId ?: "") }
}