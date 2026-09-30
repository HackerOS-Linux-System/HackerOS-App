package com.hackeros.app.data.docs

import android.content.Context
import com.hackeros.app.data.docs.DocBlock.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/** Returns a file of the website repository (by relative path) - live or from the offline copy. */
typealias DocFetch = suspend (String) -> DocRemote.Fetched

/**
 * Loads the documentation pages (H#, Hacker Lang, HackerScript, every tool, the tools index, the
 * download page ...) **live from the official HackerOS website repository** and renders them
 * natively - no WebView, no browser, and nothing bundled in the APK.
 *
 * Every page is fetched from the website's own source files on GitHub
 * (`raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-Website/main/...`), converted on the
 * device by [DocHtmlConverter], and cached for offline use. Publish or edit a page on the website
 * and the app shows the change - no app update needed.
 */
object DocRemote {

    const val RAW_BASE = "https://raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-Website/main/"

    sealed class Fetched {
        class Ok(val text: String, val fromCache: Boolean) : Fetched()
        object NotFound : Fetched()
        object Failed : Fetched()
    }

    class Page(val page: DetailPage, val fromCache: Boolean)

    // ------------------------------------------------------------------ Android glue ------------
    private val memory = ConcurrentHashMap<String, Page>()

    /** Forget everything held in memory (offline copies on disk stay) - used by the Retry button. */
    fun clearMemory() = memory.clear()

    suspend fun load(context: Context, key: String, langCode: String, force: Boolean = false): Page? {
        val memKey = "$key|$langCode"
        if (!force) memory[memKey]?.let { return it }
        val dir = File(context.filesDir, "doc-cache").apply { mkdirs() }
        val fetch: DocFetch = { path -> networkOrCache(dir, path) }
        val page = try {
            build(key, langCode, fetch)
        } catch (e: Exception) {
            null
        }
        if (page != null) memory[memKey] = page
        return page
    }

