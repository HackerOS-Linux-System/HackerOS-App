package com.hackeros.app.data.parser

import com.hackeros.app.data.model.Language
import com.hackeros.app.data.model.ReleaseInfo

/**
 * Parses HackerOS release data directly from the official website's release data file:
 * https://raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-Website/main/translations/releases.js
 *
 * This is the very same source that powers https://hackeros-linux-system.github.io/HackerOS-Website/releases.html,
 * so the app always shows exactly what's published on the website - no separate/duplicated release file needed.
 *
 * The file assigns `window.HACKEROS_TRANS_RELEASES` with a base object literal containing fully
 * localized "pl" and "en" blocks (each with its own translated `releases` array), followed by
 * `Object.assign(...)` calls that add UI-label translations for the remaining languages while
 * reusing the English release content (this mirrors exactly how the website itself behaves when
 * you switch its language switcher). This parser follows the same rule: for Language.PL it reads
 * the "pl" block, for every other supported language it reads the "en" block, so that changing the
 * language in-app translates the releases screen exactly like the website does.
 *
 * The parser does not rely on a full JS engine; it does a small amount of manual bracket-depth
 * scanning (since the release objects/arrays in this file never nest braces inside braces) which
 * keeps it dependency-free and robust to minor formatting changes upstream.
 */
object WebsiteReleaseParser {

    fun parse(jsText: String, language: Language): List<ReleaseInfo> {
        val blockKey = if (language == Language.PL) "pl" else "en"
        val block = extractLangBlock(jsText, blockKey) ?: extractLangBlock(jsText, "en")
        ?: return emptyList()
        val releases = extractReleases(block)
        return if (releases.isNotEmpty()) releases else run {
            // Extra safety net: if for some reason the requested block had no releases array,
            // fall back to the English block before giving up entirely.
            if (blockKey != "en") {
                extractLangBlock(jsText, "en")?.let { extractReleases(it) } ?: emptyList()
            } else emptyList()
        }
    }

    // --- Locate the top-level "key: { ... }" block for a given language code ---
    private fun extractLangBlock(text: String, key: String): String? {
        val startPattern = Regex("(?m)^\\s*$key:\\s*\\{")
        val match = startPattern.find(text) ?: return null
        val braceStart = text.indexOf('{', match.range.first)
        if (braceStart == -1) return null

        var depth = 0
        var i = braceStart
        while (i < text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return text.substring(braceStart, i + 1)
                }
            }
            i++
        }
        return null
    }

    // --- Extract the `releases: [ {...}, {...}, ... ]` array from a language block ---
    private fun extractReleases(block: String): List<ReleaseInfo> {
        val releasesKeyIdx = block.indexOf("releases:")
        if (releasesKeyIdx == -1) return emptyList()
        val arrStart = block.indexOf('[', releasesKeyIdx)
        if (arrStart == -1) return emptyList()

        var depth = 0
        var i = arrStart
        var arrEnd = -1
        while (i < block.length) {
            when (block[i]) {
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

        val releasesArrayText = block.substring(arrStart + 1, arrEnd)

        // Split into individual release objects by brace depth. Release objects here never
        // contain nested `{}` (only `[]` arrays for dates/changelog), so depth-0 boundaries
        // reliably delimit each object.
        val objects = mutableListOf<String>()
        var d = 0
        var objStart = -1
        var j = 0
        while (j < releasesArrayText.length) {
            when (releasesArrayText[j]) {
                '{' -> {
                    if (d == 0) objStart = j
                    d++
                }
                '}' -> {
                    d--
                    if (d == 0 && objStart != -1) {
                        objects.add(releasesArrayText.substring(objStart, j + 1))
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

    private fun extractStringField(obj: String, key: String): String? {
        val regex = Regex("$key:\\s*'((?:[^'\\\\]|\\\\.)*)'")
        val m = regex.find(obj) ?: return null
        return unescape(m.groupValues[1])
    }

    private fun extractStringArrayField(obj: String, key: String): List<String> {
        val startIdx = obj.indexOf("$key:")
        if (startIdx == -1) return emptyList()
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
        val itemRegex = Regex("'((?:[^'\\\\]|\\\\.)*)'")
        return itemRegex.findAll(arrText).map { unescape(it.groupValues[1]) }.toList()
    }

    private fun unescape(s: String): String =
        s.replace("\\'", "'")
        .replace("\\\"", "\"")
        .replace("\\n", "\n")
        .replace("\\\\", "\\")
}
