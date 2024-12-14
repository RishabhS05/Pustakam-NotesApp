package com.app.pustakam.data.models

import kotlinx.serialization.Serializable

@Serializable
class BaseResponse<T>(
    val data : T?,
    val isFromDb : Boolean = false,
    val isSuccessful : Boolean,
    val message : String? =""
)
