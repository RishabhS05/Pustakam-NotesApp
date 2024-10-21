package com.app.pustakam.android.screen

 open class BaseUIState (open val isLoading : Boolean, open val error : String? = null, open val successMessage : String? =null )
 data class LoginUIState(override val isLoading: Boolean, override var error: String? = null, override val successMessage: String? = null,
     val isLogging : Boolean = false):
     BaseUIState(isLoading = isLoading, error= error , successMessage = successMessage)

data class SignupUIState(override val isLoading: Boolean, override val error: String? = null,
                         override val successMessage: String? = null,
                        val isRegistered : Boolean = false,
    val imageUrl : String? = null ):
    BaseUIState(isLoading = isLoading, error= error , successMessage = successMessage)