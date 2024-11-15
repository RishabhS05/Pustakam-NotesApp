package com.app.pustakam.data.models.request

import kotlinx.serialization.Serializable

@Serializable
 data class NoteRequest (val title : String? = null, val description : String? = null, val _id : String? = null)