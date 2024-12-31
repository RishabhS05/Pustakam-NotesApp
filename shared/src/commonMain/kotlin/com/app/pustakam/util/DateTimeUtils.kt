package com.app.pustakam.util

import kotlinx.datetime.Clock

fun getCurrentClockTime() = Clock.System.now()
fun getCurrentTimestamp(): Long {
    return getCurrentClockTime().toEpochMilliseconds()
}