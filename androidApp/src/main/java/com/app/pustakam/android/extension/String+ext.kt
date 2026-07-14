package com.app.pustakam.android.extension

import android.graphics.Bitmap
import android.graphics.BitmapFactory

// 🔧 14-Jul-2026: PERF/CRASH FIX — decode a DOWNSAMPLED bitmap instead of the full-resolution
//   image. Loading full-res photos on the main thread was slow to open and could OutOfMemory
//   crash when several were opened. inSampleSize keeps memory bounded to ~reqWidth x reqHeight.
//   Usage: path.toBitmap()  or  path.toBitmap(reqWidth = 1080, reqHeight = 1920)
fun String.toBitmap(reqWidth: Int = 1080, reqHeight: Int = 1920): Bitmap {
    // First pass: read only bounds (no pixels loaded into memory).
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(this, bounds)

    // Compute the largest inSampleSize (power of 2) that still covers the requested size.
    var sample = 1
    var (h, w) = bounds.outHeight to bounds.outWidth
    while (h / sample > reqHeight || w / sample > reqWidth) sample *= 2

    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    // Fall back to a plain decode if bounds were unreadable (returns null-safe empty 1x1).
    return BitmapFactory.decodeFile(this, opts)
        ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
}

