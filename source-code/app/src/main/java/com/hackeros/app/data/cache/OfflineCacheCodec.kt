package com.hackeros.app.data.cache

import com.hackeros.app.data.model.GalleryImage
import com.hackeros.app.data.model.ReleaseInfo
import org.json.JSONArray
import org.json.JSONObject

/**
 * Minimal JSON (de)serialization for the two lists we cache locally for offline use
 * ([ReleaseInfo] and [GalleryImage]), built on `org.json` (already used elsewhere in the app)
 * rather than pulling in a full serialization library just for two small data classes.
 */
object OfflineCacheCodec {

    fun releasesToJson(releases: List<ReleaseInfo>): String {
        val arr = JSONArray()
        releases.forEach { r ->
            arr.put(
                JSONObject().apply {
                    put("version", r.version)
                    put("description", r.description)
                    put("editions", r.editions)
                    put("news", r.news)
                }
            )
        }
        return arr.toString()
    }

    fun releasesFromJson(json: String?): List<ReleaseInfo> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                ReleaseInfo(
                    version = o.optString("version"),
                    description = o.optString("description"),
                    editions = o.optString("editions"),
                    news = o.optString("news")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun galleryToJson(images: List<GalleryImage>): String {
        val arr = JSONArray()
        images.forEach { img ->
            arr.put(
                JSONObject().apply {
                    put("name", img.name)
                    put("sha", img.sha)
                    put("size", img.size)
                    put("url", img.url)
                    put("html_url", img.html_url)
                    put("git_url", img.git_url)
                    put("download_url", img.download_url)
                    put("type", img.type)
                }
            )
        }
        return arr.toString()
    }

    fun galleryFromJson(json: String?): List<GalleryImage> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                GalleryImage(
                    name = o.optString("name"),
                    sha = o.optString("sha"),
                    size = o.optLong("size"),
                    url = o.optString("url"),
                    html_url = o.optString("html_url"),
                    git_url = o.optString("git_url"),
                    download_url = o.optString("download_url"),
                    type = o.optString("type")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun wallpapersToJson(wallpapers: List<com.hackeros.app.WallpaperItem>): String {
        val arr = JSONArray()
        wallpapers.forEach { wp ->
            arr.put(
                JSONObject().apply {
                    put("id", wp.id)
                    put("name", wp.name)
                    put("url", wp.url)
                }
            )
        }
        return arr.toString()
    }

    fun wallpapersFromJson(json: String?): List<com.hackeros.app.WallpaperItem> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                com.hackeros.app.WallpaperItem(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    url = o.optString("url")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
