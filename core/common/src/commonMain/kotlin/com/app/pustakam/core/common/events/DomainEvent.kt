package com.app.pustakam.core.common.events

sealed interface DomainEvent {
    data class NoteSaved(val noteId: String) : DomainEvent
    data class NoteDeleted(val noteId: String) : DomainEvent
    data class NoteContentDeleted(val contentId: String) : DomainEvent
}
