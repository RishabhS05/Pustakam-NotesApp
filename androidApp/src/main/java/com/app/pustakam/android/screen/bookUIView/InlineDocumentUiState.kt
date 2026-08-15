package com.app.pustakam.android.screen.bookUIView

import androidx.compose.runtime.Immutable
import com.app.pustakam.core.model.models.response.notes.NoteContentModel

// 📖 15-Aug-2026: everything the reader needs to open a document in place. Disabled by default, so
//   every existing caller keeps the card-only behaviour it has today.
@Immutable
data class InlineDocumentUiState(
    val enabled: Boolean = false,
    val expandedIds: Set<String> = emptySet(),
    val busyIds: Set<String> = emptySet(),
    val unreadableIds: Set<String> = emptySet(),
    val onToggle: (NoteContentModel.MediaContent) -> Unit = {},
    val onLoadMore: (String) -> Unit = {},
) {
    fun isExpanded(contentId: String): Boolean = enabled && contentId in expandedIds

    fun isBusy(contentId: String): Boolean = enabled && contentId in busyIds

    /** False once a probe proved the file has no sheets we can draw — the card stays a card. */
    fun isReadable(contentId: String): Boolean = contentId !in unreadableIds

    companion object {
        val Disabled = InlineDocumentUiState()
    }
}
