package com.app.pustakam.core.common.util



fun currentSliderProgress(progress : Long , totalDuration : Long ) : Int =
    if(progress > 0) ((progress/totalDuration) * 100L).toInt() else 0

