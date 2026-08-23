package com.hackeros.app

object Constants {
    const val APP_VERSION = "0.6"

    // --- Releases -----------------------------------------------------------------------
    //
    // IMPORTANT: https://hackeros-linux-system.github.io/HackerOS-Website/releases.html renders
    // its release list from `translations/releases.js`, but that file ONLY contains empty
    // `releases: []` placeholders + UI label strings - the actual release data is merged in at
    // runtime (in the browser) from the per-language files under `translations/files/all/`
    // (e.g. `translations/files/all/pl.js` sets `window.HACKEROS_RELEASES_ALL.pl = [ ... ]`).
    //
    // Earlier versions of this app fetched `translations/releases.js` directly and tried to
    // parse a `releases:` array out of it - which is always empty in the raw file, so the app
    // never actually showed any releases even with a working connection. Fetching the
    // per-language data file directly is the real, always-populated source of truth and is
    // exactly what the website itself merges in via JS, so the app now mirrors the website 1:1.
    const val WEBSITE_RELEASES_PAGE_URL = "https://hackeros-linux-system.github.io/HackerOS-Website/releases.html"
    const val WEBSITE_RELEASES_LANG_URL_TEMPLATE =
        "https://raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-Website/main/translations/files/all/%s.js"

    fun releasesUrlFor(langCode: String) =
        WEBSITE_RELEASES_LANG_URL_TEMPLATE.format(langCode)

    // Legacy/offline fallback source, used only if the website data cannot be reached at all.
    const val LEGACY_RELEASE_INFO_URL = "https://raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-App/main/release-info.hacker"

    const val VERSION_CHECK_URL = "https://raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-App/main/version.hacker"
    // --- Gallery ------------------------------------------------------------------------
    //
    // As of v0.5, gallery images are sourced from the official HackerOS Website repo's own
    // `gallery` folder (the same folder shown at
    // https://github.com/HackerOS-Linux-System/HackerOS-Website/tree/main/gallery), rather than
    // the App repo's separate copy - so the app's Gallery tab always mirrors the website 1:1.
    const val GALLERY_API_URL = "https://api.github.com/repos/HackerOS-Linux-System/HackerOS-Website/contents/gallery"

    // --- Documentation --------------------------------------------------------------------
    //
    // As of v0.5.2, documentation is parsed natively - no WebView. The website assembles its
    // documentation page client-side from two files: `translations/hackeros-documentation.js`
    // (per-language page metadata + structured content fields, PL/EN/DE fully translated, other
    // languages fall back to EN exactly like the website does) and `translations/doc-engine.js`
    // (the renderer, whose per-tab field layout this app's DocContentParser mirrors exactly).
    // See data/docs/JsLenientJson.kt + data/docs/DocContentParser.kt.
    const val DOCUMENTATION_JS_URL =
        "https://raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-Website/main/translations/hackeros-documentation.js"

    // Kept only as an optional "open on website" escape hatch from Settings/error states.
    const val DOCUMENTATION_WEB_URL = "https://hackeros-linux-system.github.io/HackerOS-Website/hackeros-documentation.html"

    // --- App updates ----------------------------------------------------------------------
    //
    // APK download + checksum verification, mirroring the same "install on device with a live
    // progress bar" UX already used for wallpapers (see WallpapersScreen/ApkUpdater).
    fun apkUrlFor(version: String) =
        "https://github.com/HackerOS-Linux-System/HackerOS-App/releases/download/v$version/HackerOS-App-$version.apk"

    // Convention: a checksum file published alongside the APK on the GitHub Release, containing
    // just the hex SHA-256 digest. If a given release doesn't publish one, update verification
    // is skipped gracefully (see ApkUpdater) rather than blocking the update entirely.
    fun apkChecksumUrlFor(version: String) = "${apkUrlFor(version)}.sha256"

    // --- Games Store ------------------------------------------------------------------------
    //
    // Community game listings for the "Games Store" section - HackerOS-App acts as a manager
    // for community-submitted phone games, reading the shared catalog file maintained in this
    // same repository. See data/games/GamesStoreParser.kt for the expected JSON schema.
    const val GAMES_STORE_JSON_URL =
        "https://raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-App/main/games-store/community-games.json"

    val WALLPAPERS = listOf(
        WallpaperItem(1, "Default HackerOS",
            "https://raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-Website/main/phone-wallpapers/default-wallpaper.png"),
        WallpaperItem(2, "Cyber Grid",
            "https://raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-Website/main/phone-wallpapers/wallpaper.png"),
        WallpaperItem(3, "Neon Nights",
            "https://raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-Website/main/phone-wallpapers/wallpaper1.png"),
        WallpaperItem(4, "Abstract Flow",
            "https://raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-Website/main/phone-wallpapers/wallpaper2.png"),
        WallpaperItem(5, "Deep Space",
            "https://raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-Website/main/phone-wallpapers/wallpaper3.png"),
        WallpaperItem(6, "Code Rain",
            "https://raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-Website/main/phone-wallpapers/wallpaper4.png"),
        WallpaperItem(7, "Circuitry",
            "https://raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-Website/main/phone-wallpapers/wallpaper5.png"),
    )
}

data class WallpaperItem(
    val id: Int,
    val name: String,
    val url: String
)
