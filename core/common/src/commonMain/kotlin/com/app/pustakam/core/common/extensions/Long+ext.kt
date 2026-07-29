package com.app.pustakam.core.common.extensions

import com.app.pustakam.util.log_d

fun Long.getTimerFormatedString() : String{
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val seconds = this % 60
    val minsec ="${minutes.toString().pad()}:${seconds.pad()}"
    val format = if (hours >= 1) "${hours.pad()}:" + minsec
    else minsec
    return format
}
//calculate time with fprmated string
fun Long.getSliderPosition(duration: Long) : Long = ((this * duration )/100)
fun Long.getReadableHMS() : String {
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val seconds = this % 60
    val hrs = "${hours.pad()} hrs "
    val min = "${minutes.pad()} min "
    val sec =  "${seconds.pad()} sec"
    val format : String  = when {
        hours>= 1 -> hrs+min+sec
        minutes >= 1 ->min+sec
        else -> sec
    }
    return format
}
fun Long.readableTimer(): String {
    val totalSeconds = this / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    // 🔧 14-Jul-2026: FIX — seconds must be the REMAINDER (was the total: 90s printed as "01:90").
    //   Pure display-math fix, same behavior contract (receiver is milliseconds) on all platforms.
    val seconds = totalSeconds % 60
    val format : String  = when {
        hours>= 1 -> hours.pad() +":"+minutes.pad() +":"+seconds.pad()
        else  -> minutes.pad()+":"+seconds.pad()
    }
    return format
}
fun Long.timerRemaining(currentValue : Long) : String  {
    val time = this-currentValue
    log_d("log Time Calculation $this $currentValue", "duration-current ${time} ${time.getTimerFormatedString()}")
   return time.readableTimer()
}
fun Any.pad() : String {
    return this.toString().padStart(2,'0')
}