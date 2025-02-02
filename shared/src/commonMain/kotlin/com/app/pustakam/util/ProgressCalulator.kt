package com.app.pustakam.util



fun currentSliderProgress(progress : Long , totalDuration : Long ) : Int =
    if(progress > 0) ((progress/totalDuration) * 100L).toInt() else 0

