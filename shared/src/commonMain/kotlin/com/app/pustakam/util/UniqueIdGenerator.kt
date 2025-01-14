package com.app.pustakam.util

object UniqueIdGenerator {
    fun generateUniqueId(): String = "${getCurrentTimestamp()}-${generateUUID()}"
}
expect fun generateUUID() : String