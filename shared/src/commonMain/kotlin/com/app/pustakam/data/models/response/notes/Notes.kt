package com.app.pustakam.data.models.response.notes

import kotlinx.serialization.Serializable

@Serializable
data class Notes(val notes : ArrayList<Note> = arrayListOf(),  val count : Int = 0 , val page : Int = 0)