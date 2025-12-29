package com.app.pustakam.data.models.response.notes

import com.app.pustakam.data.localdb.database.RichTextMetadata
import com.app.pustakam.util.ContentType
import com.app.pustakam.util.UniqueIdGenerator
import com.app.pustakam.util.getCurrentTimestamp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Note(
    @SerialName("_id")
    val id: String,
    var title: String?,
    var updates: List<String>? = null,
    var updatedAt: String?,
    var createdAt: String?,
    var categoryId: String? ="",
    var isSynced : Boolean? = false,
    var contents: List<NoteContentModel>? = emptyList(),
    ) {    override fun equals(other: Any?): Boolean {
        return other is Note && other.id == this.id
    }

    override fun hashCode(): Int {
       return  31 * ( id.hashCode()+
           (updatedAt?.hashCode() ?: 0) +
               (createdAt?.hashCode() ?: 0) +
               (title?.hashCode() ?: 0 ) +
               (isSynced?.hashCode() ?: 0) +
               (contents?.count()?.hashCode() ?: 0))
    }
}

@Serializable
sealed class NoteContentModel {
    abstract val position: Long  // Position in the layout
    abstract val updatedAt: String?
    abstract val createdAt: String?
    abstract val type: ContentType
    abstract val isDeletedContent : Boolean
    @SerialName("_id")
    abstract val id: String
    abstract val noteId : String
    override fun hashCode(): Int {
        return  31 * (+id.hashCode()+ noteId.hashCode()  + position.hashCode() + type.hashCode() + (updatedAt?.hashCode() ?: 0) +
                (createdAt?.hashCode() ?: 0) )
    }
    override fun equals(other: Any?): Boolean {

        if (other == null) return false
        other as NoteContentModel
        if (position != other.position) return false
        if (createdAt != other.createdAt) return false
        if (type != other.type) return false
        if (id != other.id) return false
        if (noteId != other.noteId) return false

        return true
    }

    data class TextContent(
        var text: String = "",
        override val updatedAt: String? = "${getCurrentTimestamp()}",
        override val createdAt: String?= "${getCurrentTimestamp()}",
        override val type: ContentType = ContentType.TEXT,
        override val id: String = UniqueIdGenerator.generateUniqueId(),
        override val isDeletedContent: Boolean = false ,
        override val noteId: String ,
        override val position: Long,
        val metadata: RichTextMetadata? = null,
    ) : NoteContentModel()
    data class MediaContent(
        override val position: Long,
        override val noteId: String,
        override val type: ContentType ,
        override val updatedAt: String? = "${getCurrentTimestamp()}" ,
        override val createdAt: String? = "${getCurrentTimestamp()}",
        override val id: String = UniqueIdGenerator.generateUniqueId(),
        override val isDeletedContent: Boolean = false,
        val duration: Long = 0,
        val localPath: String? = null,
        val url: String = "",
        val title: String = "Audio",
    ) : NoteContentModel()

    data class Link(
        val url: String ="",
        override val isDeletedContent: Boolean = false,
        override val updatedAt: String? = "${getCurrentTimestamp()}" ,
        override val createdAt: String? = "${getCurrentTimestamp()}",
        override val type: ContentType = ContentType.LINK,
        override val id: String = UniqueIdGenerator.generateUniqueId(),
        override val position: Long,
        override val noteId: String,
    ) : NoteContentModel()

    data class Location(
        val latitude: Double =0.0,
        val longitude: Double= 0.0,
        val address: String? = null,
        override val isDeletedContent: Boolean = false,
        override val updatedAt: String? = "${getCurrentTimestamp()}",
        override val createdAt: String? = "${getCurrentTimestamp()}",
        override val type: ContentType = ContentType.LOCATION,
        override val id: String= UniqueIdGenerator.generateUniqueId(),
        override val noteId: String,
        override val position: Long,
    ) : NoteContentModel()
    fun isMediaFile() : Boolean = this is MediaContent
    fun isPlayingMedia(): Boolean = this.type == ContentType.AUDIO || this.type == ContentType.VIDEO
}
fun NoteContentModel.MediaContent.getMediaUrl(): String = localPath?.takeIf { it.isNotEmpty() }
        ?: url.takeIf { it.isNotEmpty() }
        ?: ""
