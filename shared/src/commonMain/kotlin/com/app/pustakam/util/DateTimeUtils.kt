package com.app.pustakam.util

import kotlinx.datetime.*

fun getCurrentClockTime() = Clock.System.now()
fun getCurrentTimestamp(): Long {
    return getCurrentClockTime().toEpochMilliseconds()
}
