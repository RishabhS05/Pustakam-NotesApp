package com.app.pustakam.core.model.models.request

import kotlinx.serialization.Serializable

@Serializable
data class RegisterReq(
    val name : String?,
    val email: String?,
    val password : String? ="",
    val passwordConfirm: String? ="",
    val phone :String?="",
    val imageUrl : String? = ""
)