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
            "https://hackeros-linux-system.github.io/HackerOS-Website/hacker-lang/docs.html",
            nativeDetailKey = "hacker-lang"
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
            "https://hackeros-linux-system.github.io/HackerOS-Website/h-sharp/docs.html",
            nativeDetailKey = "h-sharp"
        )

        blocks += Heading3("HackerScript")
        blocks += Paragraph(
            if (isEnglish) "HackerScript is an experimental, hobby programming language (not only for HackerOS), transpiled to Rust and Python in a single file, with support for a wide range of ecosystems."
            else "HackerScript to eksperymentalny, hobbystyczny język programowania (nie tylko dla HackerOS), transpilowany do Rust i Python w jednym pliku, obsługujący masę ekosystemów."
        )
        blocks += CodeSample(HACKERSCRIPT_SAMPLE)
        blocks += LinkLine(
            if (isEnglish) "Official HackerScript documentation" else "Oficjalna dokumentacja HackerScript",
            "https://hackeros-linux-system.github.io/HackerOS-Website/tools-docs/HackerScript/docs.html",
            nativeDetailKey = "hackerscript"
        )
        return blocks
    }

    // --- Native "documentation link" detail pages (v0.7) -------------------------------------
    //
    // Previously, the three LinkLine entries above opened the target page in the external
    // browser - tapping them "teleported" the user out of the app, and (for HackerScript
    // specifically) the target page on the website has no content at all, so it just opened an
    // empty tab. As of v0.7 every one of these links opens a native, in-app detail page instead
    // (see DocumentationScreen's local `openDetailKey` state) - nothing ever leaves the app and
    // no WebView is involved anywhere. Hacker Lang's content below is a straight, structural
    // port of the website's own static hacker-lang/docs.html (grouped the same way its sidebar
    // groups it); H#'s is ported from its real intro/install sections plus the real table of
    // contents for the rest (that site is a much larger JS-driven single-page app that isn't
    // practical to fully mirror natively); HackerScript's page on the website currently has no
    // content published at all, so its native page instead expands on what this app already
    // knows about the language.
    fun detailTabsFor(key: String): List<DocTab>? = when (key) {
        "hacker-lang" -> buildHackerLangDetailTabs()
        "h-sharp" -> buildHSharpDetailTabs()
        "hackerscript" -> buildHackerScriptDetailTabs()
        else -> null
    }

    fun detailTitleFor(key: String): String = when (key) {
        "hacker-lang" -> "Hacker Lang"
        "h-sharp" -> "H#"
        "hackerscript" -> "HackerScript"
        else -> ""
    }

    // --- Native Hacker Lang reference (extracted from the website's own static docs.html; ---
    // --- content is Polish-only at the source, same as the website itself) -------------------
    private fun buildHackerLangDetailTabs(): List<DocTab> = listOf(
        DocTab(
            key = "hl-wstep",
            label = "Wstęp",
            blocks = listOf(
                Heading3("Czym jest Hacker Lang?"),
                Paragraph("Hacker Lang (<strong>HL</strong>) to interpretowany język programowania napisany w Rust, natywny język skryptowy <strong>HackerOS</strong>. Pliki źródłowe mają rozszerzenie <code>.hl</code>, skompilowany bytecode — <code>.bc</code>."),
                Paragraph("✨ <strong>Gen 2 (domyślny):</strong> typowane zmienne <code>%: typ</code>, arytmetyka <code>\$()</code>, pipe do zmiennej <code>|></code>, for-in <code>@ item in</code>, while <code>?~</code>, switch <code>? switch</code>, HackerOS API <code>||</code>, goroutines z nazwą <code>:* nazwa def</code>, manager pakietów <strong>bit</strong> z progress barem."),
                Heading3("Dostępność"),
                Paragraph("🔒 <strong>Hacker Lang jest wbudowany w HackerOS.</strong> Binarka <code>hl</code> i manager <code>bit</code> są częścią systemu."),
                CodeSample("hl version\nbit"),
                Heading3("Szybki start"),
                CodeSample("#!/usr/bin/env hl\n/// Mój pierwszy skrypt\nusing <gen 2>\n\n// curl\n# <main/colors>\n\n% name: str = HackerOS\n% count: int = 42\n\n\$( @count * 2 ) -> @doubled\n\n~> Witaj w @name! count=@count doubled=@doubled\n\n> uname -r |> @kernel\n~> Kernel: @kernel\n\n@ tool in curl git python3\n    ::which @tool\n    ? ok\n        ::green ✓ @tool\n    done\ndone"),
                CodeSample("hl hello.hl              # uruchom\nhl compile hello.hl      # → hello.bc (bytecode)\nhl compile hello.bc      # → binarka ELF\n./hello.bc               # uruchom .bc bezpośrednio\nhl repl                  # REPL interaktywny"),
            )
        ),
        DocTab(
            key = "hl-meta",
            label = "Meta",
            blocks = listOf(
                Heading3("Shebang"),
                CodeSample("#!/usr/bin/env hl          # .hl — zalecana forma\n#!/usr/bin/hl              # bezpośrednia ścieżka\n#!/usr/bin/env -S /usr/bin/hl run  # .bc — auto-dodawany"),
                Heading3("System Genów"),
                Paragraph("Geny to odpowiednik Rust editions. <strong>Gen 2 jest domyślny</strong> — pliki bez <code>using</code> dostają gen 2."),
                CodeSample("using <gen 2>    # domyślny\nusing <gen 1>    # kompatybilność wsteczna"),
                BulletList(listOf("<code>gen 1</code> — aktywny — & *> _N ＜＜ :* :** *-- — podstawowa składnia", "<code>gen 2</code> — domyślny — %: typ \$() |> @ in ?~ ? switch || — typowanie, pętle, switch, HackerOS API", "<code>gen 3</code> — plan — Domknięcia (planowane)")),
            )
        ),
        DocTab(
            key = "hl-gen1",
            label = "Składnia gen 1",
            blocks = listOf(
                Heading3("Print —~>"),
                Paragraph("🚫 <strong>echo jest zabronione.</strong> <code>> echo tekst</code> generuje błąd. Użyj <code>~> tekst</code>."),
                CodeSample("~> Zwykły tekst\n~> Zmienna: @name\n~> Wynik: @count doubled=@doubled"),
                Heading3("Quick-funkcje —::"),
                BulletList(listOf("<code>::upper/lower</code> — zmiana wielkości", "<code>::len/trim/rev</code> — operacje na str", "<code>::replace/split</code> — transformacje", "<code>::contains/startswith/endswith</code> — sprawdzanie", "<code>::abs/ceil/floor/round</code> — matematyka", "<code>::max/min/rand</code> — matematyka", "<code>::env/date/time/pid</code> — system", "<code>::which/exists/isdir/isfile</code> — pliki", "<code>::basename/dirname/read</code> — ścieżki", "<code>::set/get/type/unset</code> — zmienne", "<code>::nl/hr/bold</code> — formatowanie", "<code>::red/green/yellow/cyan</code> — kolory")),
                Heading3("Komendy —> ^> ->"),
                BulletList(listOf("<code>> komenda</code> — Zwykłe wykonanie.", "<code>^> komenda</code> — Sudo.", "<code>-> komenda</code> — Izolacja namespace.", "<code>^-> komenda</code> — Sudo + izolacja.", "<code>>> komenda @var</code> — Z interpolacją zmiennych.", "<code>^>> komenda</code> — Interpolacja + sudo.")),
                Heading3("Builtin coreutils —/>"),
                Paragraph("Operator <code>/></code> wywołuje wbudowaną, natywną implementację popularnych narzędzi coreutils — bez spawnowania zewnętrznego procesu. Szybsze niż <code>></code> i działa nawet tam, gdzie danego binarnego narzędzia brakuje w systemie."),
                CodeSample("/> cat plik.txt\n/> ls -la /tmp\n/> grep TODO src/main.rs\n/> wc -l plik.txt |> @liczba_linii"),
                BulletList(listOf("<code>cat / ls / grep</code> — odczyt / listowanie / szukanie", "<code>head / tail / wc</code> — fragmenty / liczenie", "<code>find / sort / uniq</code> — wyszukiwanie / sortowanie", "<code>cut / tr / rev</code> — przetwarzanie tekstu", "<code>cp / mv / rm</code> — operacje na plikach", "<code>mkdir / touch / stat</code> — tworzenie / metadane", "<code>chmod / du / pwd</code> — uprawnienia / rozmiar / cwd", "<code>which / env / date</code> — system", "<code>basename / dirname</code> — ścieżki")),
                Paragraph("📝 <strong>Przechwytywanie wyniku</strong> działa tak samo jak przy zwykłych komendach: <code>/> komenda |> @zmienna</code>."),
                Heading3("Tło —&"),
                CodeSample("& python3 -m http.server 8080\n& redis-server --port 6379\n~> Serwer PID: @_bg_pid\n\n;; Równoległy download\n& wget -q https://example.com/a.zip\n& wget -q https://example.com/b.zip"),
                Heading3("Hsh —*>"),
                Paragraph("Operator <code>*></code> uruchamia komendę przez <code>hsh -c</code> — autorską powłokę HackerOS. <code>echo</code> jest dozwolone wewnątrz <code>*></code>."),
                CodeSample("*> uname -a\n*> notify-send \"Gotowe\"\n;; Brak interpolacji @zmiennych — użyj >> dla zmiennych"),
                Heading3("Pętla _N"),
                CodeSample("_10 > hacker update\n_5  ~> powtorzenie!\n_3  ::green OK"),
                Heading3("Goroutines —:*"),
                CodeSample(":** wyniki          ;; zadeklaruj kanał\n\n:* scanner def      ;; goroutine z nazwą (gen 2)\n    >> nmap -sn 192.168.1.0/24\n    *-- wyniki\ndone\n\n:*                  ;; anonimowa (gen 1)\n    > jakies_zadanie\ndone\n\n*-- wyniki           ;; odbierz z kanału"),
                Heading3("Import pliku —<<"),
                CodeSample("<< utils.hl\n<< config.hl | produkcja   ;; detal → @_import_detail"),
            )
        ),
        DocTab(
            key = "hl-gen2",
            label = "Składnia gen 2",
            blocks = listOf(
                Heading3("Typowane zmienne —%: typ(gen 2)"),
                CodeSample("% count: int   = 42\n% score: float = 3.14\n% label: str   = hello world\n% active: bool = true\n% x: int       = \$( @count * 2 )   ;; arytmetyka jako wartość\n% name         = bez_typu           ;; gen 1 — nadal działa"),
                Heading3("Arytmetyka —\$( )(gen 2)"),
                CodeSample("\$( 2 + 2 )              -> @res\n\$( 10 * @count )        -> @mul\n\$( @a + @b )            -> @sum\n\$( (2 + 3) * 4 )        -> @paren\n\$( 100 / @cores )       -> @per_core\n% threads: int = \$( @cores * 2 )"),
                Paragraph("💡 Backend: <code>sh -c 'echo \$(( expr ))'</code>. Obsługuje wszystkie operatory shell: <code>+ - * / % ** ＜＜ >></code>."),
                Heading3("Pipe do zmiennej —|>(gen 2)"),
                CodeSample("> hostname           |> @host\n> uname -r           |> @kernel\n> date +%Y-%m-%d     |> @today\n> id -un             |> @user\n> nproc              |> @cores\n^> id -u              |> @uid   ;; sudo pipe\n~> Host: @host | Kernel: @kernel"),
                Heading3("For-in —@ item in(gen 2)"),
                CodeSample("@ tool in curl git python3\n    ::which @tool\n    ? ok\n        ::green ✓ @tool\n    done\ndone\n\n;; Ze zmiennej (ważne: ścieżki z / muszą być w zmiennej)\n% dirs = \"/tmp /etc /usr\"\n@ d in @dirs\n    ~> @d\ndone"),
                Paragraph("💡 Iterable to lista oddzielona spacjami. Ścieżki zawierające <code>/</code> należy umieszczać w zmiennej (<code>% dirs = \"/tmp /etc\"</code>), a nie bezpośrednio w <code>@ item in</code>."),
                Heading3("While —?~(gen 2)"),
                CodeSample("% i: int = 0\n\n?~ @i < 10\n    \$( @i + 1 ) -> @i\n    ~> iteracja: @i\ndone\n\n;; Operatory: == != < > <= >=\n?~ @status == running\n    ~> działa...\ndone"),
                Heading3("Switch —? switch(gen 2)"),
                CodeSample("? switch @os\n| linux\n    ::green Linux!\n| windows\n    ::yellow Windows!\n| *\n    ~> Nieznany: @os\ndone\n\n;; Z dynamiczną wartością\n> uname -s |> @detected\n? switch @detected\n| Linux\n    ::green Linux!\n| *\n    ~> @detected\ndone"),
                Heading3("HackerOS API —||(gen 2)"),
                Paragraph("Operator <code>||</code> wywołuje natywne narzędzia HackerOS bezpośrednio."),
                CodeSample("|| hacker update\n|| hpkg install nmap\n|| lpm list\n|| hsh -c \"ls /tmp\"\n|| H# --version\n|| Blue-Environment start\n|| hackeros-steam launch"),
                Paragraph("Dostępne: <code>H# hco hacker hsh hpkg Blue-Environment hnm hpm hedit ngt eiq getit hdev anvil a hbuild lpm chker isolator hackeros-steam ulb gameframe hup hackeros-builder</code>"),
                Heading3("Import katalogu —<*(gen 2)"),
                Paragraph("<code>＜*</code> ładuje CAŁY katalog jako moduł — odpowiednik <code>mod.rs</code> w Rust. Wewnątrz katalogu musi istnieć plik <code>imports.hl</code>, zawierający listę <code>＜＜</code> dla każdego pliku wchodzącego w skład modułu."),
                CodeSample("<* narzedzia         ;; ładuje narzedzia/imports.hl\n<* lib/siec          ;; zagnieżdżona ścieżka też działa\n\n;; narzedzia/imports.hl:\n<< string_utils.hl\n<< math_utils.hl\n<< validators.hl"),
                Heading3("Arena functions —:: nazwa <rozmiar> def(gen 2)"),
                Paragraph("Szybszy wariant funkcji: przed wejściem alokowana jest arena (bump-pointer) o zadanym rozmiarze — wszystkie zmienne lokalne trafiają do niej, a po wyjściu następuje JEDEN dealokacja zamiast osobnego zwalniania każdej zmiennej. Zero presji na GC, zero fragmentacji sterty. Najlepsze dla: przetwarzania stringów, pętli matematycznych, parsowania, transformacji danych."),
                CodeSample(":: przetworz <4k> def\n    % buf: str = @wejscie\n    ~> Przetworzono: @buf\ndone\n\n;; Wywołanie — tak samo jak zwykłej funkcji przez ::\n::przetworz dane_wejsciowe\n\n;; Rozmiar domyślny (4096 B), gdy pominięty\n:: szybka def\n    ~> Bez jawnego rozmiaru — domyślnie 4k\ndone"),
                BulletList(listOf("<code>＜256></code> — 256 B — liczba = bajty wprost", "<code>＜4k></code> — 4096 B — sufiks k = ×1024", "<code>＜1m></code> — 1 048 576 B — sufiks m = ×1024×1024", "<code>(pominięty)</code> — 4096 B — domyślny rozmiar areny")),
                Heading3("Extern runtime —_>(gen 2+)"),
                Paragraph("Uruchamia zewnętrzny plik/binarkę we WŁAŚCIWYM dla niego runtime — shell, Python, Java, bezpośrednia binarka (ELF) lub biblioteka dzielona (<code>.so</code>, przez <code>dlopen</code>). Runtime dobierany jest jawnie w nawiasach kwadratowych albo zgadywany z rozszerzenia pliku."),
                CodeSample("_> deploy.sh [shell] def\n    ~> Uruchamiam deploy...\ndone\n\n_> analiza.py [python] def\n    ~> Analiza danych...\ndone\n\n;; Runtime zgadywany automatycznie z rozszerzenia — [] można pominąć\n_> narzedzie.sh def     ;; .sh/.bash → shell\ndone\n_> /usr/bin/mytool def  ;; brak rozszerzenia → elf\ndone"),
                BulletList(listOf("<code>[shell]</code> — .sh , .bash — bash/sh", "<code>[python]</code> — .py — python3", "<code>[java]</code> — .jar — java -jar", "<code>[elf]</code> — (domyślny) — bezpośrednie wykonanie binarki", "<code>[so]</code> — .so — biblioteka dzielona (dlopen)")),
            )
        ),
        DocTab(
            key = "hl-wspolne",
            label = "Wspólne",
            blocks = listOf(
                Heading3("Zmienne —%i@"),
                BulletList(listOf("<code>@HL_VERSION</code> — gen 2", "<code>@HL_OS</code> — HackerOS/Debian", "<code>@HL_GEN</code> — 2", "<code>@HL_SCRIPT</code> — ścieżka do bieżącego skryptu", "<code>@_bg_pid</code> — PID ostatniego procesu w tle (&)", "<code>@_import_detail</code> — detal z ＜＜ plik.hl | detal", "<code>@argc, @arg0…</code> — argumenty skryptu")),
                Heading3("Export —=>"),
                CodeSample("=> EDITOR  = nvim\n=> GOPATH  = /home/hacker/go\n\n=> PATH [\n| /usr/local/bin\n| /usr/bin\n| /usr/lib/HackerOS\n]"),
                Heading3("Funkcje —:i--"),
                CodeSample(": audit def\n    ~> Skanowanie @target...\n    >> nmap -sV @target\n    ? ok\n        ::green Skan OK.\n    done\ndone\n\n-- audit"),
                Heading3("Warunki —? ok / ? err"),
                CodeSample("> ping -c 1 192.168.1.1\n\n? ok\n    ::green Online ✓\ndone\n\n? err\n    ::red Offline ✗\ndone"),
                Heading3("Komentarze"),
                CodeSample(";; komentarz liniowy\n/// dokumentacyjny (widoczny w hl search)\n// blokowy — wieloliniowy — kończy się \\\\"),
                Heading3("Zależności —//"),
                CodeSample("// nmap\n// curl\n// git"),
                Heading3("Biblioteki —#"),
                CodeSample("# <main/net>           ;; NET_LOCALHOST, NET_MYIP, porty...\n# <main/fs>            ;; FS_HOME, FS_TMP...\n# <main/sys>           ;; SYS_ARCH, SYS_KERNEL, SYS_OS...\n# <main/str>           ;; STR_* stałe + funkcje str_*\n# <main/crypto>        ;; sha256, md5, base64\n# <main/colors>        ;; COLOR_RED, COLOR_GREEN...\n# <main/cli>           ;; CLI_ARGC, CLI_ARG0...\n# <main/progress-bar>  ;; pb_draw, pb_done, pb_label\n# <main/json>          ;; json_get, json_validate...\n# <main/hk-parser>     ;; parser .hk (HackerOS Config)\n# <main/hacker>        ;; parser .hacker v1/v2/v3\n# <bit/hashlib>        ;; bit — .so\n# <github/user/repo>   ;; GitHub\n\n;; Kompatybilność wsteczna (automatycznie normalizowane):\n;; # <std/net> → main/net   # <virus/x> → bit/x   # <community/u/r> → github/u/r"),
                Paragraph("Biblioteki <code>main/</code> to pliki <code>.hl</code> w <code>/usr/lib/HackerOS/Hacker-Lang/main-libs/</code> — nie wbudowane w binkarkę."),
            )
        ),
        DocTab(
            key = "hl-bit",
            label = "Bit (pkg manager)",
            blocks = listOf(
                Heading3("Manager pakietów —bit"),
                CodeSample("bit                     # auto: uruchom/skompiluj projekt\nbit install hashlib     # zainstaluj pakiet\nbit remove  hashlib     # usuń pakiet\nbit list                # lista dostępnych\nbit update              # zaktualizuj listę\nbit info    hashlib     # info o pakiecie\nbit help                # pomoc"),
                CodeSample("[->.................] [1%]\n[---->..............] [10%]\n[---------->........] [50%]\n[-------------------->] [100%]"),
                Heading3("Projekt mode —bit"),
                BulletList(listOf("<code>run.hl + kod</code> — bit→ uruchamia interpretowalnie przezhl run run.hl", "<code>build.hl + kod</code> — bit→ kompiluje dobc/elf/sowgBIT_BUILD_TARGET", "<code>source-code/ + build.hl</code> — bit→ kompiluje cały katalog (rust-like)")),
                CodeSample("using <gen 2>\n\n% BIT_BUILD_TARGET = elf    ;; bc | elf | so\n% BIT_BUILD_INPUT  = main.hl"),
                Paragraph("💡 <code>.cache/</code> tworzony automatycznie i usuwany po zakończeniu — izolowane środowisko."),
            )
        ),
        DocTab(
            key = "hl-narzedzia",
            label = "Narzędzia",
            blocks = listOf(
                Heading3("CLI —hl"),
                CodeSample("hl plik.hl               # uruchom skrypt\nhl run plik.hl           # jawna forma\nhl run plik.bc           # uruchom bytecode\nhl compile plik.hl       # → plik.bc\nhl compile plik.bc       # → binarka ELF\nhl compile --shared plik.bc  # → .so\nhl check plik.hl         # linter + składnia\nhl check --meta plik.hl  # + gen + shebang\nhl ast plik.hl           # AST jako JSON\nhl repl                  # REPL interaktywny\nhl shell                 # HL jako powłoka\nhl exec nazwa            # skrypt systemowy\nhl search fraza          # szukaj skryptów\nhl gen-info plik.hl      # gen + shebang\nhl docs                  # dokumentacja TUI\nhl version               # wersja\nhl -c \"~> Hej!\"          # kod inline"),
                Heading3("Kompilacja —.hl → .bc → ELF"),
                BulletList(listOf("<code>.hl → .bc</code> — Parsuj, serializuj AST do JSON bytecode. Plik.bcma shebang i jest wykonywalny.", "<code>.bc → ELF</code> — Wczytaj.bc, lower → Cranelift → C runtime → binarka ELF x86_64.")),
                CodeSample("hl compile skrypt.hl   # → skrypt.bc (bytecode)\n./skrypt.bc            # uruchom bezpośrednio\nhl compile skrypt.bc   # → skrypt (ELF)\n./skrypt               # uruchom binkarkę"),
                Heading3("hl execihl search"),
                CodeSample("hl search all            # wszystkie skrypty\nhl search update         # szukaj\nhl exec update-system    # uruchom"),
                Heading3("Powłoka"),
                CodeSample("using <gen 2>\n\n=> EDITOR = nvim\n=> PATH [\n| /usr/local/bin\n| /usr/bin\n| /usr/lib/HackerOS\n]\n\n: ll def\n    > ls -la\ndone"),
                Heading3("Diagnostyka i linter"),
                CodeSample("error: `echo` jest zabronione w blokach komend HL\n  --> skrypt.hl:5:1\n 5 │ > echo hello\n   ^^^^^^^^^^^\n  help: zamień na: `~> hello`\n\nwarning: `> sudo cmd` — użyj `^>`\n  --> skrypt.hl:8:1\n  help: zamień na: `^> cmd`"),
            )
        ),
        DocTab(
            key = "hl-przyklady",
            label = "Przykłady",
            blocks = listOf(
                Heading3("Przykłady — Gen 1 podstawy"),
                CodeSample("#!/usr/bin/env hl\n/// Aktualizacja systemu HackerOS\n\n: aktualizuj def\n    ::hr 50\n    ::bold APT — Aktualizacja\n    ^> apt-get update -y\n    ? ok\n        ^> apt-get upgrade -y\n        ::green APT zaktualizowany ✓\n    done\ndone\n\n& snap refresh          ;; snap w tle\n-- aktualizuj\n*> notify-send \"Zaktualizowano\""),
                Heading3("Przykłady — Gen 2"),
                CodeSample("#!/usr/bin/env hl\n/// Demo wszystkich funkcji gen 2\nusing <gen 2>\n# <main/colors>\n\n;; Typowane zmienne\n% cores: int  = 1\n> nproc |> @cores\n\$( @cores * 2 ) -> @threads\n~> Rdzenie: @cores | Wątki: @threads\n\n;; For-in\n@ tool in curl git nmap\n    ::which @tool\n    ? ok\n        ~> @COLOR_GREEN✓@COLOR_RESET @tool\n    done\ndone\n\n;; Switch\n> uname -s |> @os\n? switch @os\n| Linux\n    ::green Linux ✓\n| *\n    ~> OS: @os\ndone\n\n;; While\n% i: int = 0\n?~ @i < 3\n    \$( @i + 1 ) -> @i\n    ~> iter: @i\ndone\n\n;; HackerOS API\n|| hacker --version"),
                Heading3("Przykłady — Async"),
                CodeSample("/// Równoległy skan sieci\nusing <gen 2>\n// nmap\n\n:** wyniki\n\n:* nmap_scan def\n    >> nmap -sn 192.168.1.0/24\n    *-- wyniki\ndone\n\n:* ping_gw def\n    > ping -c 3 8.8.8.8\n    *-- wyniki\ndone\n\n& wget -q https://example.com/plik1.zip\n& wget -q https://example.com/plik2.zip\n\n*-- wyniki"),
                Heading3("Przykłady — Systemowe"),
                CodeSample("/// Raport systemowy — gen 2\nusing <gen 2>\n# <main/colors>\n\n> uname -r   |> @kernel\n> nproc       |> @cores\n> uname -m   |> @arch\n> id -un     |> @user\n\$( @cores * 2 ) -> @threads\n\n::hr 50\n~> @COLOR_BOLDHackerOS System Report@COLOR_RESET\n::hr 50\n~> Kernel:  @kernel\n~> Arch:    @arch\n~> Cores:   @cores\n~> Threads: @threads\n~> User:    @user\n::hr 50"),
            )
        ),
    )

    private fun buildHSharpDetailTabs(): List<DocTab> = listOf(
        DocTab(
            key = "hs-overview",
            label = "Wprowadzenie",
            blocks = listOf(
                Paragraph("H# to kompilowany, statycznie typowany język programowania stworzony dla HackerOS. Zastępuje Pythona w narzędziach CLI, GUI, cybersec i codziennych skryptach. Kompiluje do natywnych binarek przez <strong>LLVM 21</strong> (produkcja, O3+AVX2) lub uruchamia się od razu przez interpreter (<strong>h# preview</strong>, szybki loop deweloperski). Składnia inspirowana Ruby/Python, bezpieczeństwo pamięci z opcjonalnymi trybami <code>@safety</code>/<code>@arc</code>/<code>@arena</code>/<code>@pointers</code>."),
                Paragraph("<strong>h# preview</strong> — Interpreter — natychmiastowe uruchomienie bez kompilacji. Idealny do skryptowania i debugowania. Pełna obsługa closures, async/await, string interpolation."),
                Paragraph("<strong>h# compile</strong> — LLVM 21 O3+AVX2 — produkcyjna kompilacja do natywnej binarki. Zero transpilacji do C. AST → LLVM IR → obiekt → link. Cross-compilation: linux-x86_64, linux-aarch64, wasm32."),
                Paragraph("<strong>bytes build / run</strong> — Package manager i build system napisany w H# — czyta <code>bytes.hk</code>, shelluje do <code>h# compile</code>/<code>build</code>. Python interop przez venv. Test runner, formatter, doc gen."),
                Heading4("Kompilatory H#"),
                BulletList(listOf("<code>h# preview</code> — Interpreter — Szybki dev loop, zero kompilacji — ~5-15% C", "<code>h# compile</code> — LLVM 21 O3+AVX2 — Produkcja, natywna binarka — ~85-95% C", "<code>h# compile --target wasm32</code> — LLVM → WASM — WebAssembly moduł — ~70-80% native", "<code>bytes build</code> — LLVM (via h# compile) — Produkcja, z bytes.hk — ~85-95% C")),
            )
        ),
        DocTab(
            key = "hs-install",
            label = "Instalacja i narzędzia",
            blocks = listOf(
                Paragraph("Ekosystem H# składa się z dwóch narzędzi: <strong>h#</strong> (kompilator + interpreter CLI) i <strong>bytes</strong> (package manager i build system, napisany w H#)."),
                Paragraph("<strong>h#</strong> — Główne CLI. Kompiluje przez <strong>LLVM 21 O3+AVX2</strong> (produkcja), uruchamia interpreter (preview), sprawdza składnię i typy, tworzy projekty z szablonami (<code>app</code>, <code>web</code>, <code>tui</code>, <code>wasm</code>, <code>lib</code>, <code>cybersec</code>)."),
                Paragraph("<strong>bytes</strong> — Package manager napisany w H#. Konfiguracja: <code>bytes.hk</code> (jeden plik, prosty format sekcyjny). Workspace, Python interop, test runner, formatter, doc gen."),
                Heading4("Instalacja na HackerOS"),
                CodeSample(";; Przez package manager HackerOS\nhacker unpack h#\nhacker unpack h#-utils"),
                Heading4("Komendy h#"),
                CodeSample(";; Interpreter — natychmiastowy podgląd\nh# preview src/main.h#\n\n;; Kompilacja LLVM (natywna binarka)\nh# compile src/main.h#\nh# compile src/main.h# --release -o myapp\nh# compile src/main.h# --target linux-aarch64\nh# compile src/main.h# --target wasm32 -o module.wasm\n\n;; Sprawdź składnię i typy\nh# check src/main.h#\nh# check a.h# b.h# c.h#\n\n;; Nowy projekt\nh# new myapp\nh# new myapp --template cybersec\nh# new myapp --template web\nh# new myapp --template tui\nh# new myapp --template wasm\nh# new myapp --template lib\n\n;; Dostępne targety kompilacji\nh# targets"),
                Heading4("bytes — Package Manager & Build System"),
                Paragraph("<strong>bytes</strong> to package manager i build system dla projektów H# — napisany w samym H# (nie w Rust), buduje przez <code>hsharp compile</code>/<code>build</code> (LLVM). Workspace dla projektów wielojęzykowych, Python interop, test runner, formatter, doc gen."),
                CodeSample(";; Nowy projekt\nbytes new myapp && cd myapp\n\n;; ── Budowanie / uruchomienie ──────────────────────────\nbytes build                      ;; kompiluje przez hsharp build (LLVM)\nbytes build --release\nbytes run                        ;; build + uruchom\n\n;; ── Pakiety H# ────────────────────────────────────────\nbytes add scanner                ;; z bytes registry\nbytes add github.com/user/repo   ;; z GitHub\nbytes install                    ;; zainstaluj wszystkie z bytes.hk\nbytes update                     ;; aktualizuj do latest\nbytes remove scanner\n\n;; ── Python interop ────────────────────────────────────\nbytes python numpy               ;; zainstaluj bibliotekę Python\nbytes python cryptography\n\n;; ── Narzędzia deweloperskie ───────────────────────────\nbytes test                       ;; test runner\nbytes test tests/core/ --verbose\nbytes fmt                        ;; formatter (in-place)\nbytes doc                        ;; generuj HTML docs do docs/\n\n;; ── Cache / środowisko ────────────────────────────────\nbytes clean                      ;; czyść .cache/ i build/\n\n;; ── Workspace (multi-projekt) ─────────────────────────\nbytes workspace new monorepo --members \"backend:h# frontend:rust tools:h#\"\nbytes workspace build            ;; zbuduj wszystkich członków równolegle\nbytes workspace run backend      ;; uruchom konkretny member"),
                Heading4("bytes.hk"),
                CodeSample("! H# project — bytes.hk\n\n[package]\n-> name        => myapp\n-> version     => 0.1.0\n-> description => H# script project\n-> entry       => src/main.h#\n\n[build]\n-> emit     => bin\n-> mem-mode => safety    ! default | safety | arc | arena | pointers\n\n[dependencies]\n! scanner => latest\n\n[registry]\n-> mode   => release     ! release (latest tag) | source (HEAD)"),
                Heading4("Workspace (bytes.hk — multi-language)"),
                CodeSample("! H# SPECIAL workspace — bytes.hk\n\n[workspace]\n-> name    => monorepo\n-> version => 0.1.0\n-> mode    => special\n-> members => [\"backend\", \"frontend\", \"tools\"]\n-> languages\n--> backend   => h#\n--> frontend  => rust\n--> tools     => h#\n\n[build]\n-> parallel => true\n-> cache    => .cache/"),
                Heading4("Struktura projektu bytes"),
                CodeSample("myapp/\n├── bytes.hk            ← konfiguracja\n├── src/\n│   ├── main.h#         ← punkt wejścia (entry)\n│   └── utils.h#\n├── tests/\n│   └── main_test.h#\n└── docs/               ← generowane przez bytes doc"),
                Heading4("Plik konfiguracyjny"),
                BulletList(listOf("<code>bytes</code> — bytes.hk — HK (własny) — Projekt + build + dependencies + workspace + Python deps ( -> key => val )")),
                Heading4("Rozszerzenia plików"),
                BulletList(listOf("<code>.h#</code> — Główny format kodu źródłowego H#", "<code>.hsp</code> — Alternatywne rozszerzenie (H# script)", "<code>.hsl</code> — H# Library — skompilowana biblioteka")),
            )
        ),
        DocTab(
            key = "hs-toc",
            label = "Pełny spis treści",
            blocks = listOf(
                Paragraph("Poniżej pełny spis treści oficjalnej dokumentacji H# (interaktywna wersja z playgroundem WASM jest dostępna pod linkiem GitHub poniżej — obejmuje wszystkie tematy wymienione tu jako lista)."),
                Heading4("Wprowadzenie"),
                BulletList(listOf("O języku H#", "Instalacja &amp; narzędzia", "Struktura projektu")),
                Heading4("Podstawy"),
                BulletList(listOf("Komentarze", "Zmienne &amp; typy", "Import system", "Wyjście / builtins")),
                Heading4("Sterowanie"),
                BulletList(listOf("If / Elsif / Else", "Match", "While", "For &amp; zakresy")),
                Heading4("Organizacja kodu"),
                BulletList(listOf("Funkcje", "Structs &amp; impl", "Traits")),
                Heading4("System typów"),
                BulletList(listOf("Enum", "Typy &amp; operatory")),
                Heading4("Zaawansowane"),
                BulletList(listOf("Unsafe &amp; Arena", "@ Tryby pamięci", "extern &amp; FFI", "Atrybuty #[...]", "Optional &amp; błędy", "Generics", "Closures")),
                Heading4("Testy & Moduły"),
                BulletList(listOf("Testy", "Moduły")),
                Heading4("Biblioteka std"),
                BulletList(listOf("std — wszystkie moduły")),
                Heading4("Referencja"),
                BulletList(listOf("Gramatyka &amp; parsowanie", "Błędy &amp; diagnostyka")),
                Heading4("Przykłady"),
                BulletList(listOf("Hello World", "Port Scanner", "XOR Cipher", "Struct + Trait + Enum", "GUI App — GTK4", "Python Interop", "Config JSON", "Fibonacci + memo", "Klient HTTP", "Producer/Consumer", "Binary Search", "Parser argumentów", "Key-Value Store", "Macierze", "Parser logów", "Maszyna stanów", "Result enum", "Kolejka zadań", "Silnik szablonów", "Parser binarny", "Cheat Sheet")),
            )
        ),
    )

    private fun buildHackerScriptDetailTabs(): List<DocTab> = listOf(
        DocTab(
            key = "hsc-overview",
            label = "Wprowadzenie",
            blocks = listOf(
                Paragraph("HackerScript to eksperymentalny, hobbystyczny język programowania (nie tylko dla HackerOS), transpilowany do Rust i Python w jednym pliku, obsługujący masę ekosystemów."),
                Paragraph("Pliki źródłowe mają rozszerzenie <code>.hks</code>. Składnia opiera się na blokach domykanych nawiasem kwadratowym <code>[ ... ]</code> zamiast wcięć czy klamer, co ma ułatwiać jednoznaczną transpilację do kilku języków docelowych z tego samego drzewa składniowego."),
                BulletList(listOf(
                    "<strong>Transpilacja</strong> — jeden plik źródłowy generuje równoważny kod Rust i Python.",
                    "<strong>Funkcje, rekurencja, pętle</strong> — <code>fun</code>, <code>while</code>, <code>for ... in</code>, tablice literalne.",
                    "<strong>log(...)</strong> — wbudowana funkcja wyjścia, przyjmuje dowolną liczbę argumentów."
                )),
                Heading4("Przykład"),
                CodeSample(HACKERSCRIPT_SAMPLE),
            )
        ),
        DocTab(
            key = "hsc-status",
            label = "Status projektu",
            blocks = listOf(
                Paragraph("HackerScript jest we wczesnej, hobbystycznej fazie rozwoju — pełna, interaktywna dokumentacja (jak w przypadku Hacker Lang czy H#) jest w przygotowaniu i pojawi się na oficjalnej stronie HackerOS w kolejnych wydaniach."),
                Paragraph("Do tego czasu powyższy przykład w zakładce „Wprowadzenie” pozostaje najlepszym, zawsze aktualnym źródłem — jest częścią samej aplikacji, więc działa też offline."),
                LinkLine("Repozytorium źródłowe na GitHub", "https://github.com/HackerOS-Linux-System"),
            )
        ),
    )

}
