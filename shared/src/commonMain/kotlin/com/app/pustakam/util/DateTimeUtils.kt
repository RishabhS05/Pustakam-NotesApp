package com.app.pustakam.util

import kotlinx.datetime.*

fun getCurrentClockTime() = Clock.System.now()
fun getCurrentTimestamp(): Long {
    return getCurrentClockTime().toEpochMilliseconds()
}
fun getTimerFormatedString(elapsedTime : Long ) : String{
    val hours = elapsedTime / 3600
    val minutes = (elapsedTime % 3600) / 60
    val seconds = elapsedTime % 60
    val minsec ="${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    val format = if (hours < 1) "${hours.toString().padStart(2, '0')}:" + minsec
    else minsec
    return format

}