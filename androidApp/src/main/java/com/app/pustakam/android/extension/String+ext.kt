package com.app.pustakam.android.extension

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.createBitmap
fun String.toBitmap(reqWidth: Int = 1080, reqHeight: Int = 1920): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(this, bounds)
    var sample = 1
    var (h, w) = bounds.outHeight to bounds.outWidth
    while (h / sample > reqHeight || w / sample > reqWidth) sample *= 2

    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    return BitmapFactory.decodeFile(this, opts)
        ?: createBitmap(1, 1)
}

