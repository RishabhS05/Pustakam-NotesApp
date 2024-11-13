package com.app.pustakam.data.models.request

import kotlinx.serialization.Serializable

@Serializable
 data class NoteRequest (val title : String? = null, val body : String? = null, val _id : String? = null)