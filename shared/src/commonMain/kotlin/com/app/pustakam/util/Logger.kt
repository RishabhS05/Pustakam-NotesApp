package com.app.pustakam.util

import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.SIMPLE


fun log_i(message: String) {
    Logger.SIMPLE.log(message)
}
fun log_d(TAG : String ,message : Any){
    println("$TAG : $message")
}