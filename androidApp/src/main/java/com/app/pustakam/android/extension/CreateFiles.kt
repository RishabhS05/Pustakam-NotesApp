package com.app.pustakam.android.extension

import android.app.Activity

import java.io.File
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