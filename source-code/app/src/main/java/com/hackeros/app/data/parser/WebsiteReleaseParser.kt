package com.hackeros.app.data.parser

import com.hackeros.app.data.model.Language
import com.hackeros.app.data.model.ReleaseInfo

/**
 * Parses HackerOS release data directly from the official website's real, always-populated
 * per-language data files:
 *
 *   https://raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-Website/main/translations/files/all/{lang}.js
 *
 * Each of these files assigns a plain JS array literal, e.g.:
 *
 *   window.HACKEROS_RELEASES_ALL = window.HACKEROS_RELEASES_ALL || {};
 *   window.HACKEROS_RELEASES_ALL.pl = [
 *       { version: "HackerOS V4.9", desc: "...", dates: ["...", "..."], changelog: ["...", "..."] },
 *       ...
 *   ];
 *
 * This is the *actual* source of truth for https://hackeros-linux-system.github.io/HackerOS-Website/releases.html:
 * the website's `translations/releases.js` only ships empty `releases: []` placeholders that get
 * filled in at runtime, in the browser, from exactly these per-language files - so parsing
 * `releases.js` directly (as older versions of this app did) always yields zero releases even
 * with a perfectly healthy connection. Reading the per-language file directly is both simpler
 * and matches what a user actually sees on the website.
 *
 * The website currently publishes real (non-empty) release data for all 10 supported languages
 * (pl, en, de, es, fr, it, ru, uk, zh, ja), so no cross-language fallback is normally needed -
 * but one is still included for resilience in case a specific language file is temporarily empty.
 *
 * The parser does not depend on a full JS/JSON engine; it does small, dependency-free
 * bracket-depth scanning (release objects here never nest `{}` inside `{}`, only `[]` arrays for
 * `dates`/`changelog`), which keeps it robust to minor upstream formatting changes.
 */
object WebsiteReleaseParser {

    fun parse(jsText: String, language: Language): List<ReleaseInfo> {
        val primary = extractReleasesArray(jsText, language.code)
        if (primary.isNotEmpty()) return primary
        // Resilience fallback: if this specific language's array is empty/missing for some
        // reason, fall back to English so the screen never renders completely empty.
        if (language != Language.EN) {
            val en = extractReleasesArray(jsText, Language.EN.code)
            if (en.isNotEmpty()) return en
        }
        return emptyList()
    }

    // --- Locate `window.HACKEROS_RELEASES_ALL.<lang> = [ ... ];` and return its parsed items ---
    private fun extractReleasesArray(text: String, langCode: String): List<ReleaseInfo> {
        val assignPattern = Regex(
            "HACKEROS_RELEASES_ALL(?:\\[['\"]$langCode['\"]\\]|\\.$langCode)\\s*=\\s*\\["
        )
        val match = assignPattern.find(text) ?: return emptyList()
        val arrStart = text.indexOf('[', match.range.first)
        if (arrStart == -1) return emptyList()

        var depth = 0
        var i = arrStart
        var arrEnd = -1
        while (i < text.length) {
            when (text[i]) {
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) {
                        arrEnd = i
                        break
                    }
                }
            }
            i++
        }
        if (arrEnd == -1) return emptyList()

        val arrayText = text.substring(arrStart + 1, arrEnd)

        // Split into individual release objects by brace depth.
        val objects = mutableListOf<String>()
        var d = 0
        var objStart = -1
        var j = 0
        while (j < arrayText.length) {
            when (arrayText[j]) {
                '{' -> {
                    if (d == 0) objStart = j
                    d++
                }
                '}' -> {
                    d--
                    if (d == 0 && objStart != -1) {
                        objects.add(arrayText.substring(objStart, j + 1))
                        objStart = -1
                    }
                }
            }
            j++
        }

        // Preserve the website's ordering (newest first, as published).
        return objects.mapNotNull { parseReleaseObject(it) }
    }

    private fun parseReleaseObject(obj: String): ReleaseInfo? {
        val version = extractStringField(obj, "version") ?: return null
        val desc = extractStringField(obj, "desc") ?: ""
        val dates = extractStringArrayField(obj, "dates")
        val changelog = extractStringArrayField(obj, "changelog")
        return ReleaseInfo(
            version = version,
            description = desc,
            editions = dates.joinToString("\n"),
            news = changelog.joinToString("\n")
        )
    }

    // Matches key: "value" (double-quoted, the format used by all real release data files),
    // with a single-quote fallback kept for defensiveness against future formatting changes.
    private fun extractStringField(obj: String, key: String): String? {
        val dq = Regex("$key\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
        dq.find(obj)?.let { return unescape(it.groupValues[1]) }
        val sq = Regex("$key\\s*:\\s*'((?:[^'\\\\]|\\\\.)*)'")
        sq.find(obj)?.let { return unescape(it.groupValues[1]) }
        return null
    }

    private fun extractStringArrayField(obj: String, key: String): List<String> {
        val startIdx = Regex("$key\\s*:").find(obj)?.range?.first ?: return emptyList()
        val bracketStart = obj.indexOf('[', startIdx)
        if (bracketStart == -1) return emptyList()

        var depth = 0
        var i = bracketStart
        var end = -1
        while (i < obj.length) {
            when (obj[i]) {
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) {
                        end = i
                        break
                    }
                }
            }
            i++
        }
        if (end == -1) return emptyList()

        val arrText = obj.substring(bracketStart + 1, end)
        val itemRegex = Regex("\"((?:[^\"\\\\]|\\\\.)*)\"|'((?:[^'\\\\]|\\\\.)*)'")
        return itemRegex.findAll(arrText).map {
            unescape(it.groupValues[1].ifEmpty { it.groupValues[2] })
        }.toList()
    }

    private fun unescape(s: String): String =
        s.replace("\\\"", "\"")
            .replace("\\'", "'")
            .replace("\\n", "\n")
            .replace("\\\\", "\\")
}
