package com.app.pustakam.data.models.response.notes
import com.app.pustakam.util.ContentType
import com.app.pustakam.util.getCurrentTimestamp

object NoteContentObjectHelper {
    fun createText(noteId: String , positionedAt : Long , text : String = "" )
    = NoteContentModel.TextContent(noteId = noteId, position =
        positionedAt)
    fun createMedia(contentType: ContentType, noteId: String , positionedAt : Long,
                    localPath : String= "", url: String = "", duration:  Long = 0,
                    timestamp : String? = null) : NoteContentModel.MediaContent {
        val time = if(timestamp.isNullOrEmpty()) "${getCurrentTimestamp()}" else timestamp
       return  NoteContentModel.MediaContent(
        type = contentType,
        position = positionedAt, duration =  duration,
            createdAt = time,
           updatedAt = time,
        noteId = noteId, localPath = localPath, url = url
    )}
    fun createHyperLink(link : String ="", noteId: String, positionedAt: Long ) = NoteContentModel.Link(
        position = positionedAt,
        noteId = noteId,
        url = link
    )
    fun createLocation(noteId: String, positionedAt: Long, lat: Double = 0.0, long : Double= 0.0) = NoteContentModel.Location(
        noteId =noteId,
        position = positionedAt,
        latitude = lat,
        longitude = long
    )
}