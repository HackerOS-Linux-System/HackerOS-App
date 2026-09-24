package com.hackeros.app.data.articles

/**
 * Native model of the website's Articles section (`articles/index.js` + `articles/<id>.js` in
 * the HackerOS-Website repo). The website renders these files in the browser; the app parses the
 * very same files (see [ArticleParser]) and renders them with native Compose UI - no WebView.
 */
data class ArticlesData(
    /** tag id -> (language code -> label) */
    val tags: Map<String, Map<String, String>>,
    val articles: List<Article>
) {
    fun tagLabel(tagId: String, lang: String): String =
        tags[tagId]?.localized(lang)?.takeIf { it.isNotBlank() } ?: tagId
}

data class Article(
    val id: String,
    /** ISO date (yyyy-MM-dd), used for sorting newest first. */
    val date: String,
    val author: String,
    val readingMinutes: Int,
    val icon: String,
    val tags: List<String>,
    val title: Map<String, String>,
    val summary: Map<String, String>,
    /** language code -> ordered content blocks. Missing languages fall back to English, like the website. */
    val content: Map<String, List<ArticleBlock>>
) {
    fun titleFor(lang: String) = title.localized(lang)
    fun summaryFor(lang: String) = summary.localized(lang)

    /** The blocks to show for [lang]: exact language, else English, else Polish, else empty. */
    fun blocksFor(lang: String): List<ArticleBlock> =
        content[lang] ?: content["en"] ?: content["pl"] ?: emptyList()

    /** True when [lang] has no translation of its own and the fallback is being shown. */
    fun isFallback(lang: String) = content[lang] == null

    /** Lower-cased text used by the search box: title, summary and all block text. */
    fun searchText(lang: String): String = buildString {
        append(titleFor(lang)).append(' ').append(summaryFor(lang)).append(' ')
        blocksFor(lang).forEach { append(it.plainText()).append(' ') }
    }.lowercase()
}

sealed class ArticleBlock {
    data class Heading(val level: Int, val text: String) : ArticleBlock()
    /** [html] is limited to `<strong> <em> <code> <a href> <br>` - rendered by InlineHtmlText. */
    data class Paragraph(val html: String) : ArticleBlock()
    data class Quote(val html: String) : ArticleBlock()
    data class Bullets(val items: List<String>, val ordered: Boolean) : ArticleBlock()
    data class Code(val lang: String, val title: String?, val code: String) : ArticleBlock()
    /** [kind] is one of `info`, `tip`, `warn`. */
    data class Note(val kind: String, val html: String) : ArticleBlock()
    data class Table(val head: List<String>, val rows: List<List<String>>) : ArticleBlock()

    fun plainText(): String = when (this) {
        is Heading -> text
        is Paragraph -> stripTags(html)
        is Quote -> stripTags(html)
        is Bullets -> items.joinToString(" ") { stripTags(it) }
        is Code -> ""
        is Note -> stripTags(html)
        is Table -> (head + rows.flatten()).joinToString(" ") { stripTags(it) }
    }
}

internal fun stripTags(s: String): String = s.replace(Regex("<[^>]+>"), " ")

fun Map<String, String>.localized(lang: String): String =
    this[lang] ?: this["en"] ?: this["pl"] ?: values.firstOrNull() ?: ""
