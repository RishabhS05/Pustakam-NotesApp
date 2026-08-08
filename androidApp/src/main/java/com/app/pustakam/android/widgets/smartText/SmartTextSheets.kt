package com.app.pustakam.android.widgets.smartText

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.pustakam.android.widgets.colorPalete.ColorSelector
import com.app.pustakam.core.richtext.model.ParagraphStyle
import com.app.pustakam.core.richtext.model.TextAlign
import com.app.pustakam.core.richtext.presentation.SmartTextCatalog
import com.app.pustakam.core.richtext.presentation.SmartTextCommands
import com.app.pustakam.core.richtext.presentation.TableCommand

// which auxiliary sheet the editor is showing, if any
enum class SmartTextSheet {
    NONE,
    TEXT_STYLE,
    TEXT_COLOR,
    BACKGROUND_COLOR,
    FONT_SIZE,
    ALIGN,
    TABLE,
    LINK,
    FIND_REPLACE;

    companion object {
        fun fromIndex(index: Int): SmartTextSheet = when (index) {
            SmartTextCommands.SHEET_TEXT_STYLE -> TEXT_STYLE
            SmartTextCommands.SHEET_TEXT_COLOR -> TEXT_COLOR
            SmartTextCommands.SHEET_BACKGROUND_COLOR -> BACKGROUND_COLOR
            SmartTextCommands.SHEET_FONT_SIZE -> FONT_SIZE
            SmartTextCommands.SHEET_ALIGN -> ALIGN
            SmartTextCommands.SHEET_TABLE -> TABLE
            SmartTextCommands.SHEET_LINK -> LINK
            SmartTextCommands.SHEET_FIND_REPLACE -> FIND_REPLACE
            else -> NONE
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartTextSheetHost(
    sheet: SmartTextSheet,
    currentStyle: ParagraphStyle,
    currentAlign: TextAlign,
    currentFontSize: Float?,
    currentLink: String?,
    searchQuery: String,
    replacement: String,
    matchCount: Int,
    currentMatch: Int,
    tableRowCount: Int,
    tableColumnCount: Int,
    onDismiss: () -> Unit,
    onStyle: (ParagraphStyle) -> Unit,
    onAlign: (TextAlign) -> Unit,
    onColor: (String?) -> Unit,
    onFontSize: (Float?) -> Unit,
    onLink: (String?) -> Unit,
    onTable: (TableCommand) -> Unit,
    onInsertTable: (Int, Int) -> Unit,
    onSearchQuery: (String) -> Unit,
    onReplacement: (String) -> Unit,
    onFindNext: () -> Unit,
    onFindPrevious: () -> Unit,
    onReplaceCurrent: () -> Unit,
    onReplaceAll: () -> Unit
) {
    if (sheet == SmartTextSheet.NONE) return
    val colors = SmartTextTokens.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.onSurface
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            when (sheet) {
                SmartTextSheet.TEXT_STYLE -> TextStyleSheet(currentStyle, onStyle)
                SmartTextSheet.TEXT_COLOR ->
                    ColorSheet("Text colour", SmartTextCatalog.textColors, onColor)

                SmartTextSheet.BACKGROUND_COLOR ->
                    ColorSheet("Background colour", SmartTextCatalog.highlightColors, onColor)

                SmartTextSheet.FONT_SIZE -> FontSizeSheet(currentFontSize, onFontSize)
                SmartTextSheet.ALIGN -> AlignSheet(currentAlign, onAlign)
                SmartTextSheet.TABLE ->
                    TableSheet(tableRowCount, tableColumnCount, onInsertTable, onTable)
                SmartTextSheet.LINK -> LinkSheet(currentLink, onLink)
                SmartTextSheet.FIND_REPLACE -> FindReplaceSheet(
                    query = searchQuery,
                    replacement = replacement,
                    matchCount = matchCount,
                    currentMatch = currentMatch,
                    onSearchQuery = onSearchQuery,
                    onReplacement = onReplacement,
                    onFindNext = onFindNext,
                    onFindPrevious = onFindPrevious,
                    onReplaceCurrent = onReplaceCurrent,
                    onReplaceAll = onReplaceAll
                )

                SmartTextSheet.NONE -> Unit
            }
        }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text = text,
        style = TextStyle(
            color = SmartTextTokens.colors.onSurface,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        ),
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun TextStyleSheet(current: ParagraphStyle, onStyle: (ParagraphStyle) -> Unit) {
    val colors = SmartTextTokens.colors
    SheetTitle("Text style")
    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        SmartTextCatalog.paragraphStyles.forEach { style ->
            val selected = style == current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .background(
                        if (selected) colors.accentSoft else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onStyle(style) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = SmartTextCatalog.paragraphLabel(style),
                    style = TextStyle(
                        color = if (selected) colors.accent else colors.onSurface,
                        fontSize = (15 * style.relativeSize.coerceAtMost(1.6f)).sp,
                        fontWeight = FontWeight(style.weight)
                    )
                )
            }
        }
    }
}

@Composable
private fun ColorSheet(title: String, palette: List<String>, onColor: (String?) -> Unit) {
    val colors = SmartTextTokens.colors
    var picked by remember { mutableStateOf(SmartTextStyleMapper.parseColor(palette.first())) }
    SheetTitle(title)
    ColorSelector(initial = picked) { picked = it }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { onColor(picked.toHexString()) },
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.onAccent
            )
        ) {
            Text("Apply")
        }
        TextButton(onClick = { onColor(null) }) {
            Text("Remove colour", color = colors.accent)
        }
    }
}

