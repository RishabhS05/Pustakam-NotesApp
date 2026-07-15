package com.app.pustakam.data.models.response.notes

import kotlinx.serialization.Serializable

// 🔧 15-Jul-2026 Phase 0.1: shared page size for the notes list (Android list paging; a page is
//   "full" when it returns exactly this many notes, so fewer means no next page).
const val NOTES_PAGE_SIZE = 20

@Serializable
data class Notes(val notes: ArrayList<Note> = arrayListOf(), val count: Int = 0, val page: Int = 0)