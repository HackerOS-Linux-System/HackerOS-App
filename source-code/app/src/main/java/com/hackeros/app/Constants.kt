package com.hackeros.app

object Constants {
    const val APP_VERSION = "0.4"
    const val RELEASE_INFO_URL = "https://raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-App/main/release-info.hacker"
    const val VERSION_CHECK_URL = "https://raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-App/main/version.hacker"
    const val GALLERY_API_URL = "https://api.github.com/repos/HackerOS-Linux-System/HackerOS-App/contents/gallery"

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
