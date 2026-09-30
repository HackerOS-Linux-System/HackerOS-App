package com.hackeros.app.data.docs

import com.hackeros.app.data.docs.DocBlock.*

/**
 * Turns one of the HackerOS website's documentation pages (plain HTML, fetched live from the
 * official repository) into native [DocBlock]s grouped into tabs - the on-device equivalent of
 * what the browser does when it renders the page, but producing Compose-friendly structure.
 *
 * Nothing here knows about specific pages: headings, paragraphs, lists, tables, code blocks, info
 * boxes and links are recognised generically, and the page's own sidebar (when it has one) decides
 * how sections are grouped into tabs. So when the website's documentation changes, the app follows.
 */
internal object DocHtmlConverter {

    class Converted(val tabs: List<DocTab>, val lang: String, val title: String)

    private const val SITE = "https://hackeros-linux-system.github.io/HackerOS-Website/"

    // ------------------------------------------------------------------ small helpers -----------
    private val WS = Regex("[\\s\\u00A0]+")
    private fun ws(s: String) = s.replace(WS, " ")

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun plainOf(html: String) = html.replace(Regex("<[^>]+>"), "")

    private fun unesc(s: String) = s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&amp;", "&")

    private val SKIP_IDS = setOf("playground", "sec-playground", "pg", "playground-section")
    private val SKIP_TAGS = setOf(
        "script", "style", "noscript", "button", "select", "option", "svg", "img", "input",
        "textarea", "form", "iframe", "canvas"
    )
    private val CODE_TAGS = setOf("code", "kbd", "ci", "tt", "samp", "var")
    private val CODE_SPAN_RE = Regex("(syntax|mono|cmd|kbd|key|flag|arg|code|inline|tag-)", RegexOption.IGNORE_CASE)
    private val INLINE_TAGS = setOf(
        "a", "abbr", "b", "strong", "i", "em", "span", "code", "kbd", "ci", "tt", "samp", "var",
        "small", "sub", "sup", "u", "mark", "br", "label", "s", "del", "ins", "q", "cite", "font",
        "time", "bdi", "wbr", "data"
    )
    private val HEADINGS = setOf("h1", "h2", "h3", "h4", "h5", "h6")
    private val CONTAINER_SKIP = setOf(
        "nav", "aside", "footer", "script", "style", "noscript", "form", "button", "select",
        "svg", "iframe", "canvas", "template"
    )
    private val DECOR_RE = Regex(
        "(^|[\\s-])(icon|card-icon|lang-icon|feature-icon|code-dots|term-dot|dots|arch-arrow|sidebar|" +
            "nav-link|nav-section|nav-list|copy-btn|copy|step-num|top-bar|breadcrumb|toc|hamburger|" +
            "scroll|progress|badge-row|theme-bar|topbar|lang-switch|search|menu-btn|back-to-top)($|[\\s-])",
        RegexOption.IGNORE_CASE
    )
    private val LABEL_RE = Regex(
        "(title|name|sig|endpoint-header|cmd-name|module-file|theme-name|test-header|pre-label|code-label|" +
            "section-label|term-title|api-sig|card-sub|lang-ext|meta-key|hero-tag|compare-head|cc-feature)",
        RegexOption.IGNORE_CASE
    )
    private val CALLOUT_RE = Regex(
        "(callout|alert|\\bnote\\b|(^|[\\s-])box($|[\\s-])|abox|admonition|\\btip\\b|\\bwarn|hint|" +
            "danger|caution|important|info-box|infobox|notice)",
        RegexOption.IGNORE_CASE
    )
    private val ICON_RE = Regex("\\bfa[srbl]?\\b|\\bfa-|icon")
    private val WARN_RE = Regex("warn|danger|caution|important|abox-w|alert-w|alert-d|error|box-w|box-t\\b")
    private val TIP_RE = Regex("\\btip\\b|success|good|hint|box-g|alert-s")

    private fun calloutKind(c: String): String {
        val l = c.lowercase()
        return when {
            WARN_RE.containsMatchIn(l) -> "warn"
            TIP_RE.containsMatchIn(l) -> "tip"
            else -> "info"
        }
    }

