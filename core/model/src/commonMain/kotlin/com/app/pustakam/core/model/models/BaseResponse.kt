package com.app.pustakam.core.model.models

import kotlinx.serialization.Serializable

@Serializable
class BaseResponse<T>(
    val data : T?,
    val isFromDb : Boolean = false,
    val isSuccessful : Boolean,
    val message : String? =""
)
