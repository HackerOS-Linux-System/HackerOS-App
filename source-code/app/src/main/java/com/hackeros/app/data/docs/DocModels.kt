package com.hackeros.app.data.docs

/**
 * A single renderable unit of documentation content, mirroring the DOM nodes the website's
 * `doc-engine.js` builds for each tab (`el('h2', ...)`, `ulEl(...)`, `mkPre(...)`, etc.), so the
 * native renderer produces the same structure without needing a WebView.
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
    /** A larger, syntax-free code sample (e.g. the HackerScript example). */
    data class CodeSample(val text: String) : DocBlock()
    data class LinkLine(val labelHtml: String, val url: String) : DocBlock()
    data class ToolsTable(val rows: List<Triple<String, String, String>>) : DocBlock()
    object Divider : DocBlock()
}

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
