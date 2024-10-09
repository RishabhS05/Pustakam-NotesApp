package com.app.pustakam.data.models.response

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
)
