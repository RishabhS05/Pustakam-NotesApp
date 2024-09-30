package com.app.pustakam.data.models.response.notes

import kotlinx.serialization.Serializable

@Serializable
data class Notes(val notes : ArrayList<Note>?, val count : Int?, val page : Int? )