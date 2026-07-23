package com.app.pustakam.android.theme

import androidx.compose.ui.graphics.Color

// 🎨 22-Jul-2026 — Granth spec §6 Appearance: four modes, mirroring iOS ThemeMode (ThemeManager.swift)
//   1:1 so both platforms persist the same raw strings via the shared DataStore ("granth.themeMode").
//   SYSTEM is the default: it defers to the OS appearance instead of pinning one scheme.
enum class ThemeMode(val key: String, val title: String) {
    SYSTEM("system", "System"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark"),
    AMOLED("amoled", "AMOLED");

    /** true when true-black OLED surfaces should replace the warm dark ones. */
    val isAmoled: Boolean get() = this == AMOLED

    // 🎨 miniature of the ramp each tile represents, so AMOLED reads as distinct from Dark (spec §6)
    val swatch: List<Color>
        get() = when (this) {
            SYSTEM -> listOf(GranthParchmentSwatch, GranthDarkSwatch)
            LIGHT -> listOf(GranthParchmentSwatch, GranthSandSwatch)
            DARK -> listOf(GranthDarkSurfaceSwatch, GranthDarkSwatch)
            AMOLED -> listOf(GranthAmoledSurfaceSwatch, Color.Black)
        }

    companion object {
        /** Restores a persisted pick; unknown/absent values fall back to SYSTEM like iOS. */
        fun from(key: String?): ThemeMode =
            entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

// 🎨 22-Jul-2026 — swatch-only literals (spec §2.2). Kept local to the picker so the semantic
//   tokens in Color.kt stay the single source of truth for actual surfaces.
private val GranthParchmentSwatch = Color(0xFFF2E8D2)
private val GranthSandSwatch = Color(0xFFE6D7B8)
private val GranthDarkSwatch = Color(0xFF16120D)
private val GranthDarkSurfaceSwatch = Color(0xFF221B14)
private val GranthAmoledSurfaceSwatch = Color(0xFF0C0A07)
