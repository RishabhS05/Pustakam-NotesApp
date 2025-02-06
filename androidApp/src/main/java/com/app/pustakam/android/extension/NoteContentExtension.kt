package com.app.pustakam.android.extension

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.app.pustakam.data.models.response.notes.NoteContentModel

@OptIn(UnstableApi::class)
fun NoteContentModel.MediaContent.toMediaItem() : MediaItem{
    return MediaItem.Builder()
        .setMediaId(id)
        .setTag(title)
        .setUri(if (!localPath.isNullOrEmpty() )localPath else url)
        .build()
}