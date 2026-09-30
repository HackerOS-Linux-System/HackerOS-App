package com.hackeros.app.data.docs

/**
 * A single renderable unit of documentation content, mirroring the DOM nodes the website's
 * documentation pages are made of, so the native renderer produces the same structure without
 * needing a WebView.
 */
sealed class DocBlock {
    data class Heading2(val text: String) : DocBlock()
    data class Heading3(val text: String) : DocBlock()
    data class Heading4(val text: String) : DocBlock()
    /** [html] may contain a small subset of inline tags: <strong>, <code>, <a href>, <br>, <em>. */
    data class Paragraph(val html: String) : DocBlock()
    data class BulletList(val itemsHtml: List<String>) : DocBlock()
    data class NumberedList(val itemsHtml: List<String>) : DocBlock()
    /** A shell command block with a copy button, matching the site's `mkPre()`. */
    data class Command(val text: String) : DocBlock()
    /** A larger, syntax-free code sample (multi-line code, config files, terminal output...). */
    data class CodeSample(val text: String) : DocBlock()
    /**
     * [nativeDetailKey], when non-null, means this link opens a native in-app detail page
     * (see [DocRemote]) instead of handing off to the external browser via [url] - so tapping it
     * never "teleports" out of the app or opens any WebView. [url] is only used to leave the app
     * when [nativeDetailKey] is null.
     */
    data class LinkLine(val labelHtml: String, val url: String, val nativeDetailKey: String? = null) : DocBlock()
    data class ToolsTable(val rows: List<ToolTableRow>) : DocBlock()
    /** Generic table from the website's docs. Rendered as one compact card per row (phone-friendly). */
    data class Table(val headers: List<String>, val rows: List<List<String>>) : DocBlock()
    /** Info / warning / tip box. [kind] is "info", "warn" or "tip". [html] uses the inline subset. */
    data class Callout(val kind: String, val html: String) : DocBlock()
    /** A tappable card that opens another native documentation page. */
    data class DetailCard(val title: String, val description: String, val detailKey: String) : DocBlock()
    object Divider : DocBlock()
}

/** One row of the "Tools and applications" table; [detailKey] is set when a native page exists. */
data class ToolTableRow(
    val tool: String,
    val description: String,
    val installation: String,
    val detailKey: String? = null
)

data class DocTab(
    val key: String,
    val label: String,
    val blocks: List<DocBlock>
)

data class DocPage(
    val tabs: List<DocTab>,
    /** True when this language has no real content and English content is shown instead. */
    val isEnglishFallback: Boolean
)

/** A native "detail" documentation page fetched live from the official website (see [DocRemote]). */
data class DetailPage(
    val key: String,
    val title: String,
    /** Language the source content is written in ("pl" or "en"). */
    val lang: String,
    val tabs: List<DocTab>
)
