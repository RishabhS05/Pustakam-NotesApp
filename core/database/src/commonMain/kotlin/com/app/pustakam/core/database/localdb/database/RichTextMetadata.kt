package com.app.pustakam.core.database.localdb.database

import kotlinx.serialization.Serializable

@Serializable
enum class TextColorType {
    DEFAULT,
    ACCENT,
    ERROR
}
@Serializable
enum class  TextStyleType{
    NORMAL,
    BOLD,
    ITALIC,
    UNDERLINE,
    STRIKETHROUGH,
    HIGHLIGHT
}
@Serializable
data class RichTextMetadata(
    val spans: List<TextSpan> = null ?: emptyList()
)

@Serializable
data class TextSpan(
    val start: Int = 0,
    val end: Int = 0,
    val color: String? = "",
    val style: TextStyleType = TextStyleType.NORMAL,
    val weight: Int = 400,
    val colorType: TextColorType = TextColorType.DEFAULT
)