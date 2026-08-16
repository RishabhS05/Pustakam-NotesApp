package com.app.pustakam.android.widgets.smartText

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.widgets.masterEditor.MasterTextRenderer
import com.app.pustakam.core.richtext.master.model.CanvasNode
import com.app.pustakam.core.richtext.master.presentation.MasterTextState
import com.app.pustakam.core.richtext.model.RichBlock
import com.app.pustakam.core.richtext.model.RichDocument

// 📖 15-Aug-2026: read-only twin of MasterTextWidget. It builds the SAME MasterTextState and calls
//   the SAME MasterTextRenderer — annotate() for styling, indentation, alignment and line height,
//   drawMarkers() for bullets, numbers and checkboxes — with the text field, toolbars and reducer
//   left out. What the editor writes is exactly what this draws.
@Composable
fun SmartTextDisplay(
    block: RichBlock,
    modifier: Modifier = Modifier,
    colors: SmartTextColors = SmartTextTokens.colors,
    baseSize: TextUnit = SmartTextTokens.baseFontSize * CanvasNode.BASE_FONT_SCALE,
) {
    when (block) {
        is RichBlock.Text -> MasterParagraph(block, modifier, colors, baseSize)
        is RichBlock.Divider -> DividerRow(colors, modifier)
        is RichBlock.Code -> CodeRow(block, colors, modifier)
        is RichBlock.Table -> TableRow(block, colors, baseSize, modifier)
    }
}

@Composable
private fun MasterParagraph(
    block: RichBlock.Text,
    modifier: Modifier,
    colors: SmartTextColors,
    baseSize: TextUnit,
) {
    val state = remember(block) { MasterTextState.of(RichDocument(listOf(block))) }
    val annotated = remember(state, colors, baseSize) {
        MasterTextRenderer.annotate(state, colors, baseSize)
    }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    var layout by remember(block) { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = annotated,
        style = MasterTextRenderer.textStyle(colors.onSurface, baseSize),
        onTextLayout = { layout = it },
        modifier = modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                // the editor's own marker pass — same glyphs, same gutter, same geometry
                layout?.let {
                    MasterTextRenderer.drawMarkers(this, state, it, measurer, colors, baseSize, density)
                }
            },
    )
}

@Composable
private fun DividerRow(colors: SmartTextColors, modifier: Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(1.dp)
            .background(colors.divider)
    )
}

@Composable
private fun CodeRow(block: RichBlock.Code, colors: SmartTextColors, modifier: Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(colors.codeBackground, RoundedCornerShape(10.dp))
            .horizontalScroll(rememberScrollState())
            .padding(10.dp)
    ) {
        Text(
            block.code,
            style = TextStyle(
                color = colors.onSurface,
                fontSize = SmartTextTokens.codeFontSize,
                fontFamily = FontFamily.Monospace,
            ),
        )
    }
}

@Composable
private fun TableRow(
    block: RichBlock.Table,
    colors: SmartTextColors,
    baseSize: TextUnit,
    modifier: Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(colors.surface, RoundedCornerShape(8.dp))
            .padding(6.dp)
    ) {
        block.data.rows.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.cells.forEach { cell ->
                    Text(
                        cell.text,
                        style = TextStyle(color = colors.onSurface, fontSize = baseSize),
                        modifier = Modifier.weight(1f).padding(4.dp),
                    )
                }
            }
        }
    }
}
