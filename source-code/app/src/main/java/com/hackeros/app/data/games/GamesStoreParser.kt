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
        val downloadUrl = o.optString("downloadUrl").ifBlank { return null }

        val screenshots = o.optJSONArray("screenshots")?.let { arr ->
            (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
        } ?: emptyList()

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
            downloadUrl = downloadUrl,
            downloadSize = o.optLong("downloadSize", 0L),
            checksumUrl = o.optString("checksumUrl").ifBlank { null },
            packageName = o.optString("packageName").ifBlank { null },
            rating = o.optDouble("rating", 0.0).let { if (it.isNaN()) 0.0 else it },
            sourceUrl = o.optString("sourceUrl").ifBlank { null },
            license = o.optString("license", "-"),
            updatedAt = o.optString("updatedAt")
        )
    }
}
