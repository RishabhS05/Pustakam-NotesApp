package com.app.pustakam.android.screen

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.app.pustakam.android.permission.NeededPermission
import com.app.pustakam.android.screen.noteEditor.NoteStatus
import com.app.pustakam.android.screen.notes.list.TagIntent
import com.app.pustakam.core.model.models.Tag
import com.app.pustakam.core.model.models.response.notes.Note
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import com.app.pustakam.core.model.models.response.notes.NoteSummary
import com.app.pustakam.core.common.util.ContentType

open class BaseUIState (open val isLoading : Boolean,
                        open val error : String? = null,
                        open val successMessage : String? = null,

    ){
    override fun toString(): String {
        return "BaseUIState(isLoading=$isLoading, error=$error, successMessage=$successMessage)"
    }

}

 data class LoginUIState(override val isLoading: Boolean,
                         override var error: String? = null,
                         override val successMessage: String? = null,
     val isLogging : Boolean = false):
     BaseUIState(isLoading = isLoading, error= error , successMessage = successMessage)

data class SignupUIState(override val isLoading: Boolean,
                         override val error: String? = null,
                         override val successMessage: String? = null,
                         val isRegistered : Boolean = false,
                         val imageUrl : String? = null
): BaseUIState(isLoading = isLoading, error= error , successMessage = successMessage)

data class NotesUIState(
    override val isLoading: Boolean,
    override val error: String? =null,
    override val successMessage: String? = null,
    val page : Int = 1,
    val count : Int = 0,
    val isNextPage : Boolean = true,
    val tagDataIntent : TagIntent? = null,
    val notes: ArrayList<Note> = arrayListOf(),
    // 🔧 15-Jul-2026 Summary query: what the list actually renders now — light summaries
    //   (title/snippet/counts/thumbnail); full Note contents never load for the list screen.
    val summaries: List<NoteSummary> = emptyList(),
    // 🔄 28-Aug-2026 — pull-to-refresh. Deliberately NOT isLoading: that one blocks the screen
    //   with a spinner overlay, and a refresh must leave the list readable while it runs.
    val isRefreshing: Boolean = false,
) : BaseUIState(isLoading = isLoading, error = error, successMessage = successMessage)

//Note ActionState
data class NoteUIState(
    override val isLoading: Boolean =false,
    override val error: String? =null,
    override val successMessage: String? = null,
    val showDeleteAlert : Boolean= false,
    val showDeleteButton : Boolean = false,
    val showPermissionAlert  : Boolean? = null,
    val showAudioRecorder :Boolean = false,
    val deleteNoteContentId : String?= null,
    val noteStatus : NoteStatus? = NoteStatus.OnEditingMode,
    val contentType : ContentType? = null,
    val permissions : List<NeededPermission> = listOf(),
    val LocationState : Boolean = false ,
    // 🔄 28-Aug-2026 — pull-to-refresh inside the editor
    val isRefreshing: Boolean = false,
) : BaseUIState(isLoading = isLoading, error = error, successMessage = successMessage)

//NoteDataState
data class NoteContentUiState(
    val note: Note? = null,
    val titleTextState:MutableState<String>  = mutableStateOf(""),
    val isAllSetupDone:Boolean = false,
    val contents: SnapshotStateList<NoteContentModel> = mutableStateListOf()
)
data class TagState(val tags: ArrayList<Tag> = arrayListOf(),
                    val dialog : DialogEnum = DialogEnum.NONE)

enum class DialogEnum {
    NONE,
    CREATE_TAG,
    UPDATE_TAG,
    DELETE_TAG,
    COLOR_PICKER,
}
