package com.app.pustakam.data.models.response.notes


import com.app.pustakam.util.ContentType
import com.app.pustakam.util.UniqueIdGenerator
import com.app.pustakam.util.getCurrentTimestamp
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Note(
    @SerialName("_id")
    val id: String?,
    var title: String?,
    var updatedAt: String?,
    var createdAt: String?,
    var categoryId: String? ="",
    var isSynced : Boolean? = false,
    var content: ArrayList<NoteContentModel>? = arrayListOf()
) {
    override fun equals(other: Any?): Boolean {
        return other === this
    }

    override fun hashCode(): Int {
       return  31 * ( id.hashCode()
        + updatedAt.hashCode()
        + createdAt.hashCode()
        + title.hashCode()
               +isSynced.hashCode()
        + content?.count().hashCode())
    }
}

@Serializable
sealed class NoteContentModel {
    abstract val position: Long  // Position in the layout
    abstract val updatedAt: String?
    abstract val createdAt: String?
    abstract val type: ContentType
    @SerialName("_id")
    abstract val id: String
    abstract val noteId : String
    override fun hashCode(): Int {
        return  31 * (updatedAt.hashCode() + createdAt.hashCode() + position.hashCode() + type.hashCode() +id.hashCode()+ noteId.hashCode())
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as NoteContentModel

        if (position != other.position) return false
        if (updatedAt != other.updatedAt) return false
        if (createdAt != other.createdAt) return false
        if (type != other.type) return false
        if (id != other.id) return false
        if (noteId != other.noteId) return false

        return true
    }

    data class TextContent(
        var text: String = "",
        override val position: Long,
        override val updatedAt: String? = "${getCurrentTimestamp()}",
        override val createdAt: String?= "${getCurrentTimestamp()}",
        override val type: ContentType = ContentType.TEXT,
        override val id: String = UniqueIdGenerator.generateUniqueId(),
        override val noteId: String ,
    ) : NoteContentModel()

    data class ImageContent(
        val url: String="",
        override val position: Long, 
        override val updatedAt: String? = "${getCurrentTimestamp()}",
        override val createdAt: String? = "${getCurrentTimestamp()}",
        override val type: ContentType = ContentType.IMAGE,
        val localPath: String? = null,
        override val id: String = UniqueIdGenerator.generateUniqueId(),
        override val noteId: String ,
    ) : NoteContentModel()
    data class VideoContent(
        val url: String= "",
        val title : String= "",
        override val position: Long,
        override val updatedAt: String? = "${getCurrentTimestamp()}",
        override val createdAt: String? = "${getCurrentTimestamp()}",
        val duration: Long = 0,
        override val type: ContentType = ContentType.VIDEO,
        val localPath: String? = null,
        override val id: String =  UniqueIdGenerator.generateUniqueId(),
        override val noteId: String ,
    ) : NoteContentModel()

    data class AudioContent(
        override val position: Long,
        override val updatedAt: String? = "${getCurrentTimestamp()}" ,
        override val createdAt: String? = "${getCurrentTimestamp()}",
        override val id: String = UniqueIdGenerator.generateUniqueId(),
        override val type: ContentType = ContentType.AUDIO,
        override val noteId: String,
        val isRecorded:Boolean = false,
        val duration: Long = 0,
        val localPath: String? = null,
        val url: String = "",
        val title: String = "",
    ) : NoteContentModel()
    data class DocContent(
        val url: String ="",
        override val position: Long,
        override val updatedAt: String? = "${getCurrentTimestamp()}",
        override val createdAt: String? = "${getCurrentTimestamp()}",
        override val type: ContentType = ContentType.DOCX,
        val localPath: String? = null,
        override val id: String = UniqueIdGenerator.generateUniqueId(),
        override val noteId: String ,
    ) : NoteContentModel()

    data class Link(
        val url: String ="",
        override val position: Long,
        override val updatedAt: String? = "${getCurrentTimestamp()}" ,
        override val createdAt: String? = "${getCurrentTimestamp()}",
        override val type: ContentType = ContentType.LINK,
        override val id: String = UniqueIdGenerator.generateUniqueId(),
        override val noteId: String
    ) : NoteContentModel()

    data class Location(
        val latitude: Double =0.0,
        val longitude: Double= 0.0,
        val address: String? = null,
        override val position: Long,
        override val updatedAt: String? = "${getCurrentTimestamp()}",
        override val createdAt: String? = "${getCurrentTimestamp()}",
        override val type: ContentType = ContentType.LOCATION,
        override val id: String= UniqueIdGenerator.generateUniqueId(),
        override val noteId: String
    ) : NoteContentModel()
}