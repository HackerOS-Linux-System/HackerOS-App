package com.hackeros.app.data.docs

import org.json.JSONObject
import com.hackeros.app.data.docs.DocBlock.*

/**
 * Ports of `doc-engine.js`'s per-tab rendering logic (`buildPane`'s switch statement) to native
 * Kotlin, reading the same JSON fields in the same order. See JsLenientJson for how the raw
 * (partially non-strict) source text is turned into a JSONObject in the first place.
 */
object DocContentParser {

    val TAB_KEYS = listOf(
        "introduction", "hardware", "installation", "firstSteps",
        "environment", "configuration", "troubleshooting", "license",
        "tools", "programming", "editions", "gaming", "gallery"
    )

    data class LangMeta(
        val pageTitle: String,
        val searchPlaceholder: String,
        val tabLabels: List<String>,
        val content: JSONObject?
    )

    /** Extracts `window.HACKEROS_TRANS_DOCS.<lang> = { ... };` as a parsed JSONObject, or null. */
    fun extractLangMeta(fullJs: String, langCode: String): LangMeta? {
        val assignRegex = Regex(
            "HACKEROS_TRANS_DOCS(?:\\[['\"]$langCode['\"]\\]|\\.$langCode)\\s*=\\s*"
        )
        val match = assignRegex.find(fullJs) ?: return null
        val braceStart = fullJs.indexOf('{', match.range.last)
        if (braceStart == -1) return null

        var depth = 0
        var i = braceStart
        var inStr: Char? = null
        var esc = false
        var end = -1
        while (i < fullJs.length) {
            val ch = fullJs[i]
            if (inStr != null) {
                when {
                    esc -> esc = false
                    ch == '\\' -> esc = true
                    ch == inStr -> inStr = null
                }
            } else {
                when (ch) {
                    '\'', '"' -> inStr = ch
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) { end = i; break }
                    }
                }
            }
            i++
        }
        if (end == -1) return null

        val rawBlock = fullJs.substring(braceStart, end + 1)
        val json = try {
            JSONObject(JsLenientJson.convert(rawBlock))
        } catch (e: Exception) {
            return null
        }

        val tabs = json.optJSONArray("tabs")
        val tabLabels = tabs?.let { arr -> (0 until arr.length()).map { arr.optString(it) } } ?: emptyList()

        return LangMeta(
            pageTitle = json.optString("pageTitle"),
            searchPlaceholder = json.optString("searchPlaceholder"),
            tabLabels = tabLabels,
            content = json.optJSONObject("content").takeIf { json.isNull("content").not() && it != null }
        )
    }

    /** Builds the full, ready-to-render documentation page for [langCode]. */
    fun buildPage(fullJs: String, langCode: String): DocPage? {
        val meta = extractLangMeta(fullJs, langCode) ?: return null
        val enMeta = if (langCode != "en") extractLangMeta(fullJs, "en") else meta
        val isFallback = meta.content == null
        val contentSrc = meta.content ?: enMeta?.content
        val labels = meta.tabLabels.ifEmpty { enMeta?.tabLabels ?: TAB_KEYS }
        val isEnglish = langCode == "en" || isFallback

        val tabs = TAB_KEYS.mapIndexed { index, key ->
            val label = labels.getOrElse(index) { key }
            val blocks = when (key) {
                "tools" -> buildToolsBlocks(isEnglish)
                "programming" -> buildProgrammingBlocks(isEnglish)
                "editions" -> buildEditionsBlocks(contentSrc?.optJSONObject("editions"))
                "gallery" -> buildGalleryBlocks(isEnglish)
                else -> buildStandardBlocks(key, contentSrc?.optJSONObject(key))
            }
            DocTab(key, label, blocks)
        }
        return DocPage(tabs, isEnglishFallback = isFallback)
    }

    // --- Generic helpers -------------------------------------------------------------------

    private fun strList(c: JSONObject?, key: String): List<String> {
        val arr = c?.optJSONArray(key) ?: return emptyList()
        return (0 until arr.length()).map { arr.optString(it) }
    }

    private fun str(c: JSONObject?, key: String): String = c?.optString(key) ?: ""

    // --- Per-tab builders (mirrors buildPane's switch cases 1:1) ---------------------------

    private fun buildStandardBlocks(key: String, c: JSONObject?): List<DocBlock> {
        if (c == null) return listOf(Paragraph("[Content not yet translated - showing English fallback]"))
        val blocks = mutableListOf<DocBlock>()
        when (key) {
            "introduction" -> {
                blocks += Heading2(str(c, "h2"))
                blocks += Paragraph(str(c, "p1"))
                blocks += BulletList(strList(c, "list1"))
                blocks += Paragraph(str(c, "p2"))
                blocks += Heading3(str(c, "hPhilosophy"))
                blocks += Paragraph(str(c, "pPhilosophy"))
                blocks += BulletList(strList(c, "listPhilosophy"))
                blocks += Heading3(str(c, "hGoal"))
                blocks += Paragraph(str(c, "pGoal"))
                blocks += BulletList(strList(c, "listGoal"))
            }
            "hardware" -> {
                blocks += Heading2(str(c, "h2"))
                blocks += Heading3(str(c, "hMin"))
                blocks += BulletList(strList(c, "listMin"))
                blocks += Paragraph(str(c, "pMin"))
                blocks += Heading3(str(c, "hRec"))
                blocks += BulletList(strList(c, "listRec"))
                blocks += Heading3(str(c, "hArch"))
                blocks += BulletList(strList(c, "listArch"))
            }
            "installation" -> {
                blocks += Heading2(str(c, "h2"))
                blocks += Heading3(str(c, "hDownload"))
                blocks += Paragraph(str(c, "pDownload"))
                blocks += LinkLine(
                    "https://hackeros-linux-system.github.io/HackerOS-Website/download.html",
                    "https://hackeros-linux-system.github.io/HackerOS-Website/download.html"
                )
                blocks += Heading3(str(c, "hBootable"))
                blocks += BulletList(strList(c, "listBootable"))
                blocks += Command("sudo dd if=HackerOS.iso of=/dev/sdX bs=4M status=progress oflag=sync")
                blocks += Heading3(str(c, "hLive"))
                blocks += Paragraph(str(c, "pLive1"))
                blocks += Paragraph(str(c, "pLive2"))
                blocks += Heading3(str(c, "hInstall"))
                blocks += NumberedList(strList(c, "listInstall"))
                blocks += Heading3(str(c, "hDual"))
                blocks += BulletList(strList(c, "listDual"))
            }
            "firstSteps" -> {
                blocks += Heading2(str(c, "h2"))
                blocks += Heading3(str(c, "hLogin"))
                blocks += BulletList(strList(c, "listLogin"))
                blocks += Heading3(str(c, "hPkgMgr"))
                blocks += Paragraph(str(c, "pPkgMgr"))
                blocks += Heading4("APT (Debian)")
                blocks += Command("sudo apt update\nsudo apt upgrade\nsudo apt install <package>\nsudo apt remove <package>\nsudo apt autoremove\nsudo apt autoclean")
                blocks += Heading4("Flatpak")
                blocks += Command("flatpak install <package>\nflatpak update\nflatpak remove <package>\nflatpak search <name>")
                blocks += Heading4("Snap")
                blocks += Command("snap install <package>\nsnap refresh\nsnap remove <package>\nsnap find <name>")
                blocks += Heading4("Brew (Homebrew)")
                blocks += Command("brew install <package>\nbrew upgrade\nbrew uninstall <package>\nbrew update")
                blocks += Heading4("HackerOS Package Manager (hpm)")
                blocks += LinkLine("hpm - documentation", "https://hackeros-linux-system.github.io/HackerOS-Website/tools-docs/hpm.html")
                blocks += Heading4("HackerOS Nix Manager (hnm)")
                blocks += LinkLine("hnm - documentation", "https://hackeros-linux-system.github.io/HackerOS-Website/tools-docs/hnm.html")
                blocks += Heading3(str(c, "hNetwork"))
                blocks += Command("hacker network")
                blocks += Paragraph(str(c, "pNetwork"))
                blocks += Heading3(str(c, "hKernels"))
                blocks += Paragraph(str(c, "pKernels"))
                blocks += Command("sudo chker xanmod\n# or / lub:\nsudo chker liquorix")
                blocks += Paragraph(str(c, "pKernelsDiff"))
            }
            "environment" -> {
                blocks += Heading2(str(c, "h2"))
                blocks += Heading3(str(c, "hDefault"))
                blocks += Paragraph(str(c, "pDefault"))
                blocks += Heading3(str(c, "hPreinstalled"))
                blocks += BulletList(strList(c, "listPreinstalled"))
                blocks += Heading3(str(c, "hInstallSoft"))
                blocks += BulletList(strList(c, "listInstallSoft"))
            }
            "configuration" -> {
                blocks += Heading2(str(c, "h2"))
                blocks += Paragraph(str(c, "p1"))
                blocks += BulletList(strList(c, "list1"))
            }
            "troubleshooting" -> {
                blocks += Heading2(str(c, "h2"))
                blocks += BulletList(strList(c, "list1"))
            }
            "license" -> {
                blocks += Heading2(str(c, "h2"))
                blocks += BulletList(strList(c, "list1"))
            }
            "gaming" -> {
                blocks += Heading2(str(c, "h2"))
                blocks += Paragraph(str(c, "p1"))
                blocks += Paragraph(str(c, "p2"))
                if (c.has("hContrib")) blocks += Heading3(str(c, "hContrib"))
                if (c.has("p3")) blocks += Paragraph(str(c, "p3"))
            }
        }
        return blocks
    }

    private data class EditionDef(val name: String, val key: String, val image: String?, val docsUrl: String?)

    private val EDITION_DEFS = listOf(
        EditionDef("Official", "official", "official-edition.png", null),
        EditionDef("Hydra", "hydra", "hydra-edition.png", null),
        EditionDef("GNOME", "gnome", "gnome-edition.png", null),
        EditionDef("XFCE", "xfce", "xfce-edition.png", null),
        EditionDef("Blue", "blue", "blue-edition.png", "https://legendaryos-linux-system.github.io/website/"),
        EditionDef("HWDE", "hwde", "hwde-edition.png", "https://hackeros-linux-system.github.io/HackerOS-Website/tools-docs/HWDE/docs.html"),
        EditionDef("Gaming", "gaming", "gaming-edition.png", null),
        EditionDef("Cybersecurity", "cybersec", null, null),
        EditionDef("Cybersecurity Default", "cybersecdefault", "cybersecurity-default-edition.png", null),
        EditionDef("LTS", "lts", null, null),
        EditionDef("Atomic", "atomic", null, null),
        EditionDef("NVIDIA", "nvidia", null, null),
    )

    private fun buildEditionsBlocks(c: JSONObject?): List<DocBlock> {
        val blocks = mutableListOf<DocBlock>()
        blocks += Heading2(if (c != null) str(c, "h2") else "Editions")
        EDITION_DEFS.forEach { def ->
            blocks += Heading3("Edition ${def.name}")
            blocks += Paragraph(str(c, def.key))
            if (def.docsUrl != null) blocks += LinkLine("More details", def.docsUrl)
        }
        if (c != null) {
            blocks += Heading3(str(c, "releaseCycleH"))
            blocks += Paragraph(str(c, "releaseCycleP"))
        }
        return blocks
    }

    private fun buildGalleryBlocks(isEnglish: Boolean): List<DocBlock> {
        return listOf(
            Heading2(if (isEnglish) "13. Gallery" else "13. Galeria"),
            Paragraph(
                if (isEnglish) "Browse HackerOS community screenshots in the app's own Gallery tab - it shows the exact same images live from GitHub."
                else "Zdjęcia i zrzuty ekranu społeczności HackerOS znajdziesz w zakładce Galeria tej aplikacji - pokazuje te same obrazy na żywo z GitHub."
            )
        )
    }

    // --- Tools tab (static content in doc-engine.js; PL text shown for every non-EN language) --

    private data class ToolRow(val tool: String, val descPl: String, val descEn: String, val instPl: String, val instEn: String)

    private val TOOL_ROWS = listOf(
        ToolRow("ngt", "Narzędzie inspirowane mc (Midnight Commander), napisane w GoLang.", "File manager inspired by Midnight Commander, written in GoLang.", "wbudowane we wszystkich edycjach", "built-in all editions"),
        ToolRow("hedit", "Edytor tekstu inspirowany nano, napisane w GoLang.", "Text editor inspired by nano, written in GoLang.", "wbudowane we wszystkich edycjach", "built-in all editions"),
        ToolRow("hdev", "TUI edytor kodu.", "TUI code editor.", "wbudowane we wszystkich edycjach", "built-in all editions"),
        ToolRow("hacker", "Główne narzędzie HackerOS: instaluj, usuwaj, napraw, szybka aktualizacja.", "Main HackerOS tool: install, remove, repair, quick update.", "wbudowane we wszystkich edycjach", "built-in all editions"),
        ToolRow("HackerOS-Steam", "Uruchom Steam w izolowanym środowisku (kontener).", "Run Steam in an isolated container.", "wbudowane we wszystkich edycjach", "built-in all editions"),
        ToolRow("hackeros-builder", "Narzędzie do budowania obrazów OCI inspirowane bootc.", "Tool for building OCI images inspired by bootc.", "hacker unpack hackeros-builder", "hacker unpack hackeros-builder"),
        ToolRow("h#", "Własny język programowania H# do ogólnego zastosowania w HackerOS.", "Own programming language H# for general use in HackerOS.", "hacker unpack h#", "hacker unpack h#"),
        ToolRow("bytes", "Manager pakietów dla H#.", "Package manager for H#.", "hacker unpack h#-utils", "hacker unpack h#-utils"),
        ToolRow("hl", "Hacker Lang – następca shella (lub alternatywa).", "Hacker Lang - shell alternative / successor.", "wbudowany w każdej edycji", "built-in all editions"),
        ToolRow("hexai", "AI dla HackerOS – lokalny asystent oparty na modelach językowych.", "AI for HackerOS - local assistant based on language models.", "hacker unpack hexai", "hacker unpack hexai"),
        ToolRow("hammer", "Atomowy manager pakietów oraz następca apt, używany w edycji Atomic.", "Atomic package manager and successor to apt, used in the Atomic edition.", "Wbudowany w edycji Atomic", "Built-in - Atomic edition"),
        ToolRow("deb-ostree", "Atomowy manager pakietów z systemem ostree, używany w edycji Cybersecurity.", "Atomic package manager with ostree system, used in the Cybersecurity edition.", "Wbudowany w edycji Cybersecurity", "Built-in - Cybersecurity edition"),
        ToolRow("anvil", "Narzędzie do zarządzania systemem readonly (dla edycji Atomic).", "Read-only system management tool (Atomic edition only).", "Tylko edycja Atomic", "Atomic edition only"),
        ToolRow("isolator", "Manager pakietów / nakładka dla distrobox.", "Package manager overlay for distrobox/podman.", "Wbudowane w Atomic; inne: hacker unpack isolator", "Atomic built-in; others: hacker unpack isolator"),
        ToolRow("Hacker-Mode", "Sesja inspirowana gamescope / Steam (tryb gry na pełnym ekranie).", "Gaming session inspired by gamescope / Steam.", "hacker unpack hacker-mode", "hacker unpack hacker-mode"),
        ToolRow("bph", "Narzędzie CLI edukacyjne do testów penetracyjnych.", "CLI educational penetration testing tool.", "Tylko edycja Cybersecurity", "Cybersecurity edition only"),
        ToolRow("Hacker-Term", "Własny terminal HackerOS (z dodatkowymi funkcjami).", "Custom HackerOS terminal with additional features.", "wbudowany w każdej edycji", "built-in all editions"),
        ToolRow("HackerOS-App", "Aplikacja mobilna dla telefonów Android.", "Mobile application for Android phones.", "github.com/HackerOS-Linux-System/HackerOS-App/releases", "github.com/HackerOS-Linux-System/HackerOS-App/releases"),
        ToolRow("hsh", "Własna powłoka HackerOS (zastępuje bash/zsh).", "Custom HackerOS shell (replaces bash/zsh).", "wbudowana w każdej edycji", "built-in all editions"),
        ToolRow("hpm", "Manager pakietów z repozytorium community.", "Community package manager repository.", "wbudowany w każdej edycji", "built-in all editions"),
        ToolRow("hnm", "Nakładka dla Nix – integracja z repozytorium Nixpkgs.", "Nix overlay - integration with Nixpkgs repository.", "wbudowany we wszystkich edycjach", "built-in all editions"),
        ToolRow("Hacker Launcher", "Aplikacja do uruchamiania gier Windowsowych (Proton).", "Application for running Windows games via Proton.", "wbudowane we wszystkich edycjach", "built-in all editions"),
        ToolRow("HackerOS-Games", "Aplikacja do uruchamiania gier od HackerOS: StarBlaster, Bit Jump, The Racer, Bark Squadron.", "App to launch HackerOS games: StarBlaster, Bit Jump, The Racer, Bark Squadron.", "wbudowane we wszystkich edycjach", "built-in all editions"),
        ToolRow("getit", "Połączenie git + wget + własnego systemu pobierania katalogów z GitHub/GitLab.", "git + wget + custom GitHub/GitLab directory downloader.", "wbudowane we wszystkich edycjach", "built-in all editions"),
        ToolRow("chker", "Narzędzie CLI do zmiany jądra systemowego (Debian → XanMod lub Liquorix).", "CLI tool to change the system kernel (Debian to XanMod or Liquorix).", "wbudowane we wszystkich edycjach", "built-in all editions"),
        ToolRow("Cybersecurity Mode", "Sesja/aplikacja nakładka dla narzędzi cyberbezpieczeństwa (działa w kontenerze).", "Session/app overlay for cybersecurity tools (runs in container).", "Tylko edycja Cybersecurity", "Cybersecurity edition only"),
        ToolRow("Penetration Mode", "Aplikacja z własnymi narzędziami do testów penetracyjnych (tylko do celów edukacyjnych).", "App with custom penetration testing tools (educational only).", "Tylko edycja Cybersecurity", "Cybersecurity edition only"),
        ToolRow("HackerOS-Store", "Sklep HackerOS z programami i dodatkami.", "HackerOS store with programs and add-ons.", "wbudowany we wszystkich edycjach", "built-in all editions"),
        ToolRow("HackerOS-Containers", "Własny system kontenerów – lekka, zintegrowana platforma izolowanych środowisk.", "Custom container system - lightweight isolated environments.", "hacker unpack hackeros-containers", "hacker unpack hackeros-containers"),
        ToolRow("HackerOS-Game-Mode", "Nakładka optymalizująca system pod kątem grania, wyświetla FPS.", "System overlay optimizing for gaming, shows FPS.", "hacker unpack hackeros-game-mode", "hacker unpack hackeros-game-mode"),
        ToolRow(".hk", "Format konfiguracyjny stosowany głównie w HackerOS.", "Configuration format used mainly in HackerOS.", "H#/Hacker Lang/biblioteki Rust", "H#/Hacker Lang/Rust libs"),
        ToolRow("a", "Proste narzędzie CLI do prostych aktualizacji systemu napisane w Hacker Lang.", "Simple CLI tool for quick system updates, written in Hacker Lang.", "wbudowane we wszystkich edycjach", "built-in all editions"),
        ToolRow("GhostFS", "Autorski system plików (Beta) – trwają prace.", "Custom file system (Beta) - work in progress.", "Niedostępne (w trakcie prac)", "Not available yet"),
        ToolRow("HackerOS Cockpit", "Panel sterowania systemu w przeglądarce.", "System control panel in the browser.", "wbudowane we wszystkich edycjach", "built-in all editions"),
        ToolRow("HackerScript", "Eksperymentalny, hobbystyczny język programowania transpilowany do Rust i Python.", "Experimental, hobby programming language transpiled to Rust and Python.", "hacker unpack hackerscript", "hacker unpack hackerscript"),
        ToolRow("gaming-cli / gaming / gamescope-manager", "Zestaw narzędzi CLI dostępny wyłącznie w edycji HackerOS Gaming Edition.", "Suite of CLI tools exclusive to HackerOS Gaming Edition.", "Wbudowane w edycji Gaming", "Built-in - Gaming Edition only"),
    )

    private fun buildToolsBlocks(isEnglish: Boolean): List<DocBlock> {
        val rows = TOOL_ROWS.map {
            Triple(it.tool, if (isEnglish) it.descEn else it.descPl, if (isEnglish) it.instEn else it.instPl)
        }
        return listOf(
            Heading2(if (isEnglish) "9. Tools and Applications" else "9. Narzędzia i aplikacje"),
            Paragraph(if (isEnglish) "Full list of HackerOS custom tools and applications:" else "Pełna lista autorskich narzędzi i aplikacji HackerOS:"),
            ToolsTable(rows),
            LinkLine(
                if (isEnglish) "Full advanced tools documentation" else "Pełna dokumentacja zaawansowanych narzędzi",
                "https://hackeros-linux-system.github.io/HackerOS-Website/tools-docs/index.html"
            )
        )
    }

    // --- Programming tab (static content) ---------------------------------------------------

    private val HACKERSCRIPT_SAMPLE = """
        using <1.2>

        !! Returns the sum of two integers.
        fun add(a: Int, b: Int) -> Int [
            end a + b
        ]

        fun fib(n: Int) -> Int [
            if n <= 1 [
                end n
            ]
            end fib(n - 1) + fib(n - 2)
        ]

        fun main() [
            let name = "HackerScript"
            log("Hello,", name, "!")

            let suma = add(2, 3)
            log("2 + 3 =", suma)

            let i = 0
            while i < 5 [
                log("fib(", i, ") =", fib(i))
                i = i + 1
            ]

            for jezyk in ["Python", "Rust", "HackerScript"] [
                if jezyk == "HackerScript" [
                    log(jezyk, "- to my!")
                ] else [
                    log(jezyk, "- inspiracja")
                ]
            ]
            end
        ]
    """.trimIndent()

    private fun buildProgrammingBlocks(isEnglish: Boolean): List<DocBlock> {
        val blocks = mutableListOf<DocBlock>()
        blocks += Heading2(if (isEnglish) "10. Programming Languages" else "10. Języki programowania")

        blocks += Heading3("Hacker Lang")
        blocks += Paragraph(
            if (isEnglish) "An efficient alternative to the shell with a unique syntax and its own shell environment."
            else "Jest to wydajna alternatywa dla shella z wyjątkową składnią. Hacker Lang ma zarówno własną unikalną składnię, jak i własną powłokę."
        )
        blocks += Command("> hacker update")
        blocks += LinkLine(
            if (isEnglish) "Official Hacker Lang documentation" else "Oficjalna dokumentacja Hacker Lang",
            "https://hackeros-linux-system.github.io/HackerOS-Website/hacker-lang/docs.html"
        )

        blocks += Heading3("H#")
        blocks += Paragraph(
            if (isEnglish) "HackerOS has its own fully integrated programming language called <strong>H#</strong>. Its main goal is use in HackerOS tools and the cybersecurity ecosystem."
            else "HackerOS posiada własny, w pełni zintegrowany z systemem język programowania o nazwie <strong>H#</strong>. Jego głównym celem jest zastosowanie w ogólnych narzędziach HackerOS oraz ekosystemie cybersecurity."
        )
        blocks += BulletList(
            if (isEnglish) listOf(
                "<strong>Compiled</strong> - compiles H# programs to native binary code.",
                "<strong>Interpreted (efficient - JIT)</strong> - efficient JIT execution.",
                "<strong>Interpreted (preview)</strong> - quick results preview."
            ) else listOf(
                "<strong>Kompilowany</strong> – kompilacja do natywnego kodu binarnego.",
                "<strong>Interpretowany (wydajny - JIT)</strong> – wydajne uruchomienie JIT.",
                "<strong>Interpretowany (podgląd)</strong> – szybki podgląd efektów."
            )
        )
        blocks += LinkLine(
            if (isEnglish) "Official H# documentation" else "Oficjalna dokumentacja H#",
            "https://hackeros-linux-system.github.io/HackerOS-Website/h-sharp/docs.html"
        )

        blocks += Heading3("HackerScript")
        blocks += Paragraph(
            if (isEnglish) "HackerScript is an experimental, hobby programming language (not only for HackerOS), transpiled to Rust and Python in a single file, with support for a wide range of ecosystems."
            else "HackerScript to eksperymentalny, hobbystyczny język programowania (nie tylko dla HackerOS), transpilowany do Rust i Python w jednym pliku, obsługujący masę ekosystemów."
        )
        blocks += CodeSample(HACKERSCRIPT_SAMPLE)
        blocks += LinkLine(
            if (isEnglish) "Official HackerScript documentation" else "Oficjalna dokumentacja HackerScript",
            "https://hackeros-linux-system.github.io/HackerOS-Website/tools-docs/HackerScript/docs.html"
        )
        return blocks
    }
}
