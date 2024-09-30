package com.app.pustakam.data.models.response

import kotlinx.serialization.Serializable

/*
* {
    "isSuccessful": true,
    "data": {
        "_id": "66f08c8b98d30bd41a95cb18",
        "name": "rishabh2",
        "phone": null,
        "avatarUrl": null,
        "email": "rishabh3@gmail.com",
        "createdAt": "2024-09-22T21:30:51.618Z",
        "updatedAt": "2024-09-22T21:30:51.618Z"
    }
}*/
@Serializable
data class User(
    val _id : String?,
    val name : String?,
    val phone : String?,
    val email : String?,
    val avatarUrl : String?,
    val createdAt : String?,
    val updatedAt : String?,
)
