package com.hackeros.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.hackeros.app.data.model.AppTheme
import com.hackeros.app.data.model.ThemeId

// All themes matching the React app
val THEMES: Map<ThemeId, AppTheme> = mapOf(
    // Default theme as of v0.5 - clean gray/white/black, no accent hue.
    ThemeId.MONOCHROME to AppTheme(
        id = ThemeId.MONOCHROME,
        primary = 0xFFE4E4E7,    // Zinc 200 (near-white accent)
        background = 0xFF0A0A0A, // near-black
        card = 0xFF1C1C1E        // neutral dark gray
    ),
    ThemeId.HACKER to AppTheme(
        id = ThemeId.HACKER,
        primary = 0xFF10B981,    // Emerald 500
        background = 0xFF0A0A0C,
        card = 0xFF17171C
    ),
    ThemeId.CYBERPUNK to AppTheme(
        id = ThemeId.CYBERPUNK,
        primary = 0xFFEC4899,    // Pink 500
        background = 0xFF1C0814,
        card = 0xFF2E1022
    ),
    ThemeId.OCEAN to AppTheme(
        id = ThemeId.OCEAN,
        primary = 0xFF06B6D4,    // Cyan 500
        background = 0xFF081420,
        card = 0xFF0F2337
    ),
    ThemeId.SUNSET to AppTheme(
        id = ThemeId.SUNSET,
        primary = 0xFFF97316,    // Orange 500
        background = 0xFF1C0A05,
        card = 0xFF32140A
    ),
    ThemeId.MATRIX to AppTheme(
        id = ThemeId.MATRIX,
        primary = 0xFF22C55E,    // Green 500
        background = 0xFF000000,
        card = 0xFF141414
    ),
    ThemeId.CRIMSON to AppTheme(
        id = ThemeId.CRIMSON,
        primary = 0xFFDC2626,    // Red 600
        background = 0xFF140505,
        card = 0xFF280A0A
    ),
    ThemeId.ROYAL to AppTheme(
        id = ThemeId.ROYAL,
        primary = 0xFFEAB308,    // Yellow 500
        background = 0xFF0F0F0F,
        card = 0xFF1E1E1E
    ),
    // --- New in v0.6 ---
    ThemeId.VIOLET to AppTheme(
        id = ThemeId.VIOLET,
        primary = 0xFF8B5CF6,    // Violet 500
        background = 0xFF120A1F,
        card = 0xFF201233
    ),
    ThemeId.TEAL to AppTheme(
        id = ThemeId.TEAL,
        primary = 0xFF14B8A6,    // Teal 500
        background = 0xFF071714,
        card = 0xFF0E2622
    ),
    ThemeId.ROSE to AppTheme(
        id = ThemeId.ROSE,
        primary = 0xFFF43F5E,    // Rose 500
        background = 0xFF1A0810,
        card = 0xFF2B0F1B
    ),
    ThemeId.STEEL to AppTheme(
        id = ThemeId.STEEL,
        primary = 0xFF60A5FA,    // Blue 400
        background = 0xFF0B1220,
        card = 0xFF162136
    )
)

fun Color.Companion.fromLong(value: Long) = Color(value)

/**
 * Resolves the [AppTheme] to actually render for [themeId]. Every built-in theme lives in the
 * static [THEMES] map; [ThemeId.CUSTOM] instead uses the user's own saved colors ([customTheme]),
 * falling back to Monochrome if the user selected Custom but hasn't actually saved one yet.
 */
fun resolveTheme(themeId: ThemeId, customTheme: AppTheme?): AppTheme {
    if (themeId == ThemeId.CUSTOM) {
        return customTheme ?: THEMES.getValue(ThemeId.MONOCHROME)
    }
    return THEMES[themeId] ?: THEMES.getValue(ThemeId.MONOCHROME)
}

fun AppTheme.primaryColor() = Color(primary)
fun AppTheme.backgroundColor() = Color(background)
fun AppTheme.cardColor() = Color(card)
fun AppTheme.textColor() = Color(0xFFF1F5F9)      // Slate 100
fun AppTheme.mutedColor() = Color(0xFF94A3B8)      // Slate 400
fun AppTheme.surfaceColor() = Color(background).copy(alpha = 0.5f)
