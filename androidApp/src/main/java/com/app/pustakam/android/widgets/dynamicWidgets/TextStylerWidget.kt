package com.app.pustakam.android.widgets.dynamicWidgets

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatIndentDecrease
import androidx.compose.material.icons.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.MyApplicationTheme

/**
 * This is  a text formatting widget that provides text
 * styling options such as bold, italic, and underline.
 * */

enum class TextStyleType {
    BOLD, ITALIC, NORMAL, UNDERLINE, STRIKETHROUGH, NUMBERED_LIST, LINE_SPACING, ALIGN_CENTER,
   BULLET_LIST, JUSTIFY, SPACING_LEFT, SPACING_RIGHT, COLOR_PALETTE
}

data class NoteState(
    val content: String = "",
    val selection: TextRange = TextRange(0, 0),
    val activeStyles: Set<TextStyleType> = emptySet()
)

sealed class NoteIntent {
    data class UpdateSelection(val start: Int, val end: Int) : NoteIntent()
    data class ApplyStyle(val style: TextStyleType) : NoteIntent()
}
@Composable
fun TextEditorWidget( modifier: Modifier = Modifier,action : (TextStyleType)-> Unit,) {

val contentPaddingValues = PaddingValues(4.dp)
    Surface(
        tonalElevation = 16.dp,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.padding(2.dp)
    ) {
        Column{
            Row {
                TextButton(onClick = { action(TextStyleType.BOLD) },
                    contentPadding = contentPaddingValues,
                    ) {
                    Icon(Icons.Default.FormatBold, contentDescription = "Format Text Bold")
                }
                TextButton(onClick = { action(TextStyleType.ITALIC) },contentPadding = contentPaddingValues,) {
                    Icon(Icons.Default.FormatItalic, contentDescription = "Format Text ITALIC")
                }
                TextButton(
                    onClick = { action(TextStyleType.UNDERLINE) },
                    contentPadding = contentPaddingValues,
                ) {
                    Icon(Icons.Default.FormatUnderlined, contentDescription = "Format Text Bold" )
                }
                TextButton(
                    onClick = { action(TextStyleType.STRIKETHROUGH) },
                    contentPadding = contentPaddingValues,
                ) {
                    Icon(Icons.Default.FormatStrikethrough,
                        contentDescription = "Format Text Bold", )
                }
            }
            Row {
                TextButton(onClick = {
                    action(TextStyleType.NUMBERED_LIST)
                },
                    contentPadding = contentPaddingValues,
                ) {
                    Icon(Icons.Default.FormatListNumbered, contentDescription = "Format Text Bold")
                }
                TextButton(onClick = {
                    action(TextStyleType.LINE_SPACING)
                },
                    contentPadding = contentPaddingValues,) {
                    Icon(Icons.Default.FormatLineSpacing, contentDescription = "Format Text Bold")
                }
                TextButton(onClick = {
                    action(TextStyleType.ALIGN_CENTER)
                },
                    contentPadding = contentPaddingValues,) {
                    Icon(Icons.Default.FormatAlignCenter, contentDescription = "Format Text Bold")
                }
                TextButton(onClick = {
                    action(TextStyleType.JUSTIFY)
                },
                ) {
                    Icon(Icons.Default.FormatAlignJustify, contentDescription = "Format Text Bold")
                }
            }
            Row {
                TextButton(onClick = {
                    action(TextStyleType.BULLET_LIST)
                },
                    contentPadding = contentPaddingValues,
                ) {
                    Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Format Text Bold")
                }
                TextButton(onClick = {
                    action(TextStyleType.SPACING_RIGHT)
                },
                    contentPadding = contentPaddingValues,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.FormatIndentIncrease,
                        contentDescription = "Format Text Bold"
                    )
                }
                TextButton(onClick = {
                    action(TextStyleType.SPACING_LEFT)
                },
                    contentPadding = contentPaddingValues,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.TextSnippet,
                        contentDescription = "Format Text Bold"
                    )
                }
                TextButton(onClick = { action(TextStyleType.COLOR_PALETTE) },
                    contentPadding = contentPaddingValues,) {
                    Icon(Icons.Default.Palette,
                        contentDescription = "Format Text Bold")
                }
            }
        }
    }
}

@Preview("default")
@Preview("dark theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview("dark theme", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview("large font", fontScale = 2f)
@Composable
private fun LoadImagePrev() {
    MyApplicationTheme {
       TextEditorWidget(){}
    }
}

@Preview("default")
@Preview("dark theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview("large font", fontScale = 2f)
@Composable
private fun NoteEditorPreview() {
    /** App Theme */
    MyApplicationTheme {
        /** View */
        TextEditorWidget(
            action = {}
        )

    }
}

