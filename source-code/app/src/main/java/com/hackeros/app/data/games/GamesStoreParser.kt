package com.hackeros.app.data.games

import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses `games-store/community-games.json` into a list of [CommunityGame]. Every field beyond
 * id/name/downloadUrl is read defensively (missing/malformed values fall back to sane defaults)
 * so the catalog schema can evolve without breaking older installed app versions, and a single
 * malformed entry doesn't take down the whole list.
 */
object GamesStoreParser {

    fun parse(json: String): List<CommunityGame> {
        return try {
            val root = JSONObject(json)
            val arr: JSONArray = root.optJSONArray("games") ?: return emptyList()
            (0 until arr.length()).mapNotNull { i ->
                try {
                    parseGame(arr.getJSONObject(i))
                } catch (_: Exception) {
                    null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseGame(o: JSONObject): CommunityGame? {
        val id = o.optString("id").ifBlank { return null }
        val name = o.optString("name").ifBlank { return null }
        val installType = o.optString("installType", "apk").ifBlank { "apk" }
        val isSourceBuild = installType.equals("github_source", ignoreCase = true)

        // A prebuilt package URL is required for "apk"-type entries (as before); source-build
        // entries instead require a GitHub repo, so downloadUrl may legitimately be absent there.
        val downloadUrl = o.optString("downloadUrl")
        if (!isSourceBuild && downloadUrl.isBlank()) return null

        val screenshots = o.optJSONArray("screenshots")?.let { arr ->
            (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
        } ?: emptyList()

        val buildCommands = o.optJSONArray("buildCommands")?.let { arr ->
            (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
        } ?: emptyList()

        val repoOwner = o.optString("repoOwner").ifBlank { null }
        val repoName = o.optString("repoName").ifBlank { null }
        if (isSourceBuild && (repoOwner == null || repoName == null)) return null

        return CommunityGame(
            id = id,
            name = name,
            author = o.optString("author", "Community"),
            shortDescription = o.optString("shortDescription"),
            description = o.optString("description", o.optString("shortDescription")),
            version = o.optString("version", "-"),
            category = o.optString("category", "Game"),
            iconUrl = o.optString("iconUrl"),
            screenshots = screenshots,
            installType = installType,
            downloadUrl = downloadUrl,
            downloadSize = o.optLong("downloadSize", 0L),
            checksumUrl = o.optString("checksumUrl").ifBlank { null },
            packageName = o.optString("packageName").ifBlank { null },
            rating = o.optDouble("rating", 0.0).let { if (it.isNaN()) 0.0 else it },
            sourceUrl = o.optString("sourceUrl").ifBlank { null },
            license = o.optString("license", "-"),
            updatedAt = o.optString("updatedAt"),
            repoOwner = repoOwner,
            repoName = repoName,
            repoBranch = o.optString("repoBranch", "main").ifBlank { "main" },
            buildCommands = buildCommands
        )
    }
}
