package com.app.pustakam.data.network

 enum class ApiRoute {

    LOGIN {
        override fun getName(): String ="$baseUrl/login"
     },
     REGISTER {
         override fun getName(): String ="$baseUrl/register"
     },
     NOTES {
         override fun getName(): String = "$baseUrl/notes"
     },
     PROFILE {
         override fun getName(): String = "$baseUrl/profile"
             },
     USERS{
         override fun getName(): String = "$baseUrl/users"
         };

     val baseUrl = getUrl()
     abstract fun getName(): String
}
const val headerAuth = "Authorization"
