package com.app.pustakam.android.screen

import com.app.pustakam.android.screen.noteEditor.NoteStatus
import com.app.pustakam.data.models.response.notes.Note

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
):
    BaseUIState(isLoading = isLoading, error= error , successMessage = successMessage)

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
    override val isLoading: Boolean,
    override val error: String? =null,
    override val successMessage: String? = null,
    var isSetupValues:Boolean = false,
    val showDeleteAlert : Boolean= false,

    val noteStatus : NoteStatus? = null,
    val note : Note? = null
) : BaseUIState(isLoading = isLoading, error = error, successMessage = successMessage)

