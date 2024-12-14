package com.app.pustakam.util

import kotlinx.datetime.Clock

object UniqueIdGenerator {

    fun generateUniqueId(): String {
        val timestamp = getCurrentTimestamp()
        val uuid = generateUUID()
        return "$timestamp-$uuid"
    }

    private fun getCurrentTimestamp(): Long {
        return Clock.System.now().toEpochMilliseconds()
    }

    private fun generateUUID(): String {
        return generateUUID()  // Fully supported in KMM.
    }
}
expect fun generateUUID() : String