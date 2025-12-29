package com.app.pustakam.android.extension

import android.graphics.Bitmap
import android.graphics.BitmapFactory

fun String.toBitmap(): Bitmap = BitmapFactory.decodeFile(this)