    private suspend fun networkOrCache(dir: File, path: String): Fetched = withContext(Dispatchers.IO) {
        val cacheFile = File(dir, path.replace('/', '_') + ".txt")
        try {
            val conn = URL(RAW_BASE + path + "?t=" + System.currentTimeMillis()).openConnection() as HttpURLConnection
            conn.connectTimeout = 12000
            conn.readTimeout = 20000
            conn.setRequestProperty("User-Agent", "HackerOS-App")
            try {
                val code = conn.responseCode
                if (code == 404) return@withContext Fetched.NotFound
                if (code !in 200..299) throw java.io.IOException("HTTP $code")
                val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                try { cacheFile.writeText(text, Charsets.UTF_8) } catch (_: Exception) { }
                Fetched.Ok(text, fromCache = false)
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            if (cacheFile.exists()) {
                try { Fetched.Ok(cacheFile.readText(Charsets.UTF_8), fromCache = true) } catch (_: Exception) { Fetched.Failed }
            } else Fetched.Failed
        }
    }

    // ------------------------------------------------------------------ page assembly -----------
    /** Builds the page for [key]; [fetch] returns a file of the website repo by relative path. */
    suspend fun build(key: String, langCode: String, fetch: DocFetch): Page? {
        var usedCache = false
        val tracking: DocFetch = { p ->
            val r = fetch(p)
            if (r is Fetched.Ok && r.fromCache) usedCache = true
            r
        }
        val page = when (key) {
            "tools-index" -> buildToolsIndex(langCode, tracking)
            "download" -> buildDownload(langCode, tracking)
            "h-sharp" -> buildHSharp(tracking)
            else -> buildStandard(key, langCode, tracking)
        } ?: return null
        return Page(page, usedCache)
    }

    private val SPECIAL_PATHS = mapOf(
        "hacker-lang" to "hacker-lang/docs.html",
        "hackerscript" to "tools-docs/HackerScript/docs.html",
        "hackeros-games" to "hackeros-games/docs.html",
        "blue-environment" to "tools-docs/Blue-Environment/docs.html",
        "hwde" to "tools-docs/HWDE/docs.html",
        "system-updates" to "system-updates/docs.html",
        "hacker-launcher" to "tools-docs/Hacker-Launcher.html"
    )

    private fun uiLang(langCode: String) = if (langCode == "pl") "pl" else "en"

    private fun dirOf(path: String) = path.substringBeforeLast('/', "")

    private fun titleOf(key: String, htmlTitle: String) =
        DocLinks.titleFor(key).takeIf { it != key } ?: htmlTitle.ifBlank { key }

    private suspend fun buildStandard(key: String, langCode: String, fetch: DocFetch): DetailPage? {
        val safe = key.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
        if (safe.isEmpty()) return null
        var candidates = listOf(SPECIAL_PATHS[safe] ?: "tools-docs/$safe.html")
        var triedIndex = false
        var i = 0
        while (i < candidates.size) {
            val path = candidates[i++]
            when (val r = fetch(path)) {
                is Fetched.Failed -> return null // offline and nothing cached
                is Fetched.NotFound -> {}
                is Fetched.Ok -> {
                    if (r.text.trim().length >= 50) {
                        val conv = DocHtmlConverter.convert(r.text, dirOf(path))
                        if (conv.tabs.isNotEmpty()) {
                            return DetailPage(safe, titleOf(safe, conv.title), conv.lang, conv.tabs)
                        }
                    }
                    return buildStub(safe, langCode, fetch) // page exists but has no content yet
                }
            }
            if (i == candidates.size && !triedIndex) {
                triedIndex = true
                // Filenames on GitHub are case-sensitive; the tools index lists the real ones.
                val index = fetch("tools-docs/index.html")
                if (index is Fetched.Ok) {
                    val nav = HtmlDom.parse(index.text).findAll("a").mapNotNull { a ->
                        val href = a.attr("href") ?: return@mapNotNull null
                        if (href.startsWith("http") || !href.endsWith(".html")) return@mapNotNull null
                        val k = a.attr("data-key") ?: href.substringAfterLast('/').removeSuffix(".html")
                        if (k.lowercase() == safe || href.substringAfterLast('/').removeSuffix(".html").lowercase() == safe)
                            "tools-docs/" + href else null
                    }.filter { it !in candidates }
                    candidates = candidates + nav
                }
            }
        }
        return buildStub(safe, langCode, fetch)
    }

    // ------------------------------------------------------------------ tools metadata ----------
    private class ToolsMeta(val slides: List<Pair<String, String>>, val navItems: Map<String, String>)

    // Slide titles that differ from the navigation label of the same tool.
    private val SLIDE_ALIASES = mapOf("HackerOS Nix Manager" to "hnm")

    private suspend fun toolsMeta(langCode: String, fetch: DocFetch): ToolsMeta? {
        val js = (fetch("translations/tools-docs.js") as? Fetched.Ok)?.text ?: return null
        val root = literal(js, Regex("HACKEROS_TRANS_TOOLS\\s*=\\s*"))?.let { JSONObject(it) } ?: return null
        val lang = root.optJSONObject(langCode) ?: root.optJSONObject("en") ?: return null
        val slidesArr = lang.optJSONArray("slides") ?: JSONArray()
        val slides = (0 until slidesArr.length()).mapNotNull {
            val o = slidesArr.optJSONObject(it) ?: return@mapNotNull null
            o.optString("title") to o.optString("desc")
        }
        val nav = lang.optJSONObject("navItems")
        val navMap = LinkedHashMap<String, String>()
        nav?.keys()?.forEach { navMap[it] = nav.optString(it) }
        return ToolsMeta(slides, navMap)
    }

    private fun keyForSlide(title: String, meta: ToolsMeta): String? =
        meta.navItems.entries.firstOrNull { it.value.equals(title, ignoreCase = true) }?.key ?: SLIDE_ALIASES[title]

    private suspend fun buildStub(key: String, langCode: String, fetch: DocFetch): DetailPage {
        val lang = uiLang(langCode)
        val meta = toolsMeta(langCode, fetch)
        val slide = meta?.slides?.firstOrNull { keyForSlide(it.first, meta) == key }
        val title = slide?.first ?: DocLinks.titleFor(key)
        val note = if (lang == "pl")
            "Rozszerzona dokumentacja tego narzędzia nie została jeszcze opublikowana na oficjalnej stronie HackerOS."
        else "Extended documentation for this tool has not been published on the official HackerOS website yet."
        val blocks = ArrayList<DocBlock>()
        blocks += Heading2(title)
        if (slide != null && slide.second.isNotBlank()) {
            blocks += Paragraph(escape(slide.second))
        } else {
            // Editions (HWDE, Blue ...) are described in the website's documentation data instead.
            editionDescription(if (key == "blue-environment") "blue" else key, langCode, fetch)
                ?.let { blocks += Paragraph(it) }
        }
        blocks += Callout("info", note)
        return DetailPage(key, title, lang, listOf(DocTab("t0", if (lang == "pl") "O narzędziu" else "About", blocks)))
    }

    private suspend fun editionDescription(editionKey: String, langCode: String, fetch: DocFetch): String? {
        val js = (fetch("translations/hackeros-documentation.js") as? Fetched.Ok)?.text ?: return null
        val text = DocContentParser.extractLangMeta(js, langCode)?.content?.optJSONObject("editions")?.optString(editionKey, "")
            .orEmpty().ifBlank {
                DocContentParser.extractLangMeta(js, "en")?.content?.optJSONObject("editions")?.optString(editionKey, "").orEmpty()
            }
        return text.trim().ifBlank { null }
    }

    private fun escape(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private suspend fun buildToolsIndex(langCode: String, fetch: DocFetch): DetailPage? {
        val lang = uiLang(langCode)
        val meta = toolsMeta(langCode, fetch) ?: return null
        val tools = ArrayList<DocBlock>()
        tools += Heading2(if (lang == "pl") "Narzędzia" else "Tools")
        tools += Paragraph(
            if (lang == "pl") "Ekosystem HackerOS - stuknij narzędzie, aby otworzyć jego dokumentację."
            else "The HackerOS ecosystem - tap a tool to open its documentation."
        )
        for ((title, desc) in meta.slides) {
            val k = keyForSlide(title, meta) ?: continue
            tools += DetailCard(title, desc, k)
        }
        val langs = ArrayList<DocBlock>()
        langs += Heading2(if (lang == "pl") "Języki programowania" else "Programming languages")
        langs += Paragraph(
            if (lang == "pl") "Języki programowania HackerOS - stuknij, aby otworzyć dokumentację."
            else "HackerOS programming languages - tap one to open its documentation."
        )
        val cards = if (lang == "pl") listOf(
            Triple("H#", "Kompilowany, statycznie typowany język HackerOS (LLVM) z menedżerem pakietów bytes.", "h-sharp"),
            Triple("Hacker Lang", "Wydajna alternatywa dla shella z wyjątkową składnią i własną powłoką.", "hacker-lang"),
            Triple("HackerScript", "Eksperymentalny język hobbystyczny transpilowany do Rust i Python.", "hackerscript")
        ) else listOf(
            Triple("H#", "HackerOS' compiled, statically typed language (LLVM) with the bytes package manager.", "h-sharp"),
            Triple("Hacker Lang", "An efficient shell alternative with a unique syntax and its own shell.", "hacker-lang"),
            Triple("HackerScript", "An experimental hobby language transpiled to Rust and Python.", "hackerscript")
        )
        cards.forEach { langs += DetailCard(it.first, it.second, it.third) }
        return DetailPage(
            "tools-index", if (lang == "pl") "HackerOS · Narzędzia" else "HackerOS · Tools", lang,
            listOf(
                DocTab("t0", if (lang == "pl") "Narzędzia" else "Tools", tools),
                DocTab("t1", if (lang == "pl") "Języki programowania" else "Programming languages", langs)
            )
        )
    }

    // ------------------------------------------------------------------ download page -----------
    private suspend fun buildDownload(langCode: String, fetch: DocFetch): DetailPage? {
        val lang = uiLang(langCode)
        val edJs = (fetch("translations/download-editions.js") as? Fetched.Ok)?.text ?: return null
        val editions = literal(edJs, Regex("HACKEROS_DOWNLOAD_EDITIONS\\s*=\\s*"))?.let { JSONArray(it) } ?: return null
        val docsJs = (fetch("translations/hackeros-documentation.js") as? Fetched.Ok)?.text
        val descriptions: JSONObject? = docsJs?.let { js ->
            DocContentParser.extractLangMeta(js, langCode)?.content?.optJSONObject("editions")
                ?: DocContentParser.extractLangMeta(js, "en")?.content?.optJSONObject("editions")
        }
        val sourceNames = listOf("sf" to "SourceForge", "mega" to "Mega", "drive" to "Google Drive", "transfer" to "transfer.it")
        val soon = if (lang == "pl") "wkrótce" else "soon"
        val blocks = ArrayList<DocBlock>()
        blocks += Heading2(if (lang == "pl") "Pobieranie" else "Download")
        blocks += Paragraph(
            if (lang == "pl") "Pobierz obraz ISO wybranej edycji. Same pliki ISO są hostowane u zewnętrznych dostawców (SourceForge, Mega, Google Drive, transfer.it) - stuknięcie w źródło rozpocznie pobieranie w przeglądarce."
            else "Download the ISO of the edition you want. The ISO files themselves are hosted by external providers (SourceForge, Mega, Google Drive, transfer.it) - tapping a source starts the download in your browser."
        )
        for (i in 0 until editions.length()) {
            val ed = editions.optJSONObject(i) ?: continue
            blocks += Heading3(ed.optString("name"))
            val d = descriptions?.optString(ed.optString("docsKey"), "").orEmpty()
            if (d.isNotBlank()) blocks += Paragraph(d.trim())
            val excluded = ed.optJSONArray("excluded")?.let { a -> (0 until a.length()).map { a.optString(it) } }.orEmpty()
            val links = ed.optJSONObject("links")
            val parts = ArrayList<String>()
            for ((k, label) in sourceNames) {
                if (k in excluded) continue
                val u = if (links == null || links.isNull(k)) "" else links.optString(k)
                parts += if (u.isNotBlank()) "<a href=\"$u\">$label</a>" else "$label ($soon)"
            }
            blocks += Paragraph("<strong>${if (lang == "pl") "Źródła" else "Sources"}:</strong> " + parts.joinToString(" · "))
        }
        return DetailPage(
            "download", if (lang == "pl") "Pobieranie" else "Download", lang,
            listOf(DocTab("t0", if (lang == "pl") "Edycje" else "Editions", blocks))
        )
    }

    // ------------------------------------------------------------------ H# ----------------------
    // The H# documentation page is a shell (docs.html) whose sections are injected at runtime from
    // h-sharp/sections-js/<name>.js (template literals). We fetch the section list from the site's
    // own script.js, then every section, exactly like the browser does.
    private val TEMPLATE_RE = Regex("=\\s*`(.*)`\\s*;?\\s*$", RegexOption.DOT_MATCHES_ALL)
    private val ESCAPE_RE = Regex("\\\\(.)", RegexOption.DOT_MATCHES_ALL)

    private suspend fun buildHSharp(fetch: DocFetch): DetailPage? {
        val script = (fetch("h-sharp/script.js") as? Fetched.Ok)?.text ?: return null
        val order = Regex("\\['sec-[a-z\\-]+',\\s*'([a-z\\-]+)'\\]").findAll(script).map { it.groupValues[1] }
            .filter { it != "playground" } // interactive WASM playground: cannot run natively
            .toList()
        if (order.isEmpty()) return null
        val sections = coroutineScope {
            order.map { name ->
                async {
                    val file = name.replace("examples-extra", "examples_extra")
                    val r = fetch("h-sharp/sections-js/$file.js") as? Fetched.Ok ?: return@async ""
                    val m = TEMPLATE_RE.find(r.text) ?: return@async ""
                    ESCAPE_RE.replace(m.groupValues[1]) { mm ->
                        when (val c = mm.groupValues[1]) { "n" -> "\n"; "t" -> "\t"; else -> c }
                    }
                }
            }.awaitAll()
        }
        if (sections.all { it.isBlank() }) return null
        val docsShell = (fetch("h-sharp/docs.html") as? Fetched.Ok)?.text
        val groups = docsShell?.let { DocHtmlConverter.navGroups(HtmlDom.parse(it)) }.orEmpty()
        val html = "<main>" + sections.joinToString("\n") + "</main>"
        val conv = DocHtmlConverter.convert(html, "h-sharp", introPl = "Wprowadzenie", extraGroups = groups)
        if (conv.tabs.isEmpty()) return null
        return DetailPage("h-sharp", "H#", conv.lang, conv.tabs)
    }

    // ------------------------------------------------------------------ JS literal extraction ---
    /**
     * Finds `... = { ... }` / `... = [ ... ]` after [assign] in a website data file and returns it
     * as strict JSON (the files use unquoted keys, single quotes, comments and trailing commas).
     */
    internal fun literal(js: String, assign: Regex): String? {
        val m = assign.find(js) ?: return null
        var start = m.range.last + 1
        while (start < js.length && js[start] != '{' && js[start] != '[') start++
        if (start >= js.length) return null
        var depth = 0
        var i = start
        var inStr: Char? = null
        var esc = false
        var end = -1
        while (i < js.length) {
            val ch = js[i]
            if (inStr != null) {
                when {
                    esc -> esc = false
                    ch == '\\' -> esc = true
                    ch == inStr -> inStr = null
                }
            } else if (ch == '/' && i + 1 < js.length && js[i + 1] == '/') {
                while (i < js.length && js[i] != '\n') i++
                continue
            } else if (ch == '/' && i + 1 < js.length && js[i + 1] == '*') {
                val e = js.indexOf("*/", i + 2)
                i = if (e == -1) js.length else e + 2
                continue
            } else {
                when (ch) {
                    '\'', '"', '`' -> inStr = ch
                    '{', '[' -> depth++
                    '}', ']' -> {
                        depth--
                        if (depth == 0) { end = i; break }
                    }
                }
            }
            i++
        }
        if (end == -1) return null
        return try {
            JsLenientJson.convert(js.substring(start, end + 1))
        } catch (e: Exception) {
            null
        }
    }
}
