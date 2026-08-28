package com.app.pustakam.core.model.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class User(
    val _id : String?,
    val name : String?,
    val phone : String?,
    val email : String?,
    @SerialName("avatarUrl")
    val avatarUrl : String?,
    @SerialName("createdAt")
    val createdAt : String?,
    @SerialName("updateAt")
    val updatedAt : String?,
    // 🔐 20-Aug-2026 sync: /login, /register and /auth/refresh spread both tokens into `data`
    val accessToken : String? = null,
    val refreshToken : String? = null,
)
