package com.app.pustakam.data.models.response.notes

import kotlinx.serialization.Serializable
@Serializable
data class Note(
    val _id: String?,
    var title: String?,
    var updatedAt: String?,
    var createdAt: String?,
    var url: String?,
    var description: String?,
)
