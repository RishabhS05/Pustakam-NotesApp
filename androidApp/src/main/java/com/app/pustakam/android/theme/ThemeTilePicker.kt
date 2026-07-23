package com.app.pustakam.android.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.MyApplicationTheme
// 🎨 Color.kt / Shape.kt live in the root package (no package decl), same as PustakmApplicationTheme.kt
import radiusMd
import radiusSm

// 🎨 22-Jul-2026 — Granth spec §6 Appearance tiles, the Compose twin of iOS ThemeTilePicker.swift.
//   Reusable: any screen can drop this in and hoist the selection. Each tile previews its own ramp
//   so the choice reads at a glance.
@Composable
fun ThemeTilePicker(
    selection: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    // 🎨 four fixed tiles read better than a grid that reflows at this count
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ThemeMode.entries.forEach { mode ->
            ThemeTile(
                mode = mode,
                isSelected = selection == mode,
                onTap = { onSelect(mode) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// 🎨 22-Jul-2026 — one Appearance tile: swatch preview + label + 2dp accent selection ring (spec §6).
@Composable
private fun ThemeTile(
    mode: ThemeMode,
    isSelected: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 🎨 animate the ring so selection feels native rather than snapping
    val ringWidth by animateDpAsState(if (isSelected) 2.dp else 1.dp, label = "ringWidth")
    val ringColor by animateColorAsState(
        if (isSelected) colorScheme.primary else colorScheme.outlineVariant, label = "ringColor"
    )
    val labelColor by animateColorAsState(
        if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant, label = "labelColor"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(radiusMd))
            .background(colorScheme.surface)
            .border(ringWidth, ringColor, RoundedCornerShape(radiusMd))
            .clickable(role = Role.RadioButton, onClick = onTap)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(radiusSm))
                .background(Brush.linearGradient(mode.swatch))
                .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(radiusSm)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = mode.icon,
                contentDescription = null,
                tint = mode.iconTint,
                modifier = Modifier.height(18.dp)
            )
        }

        Text(
            text = mode.title,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor
        )
    }
}

// 🎨 22-Jul-2026 — tile iconography, mirroring the SF Symbols used on iOS.
private val ThemeMode.icon: ImageVector
    get() = when (this) {
        ThemeMode.SYSTEM -> Icons.Default.PhoneAndroid
        ThemeMode.LIGHT -> Icons.Default.LightMode
        ThemeMode.DARK -> Icons.Default.DarkMode
        ThemeMode.AMOLED -> Icons.Default.Circle
    }

private val ThemeMode.iconTint: Color
    get() = when (this) {
        ThemeMode.LIGHT -> GranthInk
        ThemeMode.SYSTEM -> Color.White
        ThemeMode.DARK, ThemeMode.AMOLED -> GranthSaffron
    }

// MARK: - Previews (sample data)

// 🎨 22-Jul-2026 — preview with sample selection, per project convention.
@Preview(name = "Theme tiles — Light", showBackground = true)
@Composable
private fun ThemeTilePickerLightPreview() {
    var mode by remember { mutableStateOf(ThemeMode.LIGHT) }
    MyApplicationTheme(isDarkTheme = false) {
        Surface(color = colorScheme.background) {
            ThemeTilePicker(selection = mode, onSelect = { mode = it }, modifier = Modifier.padding(20.dp))
        }
    }
}

@Preview(name = "Theme tiles — Dark", showBackground = true)
@Composable
private fun ThemeTilePickerDarkPreview() {
    var mode by remember { mutableStateOf(ThemeMode.DARK) }
    MyApplicationTheme(isDarkTheme = true) {
        Surface(color = colorScheme.background) {
            ThemeTilePicker(selection = mode, onSelect = { mode = it }, modifier = Modifier.padding(20.dp))
        }
    }
}

@Preview(name = "Theme tiles — AMOLED", showBackground = true)
@Composable
private fun ThemeTilePickerAmoledPreview() {
    var mode by remember { mutableStateOf(ThemeMode.AMOLED) }
    MyApplicationTheme(isDarkTheme = true, isAmoled = true) {
        Surface(color = colorScheme.background) {
            ThemeTilePicker(selection = mode, onSelect = { mode = it }, modifier = Modifier.padding(20.dp))
        }
    }
}
