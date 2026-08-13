package com.app.pustakam.core.common.util

import com.app.pustakam.core.common.util.ContentType.*
import kotlin.collections.setOf

// 🔧 18-Jul-2026: file-import feature — original order kept (ordinal-safe); new types appended

enum class ContentType {

    TEXT,

    IMAGE {
        override fun getExt() = ".png"
    }
    , VIDEO{
        override fun getExt() = ".mp4"
    }
    , AUDIO{
        override fun getExt() = ".mp3"
    }
    , LINK,
    DOCX{
        override fun getExt() = ".docx"   // 🔧 18-Jul-2026: real ext (was "")
    }, LOCATION,
    PDF{
        override fun getExt() = ".pdf"    // 🔧 18-Jul-2026: real ext (was "")
    }, GIF{
        override fun getExt() = ".gif"    // 🔧 18-Jul-2026: real ext (was "")
    },
    TXT{
        override fun getExt() = ".txt"
    },
    MD{
        override fun getExt() = ".md"
    },
    EPUB{
        override fun getExt() = ".epub"
    },
    DRAWING,
    FORMULA,
    TABLE,
    OTHER;

   open fun getExt() = ""
}

/** Types that can be written into the system gallery rather than a document folder. */
inline  fun ContentType.isGalleryEligible(): Boolean = this in setOf(IMAGE, VIDEO, GIF)
inline fun ContentType.isMedia() = this in setOf(IMAGE, VIDEO, AUDIO, GIF)
inline fun ContentType.isDoc() = this in setOf(DOCX, PDF, EPUB, TXT,MD, OTHER)
inline fun ContentType.isImage() = this in setOf(IMAGE, GIF)

inline fun ContentType.isPlayableMedia() = this in setOf(AUDIO, VIDEO)