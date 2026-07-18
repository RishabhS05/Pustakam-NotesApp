package com.app.pustakam.util

// 🔧 18-Jul-2026: file-import feature — original order kept (ordinal-safe); new types appended
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
    , LINK, DOCX{
        override fun getExt() = ".docx"   // 🔧 18-Jul-2026: real ext (was "")
    }, LOCATION, PDF{
        override fun getExt() = ".pdf"    // 🔧 18-Jul-2026: real ext (was "")
    }, GIF{
        override fun getExt() = ".gif"    // 🔧 18-Jul-2026: real ext (was "")
    },
    // 🔧 18-Jul-2026: NEW — imported document formats the book reader renders as pages
    TXT{
        override fun getExt() = ".txt"
    },
    MD{
        override fun getExt() = ".md"
    },
    EPUB{
        override fun getExt() = ".epub"
    },
    // 🔧 18-Jul-2026: NEW — fallback so ANY picked file can still be attached/imported
    OTHER;
   open fun getExt() = ""
}
