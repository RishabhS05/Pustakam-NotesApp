package com.app.pustakam.data.models

import kotlinx.serialization.Serializable

@Serializable
class BaseResponse<T>(
    val data : T?,
    val isSuccessful : Boolean,
    val message : String? =""
)
