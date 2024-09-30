package com.app.pustakam.data.network

 enum class ApiRoute {

    LOGIN {
        override fun getName(): String ="/login"
    }, REGISTER {
         override fun getName(): String ="/register"
     },NOTES {
         override fun getName(): String = "/Notes"
     },
     USERS{
         override fun getName(): String = "/users"
         };


     abstract fun getName(): String
}
const val headerAuth = "Authorization Bearer "