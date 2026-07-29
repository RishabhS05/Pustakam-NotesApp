package com.app.pustakam.core.model.models.request

import kotlinx.serialization.Serializable

@Serializable
data class Login( val email: String?, val password : String? = "", val phone :String? = "")