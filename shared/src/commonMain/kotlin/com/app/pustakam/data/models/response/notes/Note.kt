package com.app.pustakam.data.models.response.notes


import com.app.pustakam.util.ContentType
import com.app.pustakam.util.UniqueIdGenerator
import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val _id: String? ,
    var title: String? ,
    var updatedAt: String?,
    var createdAt: String?,
    var categoryId: String? ="",
    var isSynced : Boolean? = false,
    var content: List<NoteContentModel>? = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        return other === this
    }

    override fun hashCode(): Int {
       return  31 * ( _id.hashCode()
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
    abstract val id: String
    abstract val noteId : String
    override fun hashCode(): Int {
        return  31 * (updatedAt.hashCode() + createdAt.hashCode() + position.hashCode())
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
    @Serializable
    data class Text(
        val text: String,
        override val position: Long,
        override val updatedAt: String?,
        override val createdAt: String?,
        override val type: ContentType = ContentType.TEXT,
        override val id: String,
        override val noteId: String = UniqueIdGenerator.generateUniqueId(),
    ) : NoteContentModel()
    @Serializable
    data class Image(
        val url: String,
        override val position: Long, override val updatedAt: String?,
        override val createdAt: String?,
        override val type: ContentType = ContentType.IMAGE,
        val localPath: String? = null,
        override val id: String,
        override val noteId: String = UniqueIdGenerator.generateUniqueId(),
    ) : NoteContentModel()
    @Serializable
    data class Video(
        val url: String,
        override val position: Long,
        override val updatedAt: String?,
        override val createdAt: String?,
        val duration: Long,
        override val type: ContentType = ContentType.VIDEO,
        val localPath: String? = null,
        override val id: String,
        override val noteId: String =  UniqueIdGenerator.generateUniqueId(),
    ) : NoteContentModel()
    @Serializable
    data class Audio(
        val url: String,
        override val position: Long,
        override val updatedAt: String?,
        override val createdAt: String?,
        val duration: Long,
        override val type: ContentType = ContentType.AUDIO,
        val localPath: String? = null,
        override val id: String,
        override val noteId: String = UniqueIdGenerator.generateUniqueId(),
    ) : NoteContentModel()
    @Serializable
    data class Doc(
        val url: String, override val position: Long,
        override val updatedAt: String?,
        override val createdAt: String?,
        override val type: ContentType = ContentType.DOCX,
        val localPath: String? = null,
        override val id: String,
        override val noteId: String = UniqueIdGenerator.generateUniqueId(),
    ) : NoteContentModel()
    @Serializable
    data class Link(
        val url: String,
        override val position: Long,
        override val updatedAt: String?,
        override val createdAt: String?,
        override val type: ContentType = ContentType.LINK,
        override val id: String,
        override val noteId: String= UniqueIdGenerator.generateUniqueId()
    ) : NoteContentModel()

    data class Location(
        val latitude: Double, val longitude: Double, val address: String?,
        override val position: Long,
        override val updatedAt: String?,
        override val createdAt: String?,
        override val type: ContentType = ContentType.LOCATION,
        override val id: String,
        override val noteId: String = UniqueIdGenerator.generateUniqueId()
    ) : NoteContentModel()
}