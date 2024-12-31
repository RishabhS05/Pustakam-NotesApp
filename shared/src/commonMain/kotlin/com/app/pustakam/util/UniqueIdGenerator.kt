package com.app.pustakam.util

import kotlinx.datetime.Clock

object UniqueIdGenerator {
    fun generateUniqueId(): String = "${getCurrentTimestamp()}-${generateUUID()}"
}
expect fun generateUUID() : String