package com.hackeros.app.data.docs

/**
 * A tiny, dependency-free HTML parser - just enough DOM for [DocHtmlConverter] to read the
 * HackerOS website's hand-written documentation pages on the device (no WebView, no jsoup).
 *
 * Supports: nested elements, attributes (quoted / unquoted / boolean), void elements, comments,
 * doctype, raw-text elements (`script`, `style`), HTML entities, and the implicit end tags that
 * hand-written HTML relies on (`<li>`, `<p>`, `<td>`, `<tr>`, `<dt>`/`<dd>` ...).
 */
internal sealed class HNode {
    var parent: HElement? = null
}

internal class HText(val text: String) : HNode()

internal class HElement(val name: String, val attrs: Map<String, String>) : HNode() {
    val children = mutableListOf<HNode>()

    fun attr(n: String): String? = attrs[n]

    /** The `class` attribute exactly as written (space separated), or "". */
    val cls: String get() = attrs["class"]?.trim()?.replace(Regex("\\s+"), " ") ?: ""

    val classTokens: List<String> get() = cls.split(' ').filter { it.isNotEmpty() }

    val childElements: List<HElement> get() = children.filterIsInstance<HElement>()

    /** All descendant elements in document order. */
    fun descendants(): Sequence<HElement> = sequence {
        for (c in children) {
            if (c is HElement) {
                yield(c)
                yieldAll(c.descendants())
            }
        }
    }

    fun findAll(vararg names: String): List<HElement> {
        val set = names.toSet()
        return descendants().filter { it.name in set }.toList()
    }

    fun findAll(names: Set<String>): List<HElement> = descendants().filter { it.name in names }.toList()

    fun find(vararg names: String): HElement? {
        val set = names.toSet()
        return descendants().firstOrNull { it.name in set }
    }

    fun find(predicate: (HElement) -> Boolean): HElement? = descendants().firstOrNull(predicate)

    fun hasDescendant(names: Set<String>): Boolean = descendants().any { it.name in names }

    fun ancestorSequence(): Sequence<HElement> = generateSequence(parent) { it.parent }

    fun findParent(vararg names: String): HElement? {
        val set = names.toSet()
        return ancestorSequence().firstOrNull { it.name in set }
    }

    /** Text of the element, like BeautifulSoup's `get_text(" ", strip=True)` collapsed to single spaces. */
    fun textOf(): String {
        val parts = ArrayList<String>()
        collectText(this, parts)
        return parts.joinToString(" ").replace(WS, " ").trim()
    }

    private fun collectText(el: HElement, out: MutableList<String>) {
        for (c in el.children) {
            when (c) {
                is HText -> {
                    val t = c.text.replace(WS, " ").trim()
                    if (t.isNotEmpty()) out.add(t)
                }
                is HElement -> if (c.name != "script" && c.name != "style") collectText(c, out)
            }
        }
    }

    companion object {
        val WS = Regex("[\\s\\u00A0]+")
    }
}

internal object HtmlDom {

    private val VOID = setOf(
        "area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param",
        "source", "track", "wbr"
    )
    private val RAW_TEXT = setOf("script", "style")

    // Elements that implicitly close an open <p>.
    private val CLOSES_P = setOf(
        "address", "article", "aside", "blockquote", "details", "div", "dl", "fieldset", "figure",
        "footer", "form", "h1", "h2", "h3", "h4", "h5", "h6", "header", "hr", "main", "nav", "ol",
        "p", "pre", "section", "table", "ul"
    )

