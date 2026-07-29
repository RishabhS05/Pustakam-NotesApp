package com.app.pustakam.core.common.util

import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

// 🔧 15-Jul-2026 iOS MEDIA-LOST FIX: every iOS app update/reinstall assigns a NEW sandbox container
//   UUID; Documents/ files survive but absolute paths stored in the DB keep the OLD UUID, so
//   playback found nothing ("UI is there but audio/video not working"). Fix: if the stored path is
//   missing on disk, re-anchor everything after "/Documents/" onto the CURRENT Documents directory.
actual fun resolveLocalFilePath(path: String?): String? {
    val stored = path?.takeIf { it.isNotEmpty() } ?: return null
    if (!stored.startsWith("/")) return stored                    // remote url etc. — not a local path
    val fileManager = NSFileManager.defaultManager
    if (fileManager.fileExistsAtPath(stored)) return stored       // fresh path — nothing to do
    val marker = "/Documents/"
    val markerIndex = stored.indexOf(marker)
    if (markerIndex < 0) return stored                            // not under Documents (e.g. tmp) — can't rebase
    val documentsDir = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        .firstOrNull() as? String ?: return stored
    val rebased = documentsDir + "/" + stored.substring(markerIndex + marker.length)
    return if (fileManager.fileExistsAtPath(rebased)) rebased else stored
}
