package com.app.pustakam.core.common.util

object UniqueIdGenerator {
    fun generateUniqueId(): String = "${getCurrentTimestamp()}-${generateUUID()}"
}
expect fun generateUUID() : String