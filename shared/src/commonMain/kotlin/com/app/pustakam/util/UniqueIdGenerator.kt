package com.app.pustakam.util

import kotlinx.datetime.Clock

object UniqueIdGenerator {

    fun generateUniqueId(): String {
        val timestamp = getCurrentTimestamp()
        val uuid = generateUUID()
        return "$timestamp-$uuid"
    }

     fun getCurrentTimestamp(): Long {
        return Clock.System.now().toEpochMilliseconds()
    }
}
expect fun generateUUID() : String