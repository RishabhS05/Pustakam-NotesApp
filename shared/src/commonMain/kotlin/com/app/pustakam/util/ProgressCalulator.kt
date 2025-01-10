package com.app.pustakam.util

import com.app.pustakam.extensions.getTimerFormatedString

fun currentSliderProgress(progress : Long , totalDuration : Long ) : Int =
    if(progress > 0) ((progress/totalDuration) * 100L).toInt() else 0