@Composable
private fun FontSizeSheet(current: Float?, onFontSize: (Float?) -> Unit) {
    val colors = SmartTextTokens.colors
    var size by remember { mutableFloatStateOf(current ?: 16f) }
    SheetTitle("Font size")
    Text("${size.toInt()} pt", style = TextStyle(color = colors.onSurfaceMuted, fontSize = 14.sp))
    Slider(
        value = size,
        onValueChange = { size = it },
        onValueChangeFinished = { onFontSize(size) },
        valueRange = 10f..48f,
        steps = 37,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    TextButton(onClick = { onFontSize(null) }, modifier = Modifier.padding(bottom = 16.dp)) {
        Text("Reset to default", color = colors.accent)
    }
}

@Composable
private fun AlignSheet(current: TextAlign, onAlign: (TextAlign) -> Unit) {
    val colors = SmartTextTokens.colors
    SheetTitle("Alignment")
    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        SmartTextCatalog.alignments.forEach { align ->
            val label = SmartTextCatalog.alignLabel(align)
            val selected = align == current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .background(
                        if (selected) colors.accentSoft else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onAlign(align) }
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Text(
                    text = label,
                    style = TextStyle(
                        color = if (selected) colors.accent else colors.onSurface,
                        fontSize = 15.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun TableSheet(
    rowCount: Int,
    columnCount: Int,
    onInsertTable: (Int, Int) -> Unit,
    onTable: (TableCommand) -> Unit
) {
    val colors = SmartTextTokens.colors
    SheetTitle("Table")
    Row(
        modifier = Modifier.padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SmartTextCatalog.tablePresetRows.indices.forEach { index ->
            val rows = SmartTextCatalog.tablePresetRows[index]
            val columns = SmartTextCatalog.tablePresetColumns[index]
            Button(
                onClick = { onInsertTable(rows, columns) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accentSoft,
                    contentColor = colors.accent
                )
            ) {
                Text("$rows × $columns")
            }
        }
    }
    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        listOf(
            "Add row" to TableCommand.AddRow(rowCount),
            "Add column" to TableCommand.AddColumn(columnCount),
            "Delete last row" to TableCommand.DeleteRow(rowCount - 1),
            "Delete last column" to TableCommand.DeleteColumn(columnCount - 1),
            "Toggle header row" to TableCommand.ToggleHeaderRow,
            "Toggle header column" to TableCommand.ToggleHeaderColumn
        ).forEach { (label, command) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTable(command) }
                    .padding(vertical = 12.dp)
            ) {
                Text(label, style = TextStyle(color = colors.onSurface, fontSize = 15.sp))
            }
        }
    }
}

@Composable
private fun LinkSheet(current: String?, onLink: (String?) -> Unit) {
    val colors = SmartTextTokens.colors
    var url by remember { mutableStateOf(current.orEmpty()) }
    SheetTitle(if (current == null) "Insert link" else "Edit link")
    OutlinedTextField(
        value = url,
        onValueChange = { url = it },
        placeholder = { Text("https://", color = colors.onSurfaceMuted) },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = colors.accent,
            unfocusedIndicatorColor = colors.divider,
            cursorColor = colors.accent,
            focusedTextColor = colors.onSurface,
            unfocusedTextColor = colors.onSurface
        ),
        modifier = Modifier.fillMaxWidth()
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { onLink(url.takeIf { it.isNotBlank() }) },
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.onAccent
            )
        ) {
            Text("Apply")
        }
        if (current != null) {
            TextButton(onClick = { onLink(null) }) {
                Text("Remove link", color = colors.accent)
            }
        }
    }
}

@Composable
private fun FindReplaceSheet(
    query: String,
    replacement: String,
    matchCount: Int,
    currentMatch: Int,
    onSearchQuery: (String) -> Unit,
    onReplacement: (String) -> Unit,
    onFindNext: () -> Unit,
    onFindPrevious: () -> Unit,
    onReplaceCurrent: () -> Unit,
    onReplaceAll: () -> Unit
) {
    val colors = SmartTextTokens.colors
    SheetTitle("Find and replace")
    OutlinedTextField(
        value = query,
        onValueChange = onSearchQuery,
        placeholder = { Text("Find", color = colors.onSurfaceMuted) },
        singleLine = true,
        colors = sheetFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        text = if (matchCount == 0) "No matches" else "${currentMatch + 1} of $matchCount",
        style = TextStyle(color = colors.onSurfaceMuted, fontSize = 13.sp),
        modifier = Modifier.padding(vertical = 6.dp)
    )
    OutlinedTextField(
        value = replacement,
        onValueChange = onReplacement,
        placeholder = { Text("Replace with", color = colors.onSurfaceMuted) },
        singleLine = true,
        colors = sheetFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TextButton(onClick = onFindPrevious) { Text("Previous", color = colors.accent) }
        TextButton(onClick = onFindNext) { Text("Next", color = colors.accent) }
        TextButton(onClick = onReplaceCurrent) { Text("Replace", color = colors.accent) }
        TextButton(onClick = onReplaceAll) { Text("All", color = colors.accent) }
    }
}

@Composable
private fun sheetFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedIndicatorColor = SmartTextTokens.colors.accent,
    unfocusedIndicatorColor = SmartTextTokens.colors.divider,
    cursorColor = SmartTextTokens.colors.accent,
    focusedTextColor = SmartTextTokens.colors.onSurface,
    unfocusedTextColor = SmartTextTokens.colors.onSurface
)
