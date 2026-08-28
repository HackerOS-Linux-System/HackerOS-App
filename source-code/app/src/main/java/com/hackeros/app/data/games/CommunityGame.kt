package com.hackeros.app.data.games

/**
 * A single community game listing from `games-store/community-games.json`.
 *
 * Expected JSON schema (all fields except id/name are optional and default gracefully, so the
 * catalog can grow richer over time without breaking older app versions):
 *
 * ```json
 * {
 *   "games": [
 *     {
 *       "id": "bark-squadron",
 *       "name": "Bark Squadron",
 *       "author": "HackerOS Team",
 *       "shortDescription": "One-line hook shown in the grid",
 *       "description": "Longer description shown in the detail view",
 *       "version": "1.2.0",
 *       "category": "Arcade",
 *       "iconUrl": "https://.../icon.png",
 *       "screenshots": ["https://.../1.png", "https://.../2.png"],
 *       "installType": "apk",
 *       "downloadUrl": "https://github.com/.../releases/download/v1.2.0/bark-squadron.apk",
 *       "downloadSize": 24500000,
 *       "checksumUrl": "https://.../bark-squadron.apk.sha256",
 *       "packageName": "com.hackeros.games.barksquadron",
 *       "rating": 4.7,
 *       "sourceUrl": "https://github.com/HackerOS-Linux-System/HackerOS-Games",
 *       "license": "GPL-3.0",
 *       "updatedAt": "2026-07-20"
 *     }
 *   ]
 * }
 * ```
 *
 * `installType` (v0.7) selects how the game is obtained; unrecognized/missing values fall back
 * to `"apk"` so older catalog entries keep working unchanged:
 *
 * - `"apk"` (default) - a prebuilt APK is downloaded from [downloadUrl] and installed exactly as
 *   before, optionally verified against [checksumUrl].
 * - `"github_source"` - the game is distributed as source only, from a GitHub repository
 *   ([repoOwner]/[repoName], branch [repoBranch]). The app has no on-device Android toolchain to
 *   compile arbitrary source code, so instead of pretending to "build" it silently, the Games
 *   Store shows the real [buildCommands] (clone + build, copyable) and a link to open the
 *   repository, so the person can build/run it themselves on a machine that has the right
 *   toolchain (or, for HackerOS-native tools, directly on a HackerOS install).
 */
data class CommunityGame(
    val id: String,
    val name: String,
    val author: String,
    val shortDescription: String,
    val description: String,
    val version: String,
    val category: String,
    val iconUrl: String,
    val screenshots: List<String>,
    val installType: String = "apk",
    val downloadUrl: String,
    val downloadSize: Long,
    val checksumUrl: String?,
    val packageName: String?,
    val rating: Double,
    val sourceUrl: String?,
    val license: String,
    val updatedAt: String,
    val repoOwner: String? = null,
    val repoName: String? = null,
    val repoBranch: String = "main",
    val buildCommands: List<String> = emptyList()
) {
    val isSourceBuild: Boolean get() = installType.equals("github_source", ignoreCase = true)
}
