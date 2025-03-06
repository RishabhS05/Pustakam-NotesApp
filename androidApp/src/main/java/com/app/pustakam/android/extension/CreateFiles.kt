package com.app.pustakam.android.extension

import android.app.Activity
import android.graphics.Bitmap

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun createFileWithFolders(context: Activity, folderPath : String, fileName : String) : File {
    var file : File = File("")
    try{
        val baseDir = File(context.filesDir,folderPath)
        if (!baseDir.exists()) {
            if (!baseDir.mkdirs()) {
            throw IOException("Failed to create directories: ${baseDir.absolutePath}")
            }
        }
        file = File(baseDir.absolutePath, fileName)
        if (!file.exists()) {
            try {
                file.createNewFile()
            } catch (e: IOException) {
                throw IOException("Failed to create file: ${file.absolutePath}", e)
            }
        }
    } catch (e: Exception){
        e.printStackTrace()
    }
    return file
}

fun deleteFile(filePath : String){
    val file = File(filePath)
    if (file.exists() && file.isFile) {
        file.delete()
    }
}

fun saveBitmapToFile(bitmap: Bitmap, filePath: String): Boolean {
    return try {
        val file = File(filePath)
        file.parentFile?.mkdirs() // Ensure the directory exists
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG,100 , out) // Save as PNG with 100% quality
        }
        true // Success
    } catch (e: IOException) {
        e.printStackTrace()
        false // Failure
    }
}