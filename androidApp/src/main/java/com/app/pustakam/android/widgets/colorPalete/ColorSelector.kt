package com.app.pustakam.android.widgets.colorPalete// build.gradle (module)
// implementation("androidx.compose.material3:material3:<latest>")
// implementation("androidx.compose.foundation:foundation:<latest>")

import DarkBrown1
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.widgets.PrimaryFilledButton
import toHexString
import kotlin.math.roundToInt

@Composable
fun ColorSelector(
    modifier: Modifier = Modifier,
    initial: Color =  DarkBrown1,
    showAlpha: Boolean = false,
    onColorChanged: (Color) -> Unit
) {
    // Convert initial color -> HSV once
    val (initH, initS, initV) = remember(initial) { colorToHSV(initial) }
    var hue by rememberSaveable { mutableFloatStateOf(initH) }     // 0..360
    var sat by rememberSaveable { mutableFloatStateOf(initS) }     // 0..1
    var value by rememberSaveable { mutableFloatStateOf(initV) }   // 0..1
    var alpha by rememberSaveable { mutableFloatStateOf(initial.alpha) } // 0..1

    val current = remember(hue, sat, value, alpha) { Color.hsv(hue, sat, value, alpha) }

    // Call back up whenever it changes
    LaunchedEffect(current) { onColorChanged(current) }

    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Preview swatch + hex
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(current)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraLarge)
            )
            Text(
                text =current.toHexString(),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.weight(1f))
            // Quick contrast preview chip
            AssistChip(
                onClick = {},
                label = { Text("Aa") },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = current,
                    labelColor = if (current.luminance() > 0.5f) Color.Black else Color.White
                )
            )
        }

        // Quick palette (material-ish)
        val swatches = listOf(
            0xFF000000, 0xFFFFFFFF, 0xFFEF4444, 0xFFF59E0B, 0xFF10B981, 0xFF3B82F6,
            0xFF8B5CF6, 0xFFEC4899, 0xFFFB923C, 0xFF22D3EE, 0xFF84CC16, 0xFF14B8A6
        ).map { Color(it) }

        Text("Swatches", style = MaterialTheme.typography.labelLarge)
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier.height(96.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(swatches) { c ->
                Box(
                    Modifier
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.large)
                        .background(c)
                        .border(
                            width = if (colorsClose(c, current)) 2.dp else 1.dp,
                            color = if (colorsClose(c, current)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = MaterialTheme.shapes.large
                        )
                        .clickable {
                            val (h, s, v) = colorToHSV(c)
                            hue = h; sat = s; value = v; alpha = c.alpha
                        }
                )
            }
        }

        // Sliders
        LabeledSlider(
            label = "Hue",
            value = hue,
            valueRange = 0f..360f,
            onValueChange = { hue = it },
            trackBrush = Brush.horizontalGradient((0..6).map { step ->
                val h = step * 60f
                Color.hsv(h, 1f, 1f)
            })
        )
        LabeledSlider(
            label = "Saturation",
            value = sat,
            valueRange = 0f..1f,
            onValueChange = { sat = it },
            trackBrush = Brush.horizontalGradient(listOf(
                Color.hsv(hue, 0f, value),
                Color.hsv(hue, 1f, value)
            ))
        )
        LabeledSlider(
            label = "Value",
            value = value,
            valueRange = 0f..1f,
            onValueChange = { value = it },
            trackBrush = Brush.horizontalGradient(listOf(
                Color.hsv(hue, sat, 0f),
                Color.hsv(hue, sat, 1f)
            ))
        )
        if (showAlpha) {
            // Checkerboard background for alpha preview
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(checkerBrush())
                    .clip(RectangleShape)
            )
            LabeledSlider(
                label = "Alpha",
                value = alpha,
                valueRange = 0f..1f,
                onValueChange = { alpha = it },
                trackBrush = Brush.horizontalGradient(listOf(
                    Color.hsv(hue, sat, value, 0f),
                    Color.hsv(hue, sat, value, 1f)
                ))
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    trackBrush: Brush? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(8.dp))
            Text(
                when (label) {
                    "Hue" -> "${value.roundToInt()}°"
                    "Alpha" -> "${(value * 100).roundToInt()}%"
                    "Saturation", "Value" -> String.format("%.2f", value)
                    else -> value.toString()
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            track = {
                SliderDefaults.Track(
                    sliderState = it,
                    modifier = Modifier
                        .height(8.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(trackBrush ?: Brush.horizontalGradient(listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surfaceVariant
                        )))
                )
            }
        )
    }
}

/** Helpers **/

private fun colorToHSV(color: Color): Triple<Float, Float, Float> {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(color.toArgb(), hsv)
    return Triple(hsv[0], hsv[1], hsv[2])
}

private fun colorsClose(a: Color, b: Color): Boolean {
    fun c(c: Color) = intArrayOf(
        (c.red * 255).roundToInt(),
        (c.green * 255).roundToInt(),
        (c.blue * 255).roundToInt()
    )
    val (ar, ag, ab) = c(a)
    val (br, bg, bb) = c(b)
    return (kotlin.math.abs(ar - br) +
            kotlin.math.abs(ag - bg) +
            kotlin.math.abs(ab - bb)) < 20
}

@Composable
private fun checkerBrush(size: Int = 6): Brush {
    // lightweight checkerboard for alpha track backdrop
    val light = MaterialTheme.colorScheme.surfaceVariant
    val dark = MaterialTheme.colorScheme.outlineVariant
    val squares = List(8) { row ->
        List(8) { col -> if ((row + col) % 2 == 0) light else dark }
    }.flatten()
    return Brush.horizontalGradient(squares)
}
