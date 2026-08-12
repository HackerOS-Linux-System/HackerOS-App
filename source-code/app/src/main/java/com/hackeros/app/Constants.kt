package com.hackeros.app

object Constants {
    const val APP_VERSION = "0.5"

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
    // The official HackerOS documentation is a large, richly-formatted, multi-tab HTML page
    // (headings, code blocks, tables, nested lists, inline links) that is assembled client-side
    // by the website's own `translations/hackeros-documentation.js` + `translations/doc-engine.js`.
    // Rather than re-implementing that renderer (whose per-tab schema is intentionally
    // freeform/heterogeneous and would be fragile to keep in sync by hand), the app shows the
    // exact same live page natively in-app (an embedded WebView, not an external browser tab),
    // so the docs content, search, and language switching always match the website exactly.
    const val DOCUMENTATION_URL = "https://hackeros-linux-system.github.io/HackerOS-Website/hackeros-documentation.html"

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
