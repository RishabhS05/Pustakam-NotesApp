package com.app.pustakam.domain.repositories

interface ILocalRepository {
    suspend fun getUserFromPrefToken(): String?
   suspend fun setUserFromPrefToken(token : String)
   suspend fun setUserFromPrefId(userId : String)
   suspend fun  getUserFromPrefId() : String?
}