package com.hackeros.app.data.docs

/**
 * Maps links found in documentation content to the key of a native documentation page (see
 * [DocRemote]), or null when the link has no in-app equivalent (a genuinely external site).
 *
 * Handles both the app's own `hackeros://doc/<key>` scheme (written by [DocHtmlConverter]) and the
 * plain website URLs that appear in the remotely fetched documentation text
 * (the tools-docs/hpm.html page, the h-sharp/docs.html page, and so on).
 */
object DocLinks {
    private const val SCHEME = "hackeros://doc/"
    private val TOOL_PAGE = Regex("""/tools-docs/([A-Za-z0-9_\-]+)\.html""")

    /** Display names for well-known keys (mirrors the website's tools navigation). */
    private val TITLES = mapOf(
        "gaming-cli" to "gaming-cli", "hpm" to "HPM", "hackeros-containers" to "HackerOS Containers",
        "hackeros-builder" to "HackerOS Builder", "hammer" to "hammer", "hacker" to "hacker", "hk" to ".hk",
        "hsh" to "hsh", "hedit" to "hedit", "hdev" to "hdev", "ngt" to "ngt",
        "penetration-mode" to "Penetration Mode", "cybersecurity-mode" to "Cybersecurity Mode",
        "hacker-term" to "Hacker Term", "hackerdeck" to "HackerDeck", "ghostfs" to "GhostFS",
        "hexai" to "HexAI", "hnm" to "HNM", "hacker-mode" to "Hacker Mode", "isolator" to "Isolator",
        "hackeros-steam" to "HackerOS Steam", "hackeros-games" to "HackerOS Games",
        "hackeros-kernel" to "HackerOS Kernel", "hacker-launcher" to "Hacker Launcher",
        "hackeros-store" to "HackerOS Store", "blue-environment" to "Blue Environment",
        "hbuild" to "hbuild", "chker" to "chker", "deb-ostree" to "deb-ostree", "hup" to "hup",
        "hwde" to "HWDE", "h-sharp" to "H#", "hacker-lang" to "Hacker Lang", "hackerscript" to "HackerScript",
        "download" to "Download", "tools-index" to "HackerOS", "system-updates" to "System updates"
    )

    fun scheme(key: String) = SCHEME + key

    fun titleFor(key: String): String = TITLES[key] ?: key

    fun resolve(url: String): String? {
        val u = url.trim()
        if (u.startsWith(SCHEME)) return u.removePrefix(SCHEME).substringBefore('#').substringBefore('?').ifBlank { null }
        val lower = u.lowercase()
        if (lower.contains("legendaryos-linux-system.github.io")) return "blue-environment"
        if (!lower.contains("hackeros-linux-system.github.io/hackeros-website/")) return null
        return when {
            lower.contains("/hacker-lang/docs.html") -> "hacker-lang"
            lower.contains("/h-sharp/docs.html") -> "h-sharp"
            lower.contains("/tools-docs/hackerscript/docs.html") -> "hackerscript"
            lower.contains("/hackeros-games/docs.html") -> "hackeros-games"
            lower.contains("/tools-docs/index.html") -> "tools-index"
            lower.contains("/tools-docs/blue-environment/") -> "blue-environment"
            lower.contains("/tools-docs/hwde/") -> "hwde"
            lower.contains("/system-updates/") -> "system-updates"
            lower.contains("/download.html") -> "download"
            else -> TOOL_PAGE.find(lower)?.groupValues?.get(1)
        }
    }
}
