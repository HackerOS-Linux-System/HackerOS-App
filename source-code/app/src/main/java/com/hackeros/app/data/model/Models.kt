package com.hackeros.app.data.model

data class ReleaseInfo(
    val version: String,
    val description: String,
    val editions: String,
    val news: String
)

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
    RELEASES, WALLPAPERS, GALLERY, TEAM, SETTINGS
}

enum class ThemeId(val themeName: String) {
    HACKER("HackerOS Original"),
    CYBERPUNK("Night City"),
    OCEAN("Deep Sea"),
    SUNSET("Solar Flare"),
    MATRIX("The Construct"),
    CRIMSON("Red Alert"),
    ROYAL("Luxury Gold")
}

enum class Language(val code: String, val displayName: String, val flag: String) {
    PL("pl", "Polski", "🇵🇱"),
    EN("en", "English", "🇺🇸"),
    DE("de", "Deutsch", "🇩🇪"),
    ES("es", "Español", "🇪🇸"),
    FR("fr", "Français", "🇫🇷")
}

data class AppTheme(
    val id: ThemeId,
    val primary: Long,       // ARGB color
    val background: Long,
    val card: Long
)