    private fun isIcon(el: HElement) =
        (el.name == "i" || el.name == "span") && ICON_RE.containsMatchIn(el.cls) && el.textOf().isEmpty()

    // ------------------------------------------------------------------ links -------------------
    private fun normPath(p: String): String {
        val parts = ArrayList<String>()
        for (seg in p.split("/")) {
            when (seg) {
                "", "." -> {}
                ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.size - 1)
                else -> parts.add(seg)
            }
        }
        return parts.joinToString("/")
    }

    /** Doc-page links become `hackeros://doc/<key>` (opened natively); real external URLs stay. */
    private fun mapHref(hrefRaw: String?, pageDir: String): String? {
        val href = hrefRaw?.trim() ?: return null
        if (href.isEmpty() || href.startsWith("#") ||
            href.startsWith("javascript:", true) || href.startsWith("mailto:", true)
        ) return null
        val absolute: String = when {
            href.startsWith(SITE) -> href
            href.startsWith("http://") || href.startsWith("https://") -> href
            else -> SITE + normPath((if (pageDir.isNotEmpty()) "$pageDir/" else "") + href)
        }
        val key = DocLinks.resolve(absolute)
        if (key != null) return DocLinks.scheme(key)
        if (absolute.startsWith(SITE)) {
            val path = absolute.removePrefix(SITE).substringBefore('#').substringBefore('?').lowercase()
            if (path in setOf("home-page.html", "index.html", "hackeros-documentation.html", "")) return null
        }
        return absolute
    }

    // ------------------------------------------------------------------ inline ------------------
    private fun inlineNodes(nodes: List<HNode>, pageDir: String, inCode: Boolean = false): String {
        val out = StringBuilder()
        for (ch in nodes) {
            if (ch is HText) {
                out.append(esc(ws(ch.text)))
                continue
            }
            val el = ch as HElement
            val n = el.name
            if (n in SKIP_TAGS || isIcon(el)) continue
            if (n == "br") { out.append("<br>"); continue }
            if (n == "strong" || n == "b") {
                val inner = inlineNodes(el.children, pageDir, inCode)
                if (inner.isNotBlank()) out.append("<strong>").append(inner).append("</strong>")
                continue
            }
            if (n == "em" || n == "i" || n == "cite") {
                val inner = inlineNodes(el.children, pageDir, inCode)
                if (inner.isNotBlank()) out.append("<em>").append(inner).append("</em>")
                continue
            }
            if (n in CODE_TAGS || (n == "span" && CODE_SPAN_RE.containsMatchIn(el.cls) && !inCode)) {
                if (inCode) {
                    out.append(inlineNodes(el.children, pageDir, true))
                } else {
                    val inner = inlineNodes(el.children, pageDir, true)
                    if (inner.isNotBlank()) out.append("<code>").append(inner).append("</code>")
                }
                continue
            }
            if (n == "a") {
                var inner = inlineNodes(el.children, pageDir, inCode)
                val target = mapHref(el.attr("href"), pageDir)
                if (target != null && target.startsWith("hackeros://doc/")) {
                    val plain = plainOf(inner)
                    if (Regex("https?://|github\\.io|\\.html").containsMatchIn(plain)) {
                        inner = esc(DocLinks.titleFor(target.substringAfterLast('/'))) + " ↗"
                    }
                }
                if (target != null && inner.isNotBlank()) {
                    out.append("<a href=\"").append(target).append("\">").append(inner).append("</a>")
                } else out.append(inner)
                continue
            }
            if (n == "ul" || n == "ol") {
                for (li in el.childElements.filter { it.name == "li" }) {
                    out.append("<br>• ").append(inlineNodes(li.children, pageDir, inCode))
                }
                continue
            }
            if (n in setOf("div", "p", "li", "tr", "dt", "dd", "h1", "h2", "h3", "h4", "h5", "h6", "section")) {
                val inner = inlineNodes(el.children, pageDir, inCode)
                if (inner.isNotBlank()) {
                    if (out.isNotEmpty() && !out.endsWith("<br>")) out.append("<br>")
                    out.append(inner)
                }
                continue
            }
            out.append(inlineNodes(el.children, pageDir, inCode))
        }
        return out.toString()
    }

    private fun cleanInline(h0: String): String {
        var h = h0.replace("</code><code>", "")
        h = h.replace(Regex("(<br>\\s*){2,}"), "<br>")
        h = h.replace(Regex("^(\\s|<br>)+"), "")
        h = h.replace(Regex("(\\s|<br>)+$"), "")
        h = h.replace(Regex("\\s+"), " ")
        h = h.replace(Regex("\\s*<br>\\s*"), "<br>")
        return h.trim()
    }

    private fun rawLines(el: HElement): String {
        val sb = StringBuilder()
        fun rec(n: HElement) {
            for (ch in n.children) {
                if (ch is HText) {
                    sb.append(ch.text)
                } else if (ch is HElement) {
                    if (ch.name == "script" || ch.name == "style" || ch.name == "button") continue
                    if (ch.name == "br") { sb.append('\n'); continue }
                    val blockish = ch.name == "div" || ch.name == "p" || ch.name == "li" || ch.name == "tr"
                    if (blockish && sb.isNotEmpty() && sb.last() != '\n') sb.append('\n')
                    rec(ch)
                    if (blockish && (sb.isEmpty() || sb.last() != '\n')) sb.append('\n')
                }
            }
        }
        rec(el)
        val txt = sb.toString().replace("\r", "").replace('\u00A0', ' ')
        var lines = txt.split("\n").map { it.trimEnd() }
        // Markup often puts each line in its own <div> AND separates the divs with newlines, which
        // would double every line break; if most lines are followed by a blank one, drop the blanks.
        val nonBlank = lines.indices.filter { lines[it].isNotBlank() }
        if (nonBlank.size >= 3) {
            val followed = nonBlank.dropLast(1).count { it + 1 < lines.size && lines[it + 1].isBlank() }
            if (followed >= 0.6 * (nonBlank.size - 1)) lines = lines.filter { it.isNotBlank() }
        }
        lines = lines.dropWhile { it.isBlank() }.dropLastWhile { it.isBlank() }
        val ind = lines.filter { it.isNotBlank() }.minOfOrNull { l -> l.length - l.trimStart().length } ?: 0
        return lines.joinToString("\n") { if (it.isNotBlank()) it.substring(minOf(ind, it.length)) else "" }
    }

    // ------------------------------------------------------------------ block walker ------------
    /** A block plus, for h2 blocks, the ids of the element and its ancestors (for sidebar grouping). */
    private class Blk(val b: DocBlock, val ids: List<String> = emptyList())

    private class Walker(val pageDir: String, val h1Split: Boolean) {
        val blocks = ArrayList<Blk>()

        private fun inl(el: HElement) = cleanInline(inlineNodes(el.children, pageDir))

        private fun idsOf(el: HElement): List<String> {
            val ids = ArrayList<String>()
            var n: HElement? = el
            while (n != null) {
                n.attr("id")?.let { if (it.isNotEmpty()) ids.add(it) }
                n = n.parent
            }
            return ids
        }

        private fun emitP(html0: String, out: MutableList<Blk>) {
            val html = cleanInline(html0)
            if (html.split("<br>").size - 1 >= 4 && "<strong>" !in html && "<a " !in html) {
                val segs = html.split("<br>")
                if (segs.all { plainOf(it).length < 100 }) {
                    val txt = unesc(segs.joinToString("\n") { plainOf(it).trim() }).trim()
                    if (txt.isNotEmpty()) out.add(Blk(CodeSample(txt)))
                    return
                }
            }
            val plain = plainOf(html).trim()
            if (plain.isNotEmpty() && plain !in setOf("→", "↓", "|", "•", "·", "-", "—") &&
                !Regex("[A-Z]?\\d{1,2}").matches(plain)
            ) out.add(Blk(Paragraph(html)))
        }

        private fun table(t: HElement): DocBlock? {
            var headers: List<String> = emptyList()
            val rows = ArrayList<List<String>>()
            for (tr in t.findAll("tr")) {
                if (tr.findParent("table") !== t) continue
                val ths = tr.childElements.filter { it.name == "th" }
                val tds = tr.childElements.filter { it.name == "td" }
                if (ths.isNotEmpty() && tds.isEmpty() && headers.isEmpty() && rows.isEmpty()) {
                    headers = ths.map { it.textOf() }
                    continue
                }
                val cellEls = if (ths.isNotEmpty() && tds.isNotEmpty()) ths + tds else if (tds.isNotEmpty()) tds else ths
                val cells = cellEls.map { inl(it) }
                if (cells.any { plainOf(it).isNotBlank() }) rows.add(cells)
            }
            if (rows.isEmpty()) return null
            return Table(headers, rows)
        }

        private fun listItems(el: HElement): List<String> =
            el.childElements.filter { it.name == "li" }
                .map { cleanInline(inlineNodes(it.children, pageDir)) }
                .filter { it.isNotEmpty() }

        fun walkChildren(el: HElement, out: MutableList<Blk>) {
            val buf = StringBuilder()
            fun flush() {
                if (buf.isNotEmpty()) {
                    emitP(buf.toString(), out)
                    buf.setLength(0)
                }
            }
            for (ch in el.children) {
                if (ch is HText) {
                    buf.append(esc(ws(ch.text)))
                    continue
                }
                val e = ch as HElement
                if (e.name in INLINE_TAGS && e.name != "label") {
                    if (e.name == "span" && DECOR_RE.containsMatchIn(e.cls)) continue
                    buf.append(inlineNodes(listOf(e), pageDir))
                    continue
                }
                flush()
                walkBlock(e, out)
            }
            flush()
        }

        private fun wrapCallout(sub: List<Blk>, kind: String, out: MutableList<Blk>) {
            val merged = ArrayList<String>()
            for (blk in sub) {
                when (val b = blk.b) {
                    is Paragraph -> merged.add(b.html)
                    is Heading2 -> merged.add("<strong>${esc(b.text)}</strong>")
                    is Heading3 -> merged.add("<strong>${esc(b.text)}</strong>")
                    is Heading4 -> merged.add("<strong>${esc(b.text)}</strong>")
                    is BulletList -> merged.add(b.itemsHtml.joinToString("<br>") { "• $it" })
                    is NumberedList -> merged.add(b.itemsHtml.joinToString("<br>") { "• $it" })
                    is Command -> merged.add("<code>${esc(b.text.replace("\n", " ; "))}</code>")
                    is CodeSample -> merged.add("<code>${esc(b.text.replace("\n", " ; "))}</code>")
                    is Callout -> merged.add(b.html)
                    else -> { out.addAll(sub); return }
                }
            }
            if (merged.isNotEmpty()) out.add(Blk(Callout(kind, cleanInline(merged.joinToString("<br>")))))
        }

        private fun walkBlock(el: HElement, out: MutableList<Blk>) {
            val n = el.name
            val c = el.cls
            if (n in CONTAINER_SKIP || isIcon(el)) return
            if (el.attr("id") in SKIP_IDS) return
            if (n == "header" && el.findParent("main", "section") == null) return

            if (n in HEADINGS) {
                var txt = el.textOf()
                txt = txt.replace(Regex("\\s*[¶#🔗]\\s*$"), "")
                if (txt.isEmpty()) return
                if (h1Split && n == "h2") { out.add(Blk(Heading3(txt))); return }
                if (n == "h1" || n == "h2") out.add(Blk(Heading2(txt), idsOf(el)))
                else if (n == "h3") out.add(Blk(Heading3(txt)))
                else out.add(Blk(Heading4(txt)))
                return
            }
            if (n == "p") {
                if (CALLOUT_RE.containsMatchIn(c) && !DECOR_RE.containsMatchIn(c)) {
                    val h = inl(el)
                    if (h.isNotEmpty()) out.add(Blk(Callout(calloutKind(c), h)))
                } else {
                    val raw = rawLines(el)
                    val h = cleanInline(inlineNodes(el.children, pageDir))
                    if (Regex("[├└]──").containsMatchIn(raw) || (h.split("<br>").size - 1 >= 4 && "<strong>" !in h)) {
                        if (raw.isNotBlank()) out.add(Blk(CodeSample(raw)))
                    } else emitP(h, out)
                }
                return
            }
            if (n == "pre") {
                val txt = rawLines(el)
                if (txt.isNotBlank()) {
                    out.add(Blk(if ("\n" !in txt && txt.length < 90) Command(txt) else CodeSample(txt)))
                }
                return
            }
            if (n == "table") { table(el)?.let { out.add(Blk(it)) }; return }
            if (n == "ul" || n == "ol") {
                val items = listItems(el)
                if (items.isNotEmpty()) out.add(Blk(if (n == "ul") BulletList(items) else NumberedList(items)))
                return
            }
            if (n == "dl") {
                val items = ArrayList<String>()
                var term: String? = null
                for (ch in el.childElements) {
                    if (ch.name == "dt") term = inl(ch)
                    else if (ch.name == "dd") {
                        items.add((if (term != null) "<strong>$term</strong> — " else "") + inl(ch))
                        term = null
                    }
                }
                if (items.isNotEmpty()) out.add(Blk(BulletList(items)))
                return
            }
            if (n == "hr") { out.add(Blk(Divider)); return }
            if (n == "details") {
                val summ = el.find("summary")
                if (summ != null) {
                    out.add(Blk(Heading4(summ.textOf())))
                    summ.parent?.children?.remove(summ)
                }
                walkChildren(el, out)
                return
            }
            if (n == "blockquote") {
                val h = inl(el)
                if (h.isNotEmpty()) out.add(Blk(Callout("info", h)))
                return
            }

            // ---- div / section / main / article / generic containers
            val blockTags = HEADINGS + setOf("pre", "table", "ul", "ol")
            if ((n == "div" || n == "section") && !el.hasDescendant(blockTags)) {
                val raw = rawLines(el)
                if (Regex("[├└│]").containsMatchIn(raw) ||
                    Regex("(^|\\s)(tree|ascii|diagram|filetree|dirtree|flow-diagram)($|\\s)").containsMatchIn(c)
                ) {
                    if (raw.isNotBlank()) out.add(Blk(CodeSample(raw)))
                    return
                }
            }
            if (Regex("(^|\\s)divider($|\\s)").containsMatchIn(c)) { out.add(Blk(Divider)); return }
            if (DECOR_RE.containsMatchIn(c) && !el.hasDescendant(HEADINGS + setOf("p", "pre", "table"))) return

            val hasHeaderInside = el.find { it.cls.contains("code-header") || it.cls.contains("code-block-header") } != null
            if (Regex("(^|\\s)code-block($|\\s)").containsMatchIn(c) ||
                (el.find("pre") != null && hasHeaderInside && !el.hasDescendant(HEADINGS + setOf("p", "table")))
            ) {
                val fn = el.find { Regex("code-filename|code-label|pre-label|code-title").containsMatchIn(it.cls) }
                val pre = el.find("pre") ?: el.find { it.classTokens.contains("code-inner") }
                    ?: el.find { it.classTokens.contains("code-body") }
                if (pre != null) {
                    val txt = rawLines(pre)
                    if (txt.isNotBlank()) {
                        if (fn != null && fn.textOf().isNotEmpty()) out.add(Blk(Heading4(fn.textOf())))
                        out.add(Blk(if ("\n" !in txt && txt.length < 90) Command(txt) else CodeSample(txt)))
                    }
                }
                return
            }
            if (Regex("(^|\\s)(term|terminal)($|\\s)").containsMatchIn(c) &&
                el.find { Regex("term-body|terminal-body").containsMatchIn(it.cls) } != null
            ) {
                val title = el.find { it.cls.contains("term-title") }
                val body = el.find { Regex("term-body|terminal-body").containsMatchIn(it.cls) }!!
                val txt = rawLines(body)
                if (txt.isNotBlank()) {
                    if (title != null && title.textOf().isNotEmpty()) out.add(Blk(Heading4(title.textOf())))
                    out.add(Blk(CodeSample(txt)))
                }
                return
            }
            if (Regex("(^|\\s)step($|\\s)").containsMatchIn(c) && el.find { it.classTokens.contains("step-num") } != null) {
                var num: String? = el.find { it.classTokens.contains("step-num") }!!.textOf()
                val sub = ArrayList<Blk>()
                val body = el.find { it.classTokens.contains("step-body") } ?: el
                walkChildren(body, sub)
                val res = ArrayList<Blk>(sub)
                for ((idx, blk) in sub.withIndex()) {
                    val b = blk.b
                    if (num != null && (b is Heading3 || b is Heading4 || b is Paragraph)) {
                        res[idx] = Blk(
                            when (b) {
                                is Heading3 -> Heading3("$num. ${b.text}")
                                is Heading4 -> Heading4("$num. ${b.text}")
                                is Paragraph -> Paragraph("<strong>$num.</strong> ${b.html}")
                                else -> b
                            }
                        )
                        num = null
                        break
                    }
                }
                out.addAll(res)
                return
            }
            if (CALLOUT_RE.containsMatchIn(c) && n in setOf("div", "section", "aside", "article")) {
                val sub = ArrayList<Blk>()
                walkChildren(el, sub)
                wrapCallout(sub, calloutKind(c), out)
                return
            }
            val hasBlockDesc = el.hasDescendant(HEADINGS + setOf("p", "pre", "table", "ul", "ol", "div", "section", "article"))
            if (!hasBlockDesc) {
                val h = inl(el)
                val plain = plainOf(h).trim()
                if (plain.isEmpty()) return
                if (Regex("\\d+(\\s*<br>\\s*\\d+)+").matches(h.trim())) return // editor mock-up line numbers
                if (h.split("<br>").size - 1 >= 4 && "<strong>" !in h) {
                    val raw = rawLines(el)
                    if (raw.isNotBlank()) out.add(Blk(CodeSample(raw)))
                    return
                }
                if (LABEL_RE.containsMatchIn(c) && plain.length < 120 && "<br>" !in h) {
                    out.add(Blk(Heading4(unesc(plain).trim())))
                } else emitP(h, out)
                return
            }
            walkChildren(el, out)
        }
    }

    // ------------------------------------------------------------------ sidebar groups ----------
    private val GROUP_LABEL_RE = Regex(
        "(^|\\s)(nav-section|sidebar-section|sidebar-title|nav-group|nav-title|nav-label|sidenav-title|" +
            "nav-heading|nav-cat|group-title|nav-group-title|toc-title|section-title)($|\\s)"
    )

    class Group(val label: String, val ids: List<String>)

    private fun ownText(el: HElement): String {
        val t = el.children.filterIsInstance<HText>().joinToString("") { it.text }.trim()
        if (t.isNotEmpty()) return ws(t)
        for (c in el.childElements) {
            if (c.name != "a" && c.find("a") == null) {
                val tt = c.textOf()
                if (tt.isNotEmpty()) return tt
            }
        }
        return ""
    }

    fun navGroups(root: HElement): List<Group> {
        val conts = root.findAll("nav", "aside") + root.findAll("div").filter {
            Regex("sidebar|sidenav|toc|side-nav|doc-nav", RegexOption.IGNORE_CASE).containsMatchIn(it.cls)
        }
        var best: List<Group> = emptyList()
        for (cont in conts) {
            val got = ArrayList<Pair<String, MutableList<String>>>()
            var cur: Pair<String, MutableList<String>>? = null
            for (el in cont.descendants()) {
                val c = el.cls
                if (el.name == "a") {
                    val h = el.attr("href") ?: ""
                    if (h.startsWith("#") && h.length > 1) {
                        if (cur == null) { cur = "" to ArrayList(); got.add(cur) }
                        cur.second.add(h.substring(1))
                    }
                    continue
                }
                val isLabel = GROUP_LABEL_RE.containsMatchIn(c) || el.name in setOf("h3", "h4", "h5", "h6")
                if (isLabel && !Regex("logo|version").containsMatchIn(c)) {
                    val label = if (el.find("a") != null) ownText(el) else el.textOf()
                    if (label.isNotEmpty()) { cur = label to ArrayList(); got.add(cur) }
                }
            }
            val cleaned = got.map { Group(it.first, it.second.filter { id -> id !in SKIP_IDS }) }.filter { it.ids.isNotEmpty() }
            if (cleaned.sumOf { it.ids.size } > best.sumOf { it.ids.size }) best = cleaned
        }
        return if (best.size >= 2) best.map { Group(it.label.ifEmpty { "…" }, it.ids) } else emptyList()
    }

    // ------------------------------------------------------------------ tabs --------------------
    private fun cleanLabel(t0: String): String {
        var t = t0.replace(Regex("^\\s*(\\d{1,2}|[IVX]+)\\s*[.)\\-–—·:]*\\s+"), "")
        t = t.replace(Regex("^[^\\p{L}\\p{N}_#.(\\[]+"), "")
        t = t.trim()
        return if (t.isNotEmpty()) t.substring(0, 1).uppercase() + t.substring(1) else "…"
    }

    private fun cap(s: String, n: Int = 26) = if (s.length <= n) s else s.substring(0, n - 1).trimEnd() + "…"

    private class Section(var title: String?, var ids: List<String>, var blocks: MutableList<Blk>)

    private fun buildTabs(blocks: List<Blk>, groups: List<Group>, introLabel: String, maxTabs: Int = 14): List<Pair<String, List<Blk>>> {
        var sections = ArrayList<Section>()
        var cur = Section(null, emptyList(), ArrayList())
        for (b in blocks) {
            if (b.b is Heading2) {
                if (cur.title != null || cur.blocks.isNotEmpty()) sections.add(cur)
                cur = Section(b.b.text, b.ids, arrayListOf(Blk(Heading2(b.b.text))))
            } else cur.blocks.add(b)
        }
        if (cur.title != null || cur.blocks.isNotEmpty()) sections.add(cur)

        val original = ArrayList(sections)
        val merged = ArrayList<Section>()
        var pending: Section? = null
        for (s in original) {
            if (pending != null) {
                s.blocks = (pending.blocks + s.blocks).toMutableList()
                if (s.title == null) { s.title = pending.title; s.ids = pending.ids }
                pending = null
            }
            if (s.title != null && s.blocks.size == 1 && s !== original.last()) {
                pending = s
                continue
            }
            merged.add(s)
        }
        if (pending != null) merged.add(pending)
        sections = merged
        if (sections.isEmpty()) return emptyList()

        var intro: Section? = null
        if (sections[0].title == null) {
            intro = sections.removeAt(0)
            if (sections.isNotEmpty() && intro.blocks.count { it.b !== Divider } <= 3) {
                sections[0].blocks = (intro.blocks + sections[0].blocks).toMutableList()
                intro = null
            }
        }

        val tabs = ArrayList<Pair<String, List<Blk>>>()
        fun addTab(label: String, secs: List<Section>) {
            val bl = secs.flatMap { it.blocks }
            if (bl.isNotEmpty()) tabs.add(label to bl)
        }

        if (groups.isNotEmpty() && sections.size > 1) {
            val id2g = HashMap<String, Int>()
            groups.forEachIndexed { gi, g -> g.ids.forEach { id2g.getOrPut(it) { gi } } }
            val buckets = java.util.TreeMap<Int, MutableList<Section>>()
            var last = 0
            for (s in sections) {
                val gi = s.ids.firstNotNullOfOrNull { id2g[it] } ?: last
                last = gi
                buckets.getOrPut(gi) { ArrayList() }.add(s)
            }
            if (intro != null && intro.blocks.isNotEmpty() && groups.size <= maxTabs) addTab(introLabel, listOf(intro))
            for ((gi, secs) in buckets) addTab(cap(cleanLabel(groups[gi].label)), secs)
        } else if (sections.size <= maxTabs) {
            if (intro != null && intro.blocks.isNotEmpty()) addTab(introLabel, listOf(intro))
            for (s in sections) addTab(cap(cleanLabel(s.title ?: introLabel)), listOf(s))
        } else {
            if (intro != null && intro.blocks.isNotEmpty()) addTab(introLabel, listOf(intro))
            val k = Math.ceil(sections.size.toDouble() / (maxTabs - (if (intro != null) 1 else 0))).toInt()
            var i = 0
            while (i < sections.size) {
                val chunk = sections.subList(i, minOf(i + k, sections.size))
                val first = cleanLabel(chunk[0].title ?: "")
                val second = if (chunk.size > 1) cleanLabel(chunk[1].title ?: "") else ""
                val label = if (second.isEmpty()) cap(first, 28) else cap("$first · $second", 30)
                addTab(label, chunk)
                i += k
            }
        }
        // de-duplicate labels
        val seen = HashMap<String, Int>()
        return tabs.map { (label, bl) ->
            val cnt = (seen[label] ?: 0) + 1
            seen[label] = cnt
            (if (cnt > 1) "$label ($cnt)" else label) to bl
        }
    }

    private fun collapse(blocks: List<Blk>): List<DocBlock> {
        val res = ArrayList<DocBlock>()
        for (blk in blocks) {
            val b = blk.b
            if (res.isNotEmpty() && res.last() === Divider && b === Divider) continue
            if (res.isNotEmpty() && res.last() == b && (b is Heading2 || b is Heading3 || b is Heading4)) continue
            res.add(b)
        }
        while (res.isNotEmpty() && res.first() === Divider) res.removeAt(0)
        while (res.isNotEmpty() && res.last() === Divider) res.removeAt(res.size - 1)
        return res
    }

    private fun polishRatio(text: String): Double {
        if (text.isEmpty()) return 0.0
        return text.count { it in "ąćęłńóśźżĄĆĘŁŃÓŚŹŻ" }.toDouble() / maxOf(1, text.length)
    }

    /**
     * @param extraGroups sidebar groups to use instead of the ones found in [html] itself (H#'s
     *   content is assembled from several script files while its sidebar lives in docs.html).
     */
    fun convert(
        html: String,
        pageDir: String,
        introPl: String = "Wstęp",
        introEn: String = "Overview",
        extraGroups: List<Group>? = null
    ): Converted {
        val doc = HtmlDom.parse(html)
        val groups = extraGroups ?: navGroups(doc)
        val body = doc.find("body") ?: doc
        val main = doc.find("main")
        var root: HElement = main ?: body
        if (main == null) {
            val cand = body.findAll("div", "section").firstOrNull { el ->
                el.classTokens.any { it in setOf("content", "main", "doc-content", "docs", "container", "page") }
            }
            if (cand != null) root = cand
        }
        val w = Walker(pageDir, h1Split = root.findAll("h1").size >= 4)
        w.walkChildren(root, w.blocks)
        val plain = w.blocks.map { it.b }.mapNotNull {
            when (it) {
                is Paragraph -> plainOf(it.html)
                is Heading2 -> it.text
                is Heading3 -> it.text
                else -> null
            }
        }.joinToString(" ")
        val lang = if (polishRatio(plain) > 0.008) "pl" else "en"
        val tabs = buildTabs(w.blocks, groups, if (lang == "pl") introPl else introEn)
            .mapIndexed { i, (label, bl) -> DocTab("t$i", label, collapse(bl)) }
            .filter { it.blocks.isNotEmpty() }
            .mapIndexed { i, t -> DocTab("t$i", t.label, t.blocks) }
        val title = doc.find("title")?.textOf() ?: ""
        return Converted(tabs, lang, title)
    }
}
