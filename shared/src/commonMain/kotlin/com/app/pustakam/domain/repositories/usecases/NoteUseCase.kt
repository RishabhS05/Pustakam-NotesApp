package com.app.pustakam.domain.repositories.usecases

import com.app.pustakam.data.models.Tag
import com.app.pustakam.data.models.response.notes.Note


class CreateORUpdateNoteUseCase : NoteBaseUseCase() {
    suspend operator fun invoke(note: Note) =
        getBaseApiCall { noteRepository.insertOrUpdateNote(note) }
    // 🔧 15-Jul-2026 Phase 0.4: dirty-save overload — writes only the given content rows.
    //   Separate overload (not a default param) so the iOS bridge call sites stay untouched.
    suspend operator fun invoke(note: Note, dirtyContentIds: Set<String>) =
        getBaseApiCall { noteRepository.insertOrUpdateNote(note, dirtyContentIds) }
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
    // 🔧 15-Jul-2026 Phase 0.1: paged overload — Android list opts in with NOTES_PAGE_SIZE;
    //   the single-arg form keeps the legacy load-everything behavior (iOS bridge).
    suspend operator fun invoke(page: Int, limit: Int) =
        getBaseApiCall { noteRepository.getAllNotes(page, limit) }
}

class DeleteNoteContentUseCase : NoteBaseUseCase() {
    suspend operator fun invoke(id: String?) =
        getBaseApiCall { noteRepository.deleteNoteContentFromDb(id ?: "") }
}
class GetTagCase : NoteBaseUseCase() {
    suspend operator fun invoke() =
        getBaseApiCall { noteRepository.getTagsFromDB() }
}
class  CreateTagUseCase : NoteBaseUseCase() {
    suspend operator fun invoke(tag: Tag) =
        getBaseApiCall { noteRepository.createTagOnDB(tag) }
}
class UpdateTagUseCase : NoteBaseUseCase() {
    suspend operator fun invoke(tag: Tag) =
        getBaseApiCall { noteRepository.updateTagOnDB(tag) }
}
class DeleteTagUseCase : NoteBaseUseCase() {
    suspend operator fun invoke(tagId: String?) =
        getBaseApiCall { noteRepository.deleteTagOnDB(tagId ?: "") }
}