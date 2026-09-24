package com.hackeros.app.data.articles

import com.hackeros.app.data.docs.JsLenientJson
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses the website's article data files. They are `.js` files that assign a JSON object to a
 * `window.HACKEROS_ARTICLES...` global (so the website can load them with a plain `<script>`),
 * so we cut the object literal out of the assignment and hand it to [JsLenientJson] - the same
 * tolerant JS-literal-to-JSON converter the Documentation screen uses.
 *
 * Because parsing tolerates malformed entries individually, one bad article never takes down
 * the whole list, and a schema that grows new optional fields keeps working on older app versions.
 */
object ArticleParser {

    /** The list of article ids/files published by `articles/index.js` plus the tag label table. */
    data class Index(val tags: Map<String, Map<String, String>>, val files: List<Pair<String, String>>)

    /** Extracts the `{ ... }` literal that follows the assignment marker, as strict JSON. */
    private fun extractObject(js: String, marker: Regex): JSONObject? {
        val m = marker.find(js) ?: return null
        val start = js.indexOf('{', m.range.last)
        val end = js.lastIndexOf('}')
        if (start == -1 || end <= start) return null
        return try {
            JSONObject(JsLenientJson.convert(js.substring(start, end + 1)))
        } catch (_: Exception) {
            null
        }
    }

    private val INDEX_MARKER = Regex("""HACKEROS_ARTICLES_INDEX\s*=""")
    private val ARTICLE_MARKER = Regex("""HACKEROS_ARTICLES\[[^\]]*]\s*=""")

    fun parseIndex(js: String): Index? {
        val root = extractObject(js, INDEX_MARKER) ?: return null
        val tags = parseTags(root.optJSONObject("tags"))
        val arr = root.optJSONArray("articles") ?: return null
        val files = (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val id = o.optString("id").ifBlank { return@mapNotNull null }
            id to o.optString("file").ifBlank { "$id.js" }
        }
        return Index(tags, files)
    }

    /** Parses one `articles/<id>.js` file into its strict-JSON object (used for offline caching). */
    fun articleJsToJson(js: String): JSONObject? = extractObject(js, ARTICLE_MARKER)

    // ---- bundle = what we cache offline: {"tags":{...},"articles":[{...},{...}]} ----

    fun bundle(index: Index, articles: List<JSONObject>): String {
        val tags = JSONObject()
        index.tags.forEach { (id, labels) -> tags.put(id, JSONObject(labels)) }
        return JSONObject().put("tags", tags).put("articles", JSONArray(articles)).toString()
    }

    fun parseBundle(json: String?): ArticlesData? {
        if (json.isNullOrBlank()) return null
        return try {
            val root = JSONObject(json)
            val tags = parseTags(root.optJSONObject("tags"))
            val arr = root.optJSONArray("articles") ?: return null
            val articles = (0 until arr.length()).mapNotNull { i ->
                try { parseArticle(arr.getJSONObject(i)) } catch (_: Exception) { null }
            }.sortedByDescending { it.date }
            ArticlesData(tags, articles)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseTags(o: JSONObject?): Map<String, Map<String, String>> {
        if (o == null) return emptyMap()
        return o.keys().asSequence().associateWith { k -> parseStringMap(o.optJSONObject(k)) }
    }

    private fun parseStringMap(o: JSONObject?): Map<String, String> {
        if (o == null) return emptyMap()
        return o.keys().asSequence().associateWith { k -> o.optString(k) }
    }

    private fun parseArticle(o: JSONObject): Article? {
        val id = o.optString("id").ifBlank { return null }
        val contentObj = o.optJSONObject("content") ?: return null
        val content = contentObj.keys().asSequence().associateWith { lang ->
            val arr = contentObj.optJSONArray(lang) ?: JSONArray()
            (0 until arr.length()).mapNotNull { i -> parseBlock(arr.optJSONObject(i)) }
        }
        val tagsArr = o.optJSONArray("tags") ?: JSONArray()
        return Article(
            id = id,
            date = o.optString("date", "1970-01-01"),
            author = o.optString("author"),
            readingMinutes = o.optInt("readingMinutes", 1),
            icon = o.optString("icon", "📄"),
            tags = (0 until tagsArr.length()).map { tagsArr.optString(it) },
            title = parseStringMap(o.optJSONObject("title")),
            summary = parseStringMap(o.optJSONObject("summary")),
            content = content
        )
    }

    private fun stringList(a: JSONArray?): List<String> =
        if (a == null) emptyList() else (0 until a.length()).map { a.optString(it) }

    private fun parseBlock(b: JSONObject?): ArticleBlock? {
        if (b == null) return null
        return when (b.optString("t")) {
            "h2" -> ArticleBlock.Heading(2, b.optString("text"))
            "h3" -> ArticleBlock.Heading(3, b.optString("text"))
            "p" -> ArticleBlock.Paragraph(b.optString("html"))
            "quote" -> ArticleBlock.Quote(b.optString("html"))
            "ul" -> ArticleBlock.Bullets(stringList(b.optJSONArray("items")), ordered = false)
            "ol" -> ArticleBlock.Bullets(stringList(b.optJSONArray("items")), ordered = true)
            "code" -> {
                // `code` is either one string or (as authored on the website) an array of lines.
                val raw = b.opt("code")
                val text = if (raw is JSONArray) stringList(raw).joinToString("\n") else b.optString("code")
                ArticleBlock.Code(b.optString("lang", "text"), b.optString("title").ifBlank { null }, text)
            }
            "note" -> ArticleBlock.Note(b.optString("kind", "info"), b.optString("html"))
            "table" -> {
                val rows = b.optJSONArray("rows")
                ArticleBlock.Table(
                    head = stringList(b.optJSONArray("head")),
                    rows = if (rows == null) emptyList() else (0 until rows.length()).map { stringList(rows.optJSONArray(it)) }
                )
            }
            else -> null // unknown block types from a newer website are skipped, not fatal
        }
    }
}
