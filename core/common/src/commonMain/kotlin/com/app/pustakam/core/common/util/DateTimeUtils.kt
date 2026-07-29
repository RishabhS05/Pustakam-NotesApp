package com.app.pustakam.core.common.util

import kotlinx.datetime.*

fun getCurrentClockTime() = Clock.System.now()
fun getCurrentTimestamp(): Long {
    return getCurrentClockTime().toEpochMilliseconds()
}
