package com.app.pustakam.android.fileUtils

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.app.pustakam.data.models.response.notes.NoteContentModel
import com.app.pustakam.util.ContentType
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
    val file = File(filePath)
   return saveBitmapToFile(bitmap,file)
}
fun saveBitmapToFile(bitmap: Bitmap,file: File): Boolean{
    return try {
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

// 🔧 14-Jul-2026: NEW FEATURE — "Save media to device".
//   Images/Videos are written silently into the system Gallery (MediaStore);
//   Audio/PDF/DOCX/GIF are exported through the Storage-Access-Framework picker
//   (see MediaSaveOverlay in NotesEditorView.kt) into a user-chosen folder
//   defaulting to Downloads. Everything below is additive — no existing API changed.

// 🔧 14-Jul-2026: MIME type for a media block (used by MediaStore + the SAF picker).
//   Usage: mimeTypeFor(media.type)  ->  "image/png", "video/mp4", "audio/mpeg", ...
fun mimeTypeFor(type: ContentType): String = when (type) {
    ContentType.IMAGE -> "image/png"
    ContentType.VIDEO -> "video/mp4"
    ContentType.AUDIO -> "audio/mpeg"
    ContentType.PDF -> "application/pdf"
    ContentType.DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    ContentType.GIF -> "image/gif"
    else -> "application/octet-stream"
}

// 🔧 14-Jul-2026: A human/file-system friendly name for the exported/saved file.
//   Falls back to the source file name, then to a timestamped default.
//   Usage: suggestedFileName(media)  ->  "Video-2.mp4"
fun suggestedFileName(media: NoteContentModel.MediaContent): String {
    val ext = media.type.getExt().ifEmpty { "" }
    val fromSource = media.localPath?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
    val base = media.title.takeIf { it.isNotBlank() }
        ?: fromSource?.substringBeforeLast('.')
        ?: "Pustakam-${System.currentTimeMillis()}"
    // If we already have a real file name with an extension, keep it as-is.
    if (fromSource != null && fromSource.contains('.')) return fromSource
    return if (ext.isNotEmpty()) "$base$ext" else base
}

// 🔧 14-Jul-2026: Save an IMAGE or VIDEO straight into the device Gallery.
//   Uses scoped-storage MediaStore on Android 10+ (no runtime permission needed);
//   on older devices it relies on WRITE_EXTERNAL_STORAGE (declared in the manifest,
//   maxSdkVersion=28). Returns true on success.
//   Usage: val ok = saveMediaToGallery(context, media)
fun saveMediaToGallery(context: Context, media: NoteContentModel.MediaContent): Boolean {
    val sourcePath = media.localPath?.takeIf { it.isNotEmpty() } ?: media.url.takeIf { it.isNotEmpty() }
    if (sourcePath.isNullOrEmpty()) return false
    val source = File(sourcePath)
    if (!source.exists()) return false

    val resolver = context.contentResolver
    val fileName = suggestedFileName(media)
    val mime = mimeTypeFor(media.type)
    val isVideo = media.type == ContentType.VIDEO

    // Collection + relative sub-folder differ for images vs videos.
    val collection = if (isVideo)
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val relativeDir = if (isVideo)
        "${Environment.DIRECTORY_MOVIES}/Pustakam" else "${Environment.DIRECTORY_PICTURES}/Pustakam"

    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, mime)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
            put(MediaStore.MediaColumns.IS_PENDING, 1) // hide until fully written
        }
    }

    return try {
        val uri: Uri = resolver.insert(collection, values) ?: return false
        resolver.openOutputStream(uri)?.use { out ->
            source.inputStream().use { input -> input.copyTo(out) }
        } ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0) // publish
            resolver.update(uri, values, null, null)
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

// 🔧 14-Jul-2026: Copy a media file's bytes into a SAF-picked destination Uri.
//   Used for AUDIO/PDF/DOCX/GIF once the user picks a location via the
//   ACTION_CREATE_DOCUMENT picker. Returns true on success.
//   Usage: writeMediaToUri(context, media, pickedUri)
fun writeMediaToUri(context: Context, media: NoteContentModel.MediaContent, destination: Uri): Boolean {
    val sourcePath = media.localPath?.takeIf { it.isNotEmpty() } ?: media.url.takeIf { it.isNotEmpty() }
    if (sourcePath.isNullOrEmpty()) return false
    val source = File(sourcePath)
    if (!source.exists()) return false
    return try {
        context.contentResolver.openOutputStream(destination)?.use { out ->
            source.inputStream().use { input -> input.copyTo(out) }
        } ?: return false
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}