    private val ENTITIES = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to "\u00A0",
        "copy" to "©", "reg" to "®", "trade" to "™", "hellip" to "…", "mdash" to "—", "ndash" to "–",
        "rarr" to "→", "larr" to "←", "uarr" to "↑", "darr" to "↓", "harr" to "↔", "rArr" to "⇒",
        "lArr" to "⇐", "times" to "×", "middot" to "·", "bull" to "•", "laquo" to "«", "raquo" to "»",
        "check" to "✓", "deg" to "°", "plusmn" to "±", "ne" to "≠", "le" to "≤", "ge" to "≥",
        "lsquo" to "‘", "rsquo" to "’", "ldquo" to "“", "rdquo" to "”", "euro" to "€", "sect" to "§",
        "para" to "¶", "infin" to "∞", "micro" to "µ", "thinsp" to "\u2009", "ensp" to "\u2002",
        "emsp" to "\u2003", "shy" to "", "zwj" to "", "zwnj" to "", "hearts" to "♥", "star" to "★"
    )

    private val ENTITY_RE = Regex("&(#[xX][0-9a-fA-F]+|#[0-9]+|[A-Za-z][A-Za-z0-9]*);?")

    fun decode(s: String): String {
        if (!s.contains('&')) return s
        return ENTITY_RE.replace(s) { m ->
            val body = m.groupValues[1]
            val hasSemi = m.value.endsWith(";")
            try {
                when {
                    body.startsWith("#x") || body.startsWith("#X") ->
                        String(Character.toChars(body.substring(2).toInt(16)))
                    body.startsWith("#") -> String(Character.toChars(body.substring(1).toInt()))
                    hasSemi || body in setOf("amp", "lt", "gt", "quot", "nbsp", "copy") ->
                        ENTITIES[body] ?: m.value
                    else -> m.value
                }
            } catch (e: Exception) {
                m.value
            }
        }
    }

    /** Parses [html] into a synthetic root element named `#root`. */
    fun parse(html: String): HElement {
        val root = HElement("#root", emptyMap())
        val stack = ArrayList<HElement>()
        stack.add(root)
        val n = html.length
        var i = 0

        fun current() = stack.last()

        fun addChild(node: HNode) {
            node.parent = current()
            current().children.add(node)
        }

        fun closeUpTo(name: String): Boolean {
            val idx = stack.indexOfLast { it.name == name }
            if (idx <= 0) return false
            while (stack.size > idx) stack.removeAt(stack.size - 1)
            return true
        }

        // Closes the nearest open element named in [names] (searching upwards but never crossing
        // one of [stopAt]).
        fun implicitClose(names: Set<String>, stopAt: Set<String>) {
            var k = stack.size - 1
            while (k > 0) {
                val nm = stack[k].name
                if (nm in names) {
                    while (stack.size > k) stack.removeAt(stack.size - 1)
                    return
                }
                if (nm in stopAt) return
                k--
            }
        }

        fun textRun(from: Int, to: Int) {
            if (to > from) addChild(HText(decode(html.substring(from, to))))
        }

        while (i < n) {
            val lt = html.indexOf('<', i)
            if (lt == -1) {
                textRun(i, n)
                break
            }
            textRun(i, lt)
            i = lt
            // comment
            if (html.startsWith("<!--", i)) {
                val end = html.indexOf("-->", i + 4)
                i = if (end == -1) n else end + 3
                continue
            }
            // doctype / cdata / processing instruction
            if (i + 1 < n && (html[i + 1] == '!' || html[i + 1] == '?')) {
                val end = html.indexOf('>', i)
                i = if (end == -1) n else end + 1
                continue
            }
            // end tag
            if (i + 1 < n && html[i + 1] == '/') {
                val end = html.indexOf('>', i)
                if (end == -1) { i = n; continue }
                val name = html.substring(i + 2, end).trim().substringBefore(' ').lowercase()
                if (name.isNotEmpty()) closeUpTo(name)
                i = end + 1
                continue
            }
            // start tag: must be followed by a letter, otherwise it's literal text
            if (i + 1 >= n || !html[i + 1].isLetter()) {
                addChild(HText("<"))
                i++
                continue
            }
            var j = i + 1
            while (j < n && !html[j].isWhitespace() && html[j] != '>' && html[j] != '/') j++
            val name = html.substring(i + 1, j).lowercase()
            val attrs = LinkedHashMap<String, String>()
            var selfClose = false
            while (j < n) {
                while (j < n && html[j].isWhitespace()) j++
                if (j >= n) break
                val c = html[j]
                if (c == '>') { j++; break }
                if (c == '/') {
                    // "/>" self closing, or stray slash
                    if (j + 1 < n && html[j + 1] == '>') { selfClose = true; j += 2; break }
                    j++
                    continue
                }
                var k = j
                while (k < n && !html[k].isWhitespace() && html[k] != '=' && html[k] != '>' && html[k] != '/') k++
                val an = html.substring(j, k).lowercase()
                j = k
                while (j < n && html[j].isWhitespace()) j++
                var value = ""
                if (j < n && html[j] == '=') {
                    j++
                    while (j < n && html[j].isWhitespace()) j++
                    if (j < n && (html[j] == '"' || html[j] == '\'')) {
                        val q = html[j]
                        val e = html.indexOf(q, j + 1)
                        val stop = if (e == -1) n else e
                        value = html.substring(j + 1, stop)
                        j = if (e == -1) n else e + 1
                    } else {
                        var e = j
                        while (e < n && !html[e].isWhitespace() && html[e] != '>') e++
                        value = html.substring(j, e)
                        j = e
                    }
                }
                if (an.isNotEmpty() && an !in attrs) attrs[an] = decode(value)
            }
            i = j

            // implicit end tags triggered by this start tag
            when (name) {
                "li" -> implicitClose(setOf("li"), setOf("ul", "ol"))
                "dt", "dd" -> implicitClose(setOf("dt", "dd"), setOf("dl"))
                "tr" -> implicitClose(setOf("tr"), setOf("table", "thead", "tbody", "tfoot"))
                "td", "th" -> implicitClose(setOf("td", "th"), setOf("tr", "table"))
                "thead", "tbody", "tfoot" -> implicitClose(setOf("thead", "tbody", "tfoot"), setOf("table"))
                "option" -> implicitClose(setOf("option"), setOf("select"))
            }
            if (name in CLOSES_P) implicitClose(setOf("p"), setOf("div", "section", "li", "td", "th", "table", "ul", "ol", "blockquote"))

            val el = HElement(name, attrs)
            addChild(el)
            if (name in VOID || selfClose) continue
            if (name in RAW_TEXT) {
                val close = html.indexOf("</$name", i, ignoreCase = true)
                val stop = if (close == -1) n else close
                if (stop > i) {
                    val t = HText(html.substring(i, stop))
                    t.parent = el
                    el.children.add(t)
                }
                val gt = if (close == -1) n else html.indexOf('>', close)
                i = if (gt == -1) n else gt + 1
                continue
            }
            stack.add(el)
        }
        return root
    }
}
