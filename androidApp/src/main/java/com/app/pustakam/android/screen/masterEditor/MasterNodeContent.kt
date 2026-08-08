package com.app.pustakam.android.screen.masterEditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.pustakam.android.widgets.document.InlineBookFileWidget
import com.app.pustakam.android.widgets.image.ImageCard
import com.app.pustakam.android.widgets.masterEditor.MasterTextWidget
import com.app.pustakam.android.widgets.smartText.SmartTextTokens
import com.app.pustakam.android.widgets.video.VideoCard
import com.app.pustakam.core.common.util.ContentType
import com.app.pustakam.core.model.models.response.notes.NoteContentModel
import com.app.pustakam.core.model.models.response.notes.getMediaUrl
import com.app.pustakam.core.richtext.master.model.CanvasNode
import com.app.pustakam.core.richtext.master.model.CanvasNodeKind
import com.app.pustakam.core.richtext.master.presentation.MasterTextState

@Composable
fun MasterNodeContent(
    node: CanvasNode,
    isEditing: Boolean,
    scale: Float,
    textState: MasterTextState?,
    content: NoteContentModel?,
    onTextIntent: (com.app.pustakam.core.richtext.master.presentation.MasterTextIntent) -> Unit,
    onFocused: () -> Unit,
    onOpenMedia: () -> Unit
) {
    val colors = SmartTextTokens.colors

    when (node.kind) {
        CanvasNodeKind.MASTER_TEXT -> {
            if (textState == null) {
                MasterNodePlaceholder("Empty text")
            } else {
                MasterTextWidget(
                    state = textState,
                    modifier = Modifier.padding(16.dp),
                    scale = scale,
                    readOnly = !isEditing,
                    onIntent = onTextIntent,
                    onFocusChanged = { if (it) onFocused() }
                )
            }
        }

        CanvasNodeKind.MEDIA -> {
            val media = content as? NoteContentModel.MediaContent
            when (media?.type) {
                ContentType.IMAGE, ContentType.GIF -> ImageCard(
                    modifier = Modifier.fillMaxSize(),
                    imageUrl = media.getMediaUrl(),
                    onClick = onOpenMedia
                )

                ContentType.VIDEO -> VideoCard(
                    modifier = Modifier.fillMaxSize(),
                    contentVideo = media,
                    onClick = onOpenMedia
                )

                else -> MasterNodePlaceholder("Media")
            }
        }

        CanvasNodeKind.DOCUMENT -> {
            val media = content as? NoteContentModel.MediaContent
            if (media == null) {
                MasterNodePlaceholder("Document")
            } else {
                InlineBookFileWidget(
                    media = media,
                    modifier = Modifier.fillMaxSize(),
                    onOpenFull = onOpenMedia
                )
            }
        }

        CanvasNodeKind.LINK -> {
            val link = content as? NoteContentModel.Link
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = link?.url.orEmpty().ifEmpty { "Link" },
                    style = TextStyle(color = colors.accent, fontSize = 15.sp)
                )
            }
        }

        CanvasNodeKind.LOCATION -> {
            val location = content as? NoteContentModel.Location
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = location?.address ?: "Location",
                    style = TextStyle(color = colors.onSurface, fontSize = 15.sp)
                )
            }
        }

        else -> MasterNodePlaceholder(node.kind.name)
    }
}

@Composable
private fun MasterNodePlaceholder(label: String) {
    val colors = SmartTextTokens.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.codeBackground),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TextStyle(color = colors.onSurfaceMuted, fontSize = 13.sp)
        )
    }
}
