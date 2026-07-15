package com.app.pustakam.util

// 🔧 15-Jul-2026 iOS MEDIA-LOST FIX (Android side): pass-through — Android app storage paths are
//   stable across updates (/data/data/<pkg>/ never moves), so no rebasing is ever needed here.
actual fun resolveLocalFilePath(path: String?): String? = path?.takeIf { it.isNotEmpty() }
