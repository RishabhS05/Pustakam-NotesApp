package com.app.pustakam.feature.notes.domain.usecase

import com.app.pustakam.core.usecases.BaseUseCase
import com.app.pustakam.feature.notes.domain.repository.INoteRepository
import org.koin.core.component.inject

abstract class NoteBaseUseCase : BaseUseCase() {
    protected val noteRepository: INoteRepository by inject()

    val notes = noteRepository.notesState
    val tags = noteRepository.tagState
    val noteSummaries = noteRepository.noteSummariesState
}
