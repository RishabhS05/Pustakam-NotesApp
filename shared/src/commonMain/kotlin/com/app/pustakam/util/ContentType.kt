package com.app.pustakam.util

enum class ContentType {
    TEXT{},
    IMAGE{
        override fun getExt() = ".png"
    }
    , VIDEO{
        override fun getExt() = ".mp4"
    }
    , AUDIO{
        override fun getExt() = ".mp3"
    }
    , LINK, DOCX, LOCATION, PDF, GIF;
   open fun getExt() = ""
}