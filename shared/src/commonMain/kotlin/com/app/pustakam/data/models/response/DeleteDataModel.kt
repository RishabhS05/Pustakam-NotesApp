package com.app.pustakam.data.models.response

import kotlinx.serialization.Serializable

@Serializable
data class DeleteDataModel(val acknowledged : Boolean?, val deletedCount : Int?)