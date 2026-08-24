package com.hackeros.app.data.model

data class ReleaseInfo(
    val version: String,
    val description: String,
    val editions: String,
    val news: String
)

/**
 * Parses the human-readable "editions" block (e.g. "HackerOS Official: 7.08.2026\nHackerOS
 * Cybersecurity: 7.08.2026\nHackerOS NVIDIA: 7.08.2026") into just the edition names
 * ("HackerOS Official", "HackerOS Cybersecurity", "HackerOS NVIDIA"), used to let the user pick
 * which specific editions they want new-release notifications for.
 */
fun ReleaseInfo.editionNames(): List<String> =
    editions.split("\n")
        .map { it.substringBefore(":").trim() }
        .filter { it.isNotBlank() }
        .distinct()

data class GalleryImage(
    val name: String,
    val sha: String,
    val size: Long,
    val url: String,
    val html_url: String,
    val git_url: String,
    val download_url: String,
    val type: String
)

enum class AppScreen {
    RELEASES, WALLPAPERS, GALLERY, DOCS, GAMES_STORE, TEAM, SETTINGS
}

enum class ThemeId(val themeName: String) {
    // New default theme as of v0.5: a clean, neutral gray/white/black look. Listed first so it
    // also shows first in the theme picker grid on the Settings screen.
    MONOCHROME("Monochrome"),
    HACKER("HackerOS Original"),
    CYBERPUNK("Night City"),
    OCEAN("Deep Sea"),
    SUNSET("Solar Flare"),
    MATRIX("The Construct"),
    CRIMSON("Red Alert"),
    ROYAL("Luxury Gold"),
    // New in v0.6.
    VIOLET("Ultraviolet"),
    TEAL("Cool Mint"),
    ROSE("Blackout Rose"),
    STEEL("Steel Blue"),
    // A user-defined theme built from their own chosen colors (see CustomThemeDialog). Its
    // actual colors live in PreferencesRepository, not in the static THEMES map, since they're
    // per-user data rather than a fixed built-in palette.
    CUSTOM("Custom")
}

enum class Language(val code: String, val displayName: String, val flag: String) {
    PL("pl", "Polski", "🇵🇱"),
    EN("en", "English", "🇺🇸"),
    DE("de", "Deutsch", "🇩🇪"),
    ES("es", "Español", "🇪🇸"),
    FR("fr", "Français", "🇫🇷"),
    IT("it", "Italiano", "🇮🇹"),
    RU("ru", "Русский", "🇷🇺"),
    UK("uk", "Українська", "🇺🇦"),
    ZH("zh", "中文", "🇨🇳"),
    JA("ja", "日本語", "🇯🇵")
}

data class AppTheme(
    val id: ThemeId,
    val primary: Long,       // ARGB color
    val background: Long,
    val card: Long
)
