package com.app.pustakam.core.common.util

// 🔧 15-Jul-2026 iOS MEDIA-LOST FIX: stored absolute media paths go stale on iOS because every app
//   update/reinstall changes the sandbox container UUID (/var/mobile/.../Application/<UUID>/Documents/…)
//   while the files themselves are preserved. This resolver re-anchors a stored path onto the
//   CURRENT container at read time — no migration, fixes all existing rows on every future update.
//   Android's package dir never moves, so its actual is a pass-through.
//   Returns null for null/blank input; non-local strings (http urls) pass through unchanged.
expect fun resolveLocalFilePath(path: String?): String?
