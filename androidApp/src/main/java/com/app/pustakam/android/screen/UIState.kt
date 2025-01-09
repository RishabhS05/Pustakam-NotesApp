package com.app.pustakam.android.screen

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.app.pustakam.android.permission.NeededPermission
import com.app.pustakam.android.screen.noteEditor.NoteStatus
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.util.ContentType

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
    val notes: ArrayList<Note> = arrayListOf()
) : BaseUIState(isLoading = isLoading, error = error, successMessage = successMessage)

data class NoteUIState(
    override val isLoading: Boolean =false,
    override val error: String? =null,
    override val successMessage: String? = null,
    val showDeleteAlert : Boolean= false,
    val showDeleteButton : Boolean = false,
    val noteStatus : NoteStatus? = NoteStatus.OnEditingMode,
    val contentType : ContentType? = null,
    val showPermissionAlert  : Boolean? = null,
    val permissions : List<NeededPermission> = listOf(),
) : BaseUIState(isLoading = isLoading, error = error, successMessage = successMessage)

data class NoteContentUiState(
    val note: Note? = null,
    val titleTextState:MutableState<String>  = mutableStateOf(""),
    val isAllSetupDone:Boolean = false,
    val contents: SnapshotStateList<NoteContentModel> = mutableStateListOf()
)