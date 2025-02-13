package com.app.pustakam.extensions

import kotlin.time.DurationUnit

fun Long.getTimerFormatedString() : String{
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val seconds = this % 60
    val minsec ="${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    val format = if (hours >= 1) "${hours.toString().padStart(2, '0')}:" + minsec
    else minsec
    return format
}
//calculate time with fprmated string
fun Long.getSliderPosition(duration: Long) : Long = ((this * duration )/100)
fun Long.getReadableDuration() : String {
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val seconds = this % 60
    val hrs = "${hours.toString().padStart(2, '0')} hrs "
    val min = "${minutes.toString().padStart(2, '0')} min "
    val sec =  "${seconds.toString().padStart(2, '0')} sec"
    val format : String  = when {
        hours>= 1 -> hrs+min+sec
        minutes >= 1 ->min+sec
        else -> sec
    }
    return format
}

fun Long.timerRemaining(currentValue : Long) : String = (this - currentValue).getTimerFormatedString()