package com.app.pustakam.core.model.models.response

import kotlinx.serialization.Serializable

@Serializable
data class DeleteDataModel(val acknowledged : Boolean?, val deletedCount : Int?)