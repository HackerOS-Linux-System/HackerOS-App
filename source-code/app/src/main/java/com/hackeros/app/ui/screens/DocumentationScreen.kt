package com.hackeros.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackeros.app.data.docs.DetailPage
import com.hackeros.app.data.docs.DocRemote
import com.hackeros.app.data.docs.DocBlock
import com.hackeros.app.data.docs.DocLinks
import com.hackeros.app.data.docs.DocPage
import com.hackeros.app.data.docs.DocTab
import com.hackeros.app.data.docs.ToolTableRow
import com.hackeros.app.data.model.Language
import com.hackeros.app.ui.components.InlineHtmlText
import com.hackeros.app.ui.components.LocalDocLinkHandler
import com.hackeros.app.ui.theme.LocalAppTheme
import com.hackeros.app.ui.theme.backgroundColor
import com.hackeros.app.ui.theme.cardColor
import com.hackeros.app.ui.theme.mutedColor
import com.hackeros.app.ui.theme.primaryColor
import com.hackeros.app.ui.theme.textColor
import com.hackeros.app.utils.Translations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DocumentationScreen(
    docPage: DocPage?,
    loading: Boolean,
    error: Boolean,
    fromCache: Boolean,
    currentLanguage: Language,
    translations: Translations,
    onRetry: () -> Unit
) {
    val theme = LocalAppTheme.current
    val t = translations
    var selectedTabKey by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Stack of native documentation "detail" pages that are currently open on top of the main
    // documentation page (H#, Hacker Lang, HackerScript, a tool's page, the tools index, ...). A
    // detail page can open another one (tools index -> hpm -> HK format), and the system back
    // gesture / back arrow pops one level at a time. Everything is plain Compose UI rendered from
    // the pages fetched live from the official website - nothing ever opens a browser or a WebView.
    val detailStack = remember { mutableStateListOf<String>() }
    val openDetail: (String) -> Unit = { key ->
        if (detailStack.lastOrNull() != key) detailStack.add(key)
    }
    val closeDetail: () -> Unit = {
        if (detailStack.isNotEmpty()) detailStack.removeAt(detailStack.lastIndex)
    }

    // Reset the active tab whenever a new page loads (e.g. after a language change). The detail
    // stack is cleared too, so a stale detail page is never shown on top of new content.
    LaunchedEffect(docPage) {
        if (docPage != null && (selectedTabKey == null || docPage.tabs.none { it.key == selectedTabKey })) {
            selectedTabKey = docPage.tabs.firstOrNull()?.key
        }
        detailStack.clear()
    }

    BackHandler(enabled = detailStack.isNotEmpty()) { closeDetail() }

    // Links written inside documentation text (<a href>) that point at another doc page are
    // opened natively too; only genuinely external URLs fall through to the browser.
    val linkHandler: (String) -> Boolean = { url ->
        val key = DocLinks.resolve(url)
        if (key != null) {
            openDetail(key)
            true
        } else false
    }

    CompositionLocalProvider(LocalDocLinkHandler provides linkHandler) {
        if (detailStack.isNotEmpty()) {
            DocDetailScreen(
                detailKey = detailStack.last(),
                currentLanguage = currentLanguage,
                translations = t,
                onBack = closeDetail,
                onOpenDetail = openDetail
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 10.dp)) {
                    Text(
                        text = t.header_docs,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = Color.White
                    )
                    Text(
                        text = t.sub_docs,
                        fontSize = 13.sp,
                        color = theme.mutedColor(),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                when {
                    loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = theme.primaryColor())
                    }
                    error || docPage == null -> Box(
                        Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.WifiOff, null, tint = Color(0xFFEF4444), modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(t.error_signal, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(t.error_network, color = theme.mutedColor(), fontSize = 12.sp)
                            Spacer(Modifier.height(20.dp))
                            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor())) {
                                Icon(Icons.Default.Refresh, null, tint = theme.backgroundColor(), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(t.retry, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = theme.backgroundColor())
                            }
                        }
                    }
                    else -> {
                        if (fromCache) {
                            Box(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                                OfflineBanner(text = t.offline_cached_banner, primaryColor = theme.primaryColor())
                            }
                        }
                        if (docPage.isEnglishFallback) {
                            Box(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                                InfoBanner(t.doc_en_only_banner)
                            }
                        }
                        DocTabsBody(
                            tabs = docPage.tabs,
                            selectedTabKey = selectedTabKey,
                            onSelectTab = { selectedTabKey = it },
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            translations = t,
                            onOpenDetail = openDetail
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoBanner(text: String) {
    val theme = LocalAppTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(theme.primaryColor().copy(alpha = 0.08f))
            .border(1.dp, theme.primaryColor().copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.Info, null, tint = theme.primaryColor(), modifier = Modifier.size(14.dp))
        Text(text, fontSize = 11.sp, color = theme.primaryColor())
    }
}

/**
 * Search field + tab chips + the active tab's content. Shared by the main documentation page and
 * every native detail page.
 *
 * Search filters the tab list by matching against a tab's label or any of its rendered text - the
 * same behavior as the website's own doSearch(), just running natively instead of against a DOM.
 */
@Composable
private fun DocTabsBody(
    tabs: List<DocTab>,
    selectedTabKey: String?,
    onSelectTab: (String) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    translations: Translations,
    onOpenDetail: (String) -> Unit
) {
    val theme = LocalAppTheme.current
    val t = translations

    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 8.dp),
        placeholder = { Text(t.doc_tab_search_placeholder, fontSize = 13.sp, color = theme.mutedColor()) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = theme.mutedColor(), modifier = Modifier.size(18.dp)) },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchChange("") }) {
                    Icon(Icons.Default.Close, null, tint = theme.mutedColor(), modifier = Modifier.size(16.dp))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = theme.primaryColor(),
            unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
        )
    )

    val filteredTabs = remember(tabs, searchQuery) {
        if (searchQuery.isBlank()) tabs else {
            val q = searchQuery.trim().lowercase()
            tabs.filter { tab ->
                tab.label.lowercase().contains(q) || blockTextOf(tab).lowercase().contains(q)
            }
        }
    }

    if (searchQuery.isNotBlank() && filteredTabs.isEmpty()) {
        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(t.doc_no_search_results, color = theme.mutedColor(), fontSize = 12.sp)
        }
        return
    }

    // Tab menu
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        items(filteredTabs, key = { it.key }) { tab ->
            val active = tab.key == selectedTabKey
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (active) theme.primaryColor() else Color.White.copy(alpha = 0.06f))
                    .clickable { onSelectTab(tab.key) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    tab.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (active) theme.backgroundColor() else theme.mutedColor()
                )
            }
        }
    }

    val activeTab = filteredTabs.find { it.key == selectedTabKey } ?: filteredTabs.firstOrNull()
    if (activeTab != null) {
        LazyColumn(
            // Generous bottom padding: the last block in a tab (often a link) must always clear
            // the floating bottom nav bar entirely, or it is hidden and its touches are blocked.
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(activeTab.blocks) { block ->
                DocBlockView(block, theme, t, onOpenDetail)
            }
        }
    }
}

private fun blockTextOf(tab: DocTab): String = buildString {
    tab.blocks.forEach { b ->
        when (b) {
            is DocBlock.Heading2 -> append(b.text).append(' ')
            is DocBlock.Heading3 -> append(b.text).append(' ')
            is DocBlock.Heading4 -> append(b.text).append(' ')
            is DocBlock.Paragraph -> append(b.html).append(' ')
            is DocBlock.BulletList -> b.itemsHtml.forEach { append(it).append(' ') }
            is DocBlock.NumberedList -> b.itemsHtml.forEach { append(it).append(' ') }
            is DocBlock.Command -> append(b.text).append(' ')
            is DocBlock.CodeSample -> append(b.text).append(' ')
            is DocBlock.LinkLine -> append(b.labelHtml).append(' ')
            is DocBlock.ToolsTable -> b.rows.forEach { append(it.tool).append(' ').append(it.description).append(' ') }
            is DocBlock.Table -> {
                b.headers.forEach { append(it).append(' ') }
                b.rows.forEach { row -> row.forEach { append(it).append(' ') } }
            }
            is DocBlock.Callout -> append(b.html).append(' ')
            is DocBlock.DetailCard -> append(b.title).append(' ').append(b.description).append(' ')
            DocBlock.Divider -> {}
        }
    }
}

@Composable
private fun DocBlockView(
    block: DocBlock,
    theme: com.hackeros.app.data.model.AppTheme,
    translations: Translations,
    onOpenDetail: (String) -> Unit = {}
) {
    when (block) {
        is DocBlock.Heading2 -> Text(
            block.text, fontSize = 19.sp, fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace, color = Color.White,
            modifier = Modifier.padding(top = 10.dp)
        )
        is DocBlock.Heading3 -> Text(
            block.text, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            color = theme.primaryColor(), modifier = Modifier.padding(top = 6.dp)
        )
        is DocBlock.Heading4 -> Text(
            block.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            color = theme.textColor(), modifier = Modifier.padding(top = 2.dp)
        )
        is DocBlock.Paragraph -> if (block.html.isNotBlank()) {
            InlineHtmlText(
                html = block.html, color = theme.textColor(), fontSize = 13.sp, lineHeight = 19.sp,
                linkColor = theme.primaryColor(), codeColor = theme.primaryColor()
            )
        }
        is DocBlock.BulletList -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            block.itemsHtml.forEach { item ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("•", color = theme.primaryColor(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    InlineHtmlText(
                        html = item, color = theme.textColor(), fontSize = 13.sp, lineHeight = 18.sp,
                        linkColor = theme.primaryColor(), codeColor = theme.primaryColor(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        is DocBlock.NumberedList -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            block.itemsHtml.forEachIndexed { index, item ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${index + 1}.", color = theme.primaryColor(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    InlineHtmlText(
                        html = item, color = theme.textColor(), fontSize = 13.sp, lineHeight = 18.sp,
                        linkColor = theme.primaryColor(), codeColor = theme.primaryColor(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        is DocBlock.Command -> CommandBlockView(block.text, theme, translations)
        is DocBlock.CodeSample -> CodeSampleView(block.text, theme, translations)
        is DocBlock.LinkLine -> LinkLineView(
            labelHtml = block.labelHtml,
            url = block.url,
            theme = theme,
            isNative = block.nativeDetailKey != null,
            onClick = {
                if (block.nativeDetailKey != null) {
                    onOpenDetail(block.nativeDetailKey)
                    true
                } else false
            }
        )
        is DocBlock.ToolsTable -> ToolsTableView(block.rows, theme, onOpenDetail)
        is DocBlock.Table -> DocTableView(block.headers, block.rows, theme)
        is DocBlock.Callout -> CalloutView(block.kind, block.html, theme)
        is DocBlock.DetailCard -> DetailCardView(block.title, block.description, block.detailKey, theme, onOpenDetail)
        DocBlock.Divider -> HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
    }
}

@Composable
private fun CommandBlockView(command: String, theme: com.hackeros.app.data.model.AppTheme, translations: Translations) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(theme.cardColor())
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            command, fontFamily = FontFamily.Monospace, fontSize = 12.sp,
            color = theme.primaryColor(), modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { copyToClipboard(context, command, translations) }, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.ContentCopy, null, tint = theme.mutedColor(), modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun CodeSampleView(code: String, theme: com.hackeros.app.data.model.AppTheme, translations: Translations) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(theme.cardColor())
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = { copyToClipboard(context, code, translations) }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.ContentCopy, null, tint = theme.mutedColor(), modifier = Modifier.size(14.dp))
            }
        }
        // Code keeps its own line structure and scrolls sideways instead of re-wrapping (wrapping
        // breaks alignment in ASCII trees, tables and indented code).
        Text(
            code,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = theme.textColor(),
            lineHeight = 16.sp,
            softWrap = false,
            modifier = Modifier.horizontalScroll(rememberScrollState())
        )
    }
}

@Composable
private fun LinkLineView(
    labelHtml: String,
    url: String,
    theme: com.hackeros.app.data.model.AppTheme,
    isNative: Boolean = false,
    onClick: () -> Boolean = { false }
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                // onClick() returns true when this link was handled natively in-app (see
                // DocBlock.LinkLine.nativeDetailKey) - the external browser is only ever a
                // fallback for links that don't have a native destination.
                if (!onClick() && url.isNotBlank()) {
                    try { uriHandler.openUri(url) } catch (_: Exception) {}
                }
            }
            // A larger touch target than the text itself, and enough vertical padding that
            // this row can never end up sitting flush against - or clipped by - the bottom
            // nav bar even as the very last item in a tab.
            .padding(vertical = 10.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            if (isNative) Icons.Default.ArrowForward else Icons.Default.OpenInNew,
            null, tint = theme.primaryColor(), modifier = Modifier.size(13.dp)
        )
        InlineHtmlText(html = labelHtml, color = theme.primaryColor(), fontSize = 12.sp)
    }
}

@Composable
private fun ToolsTableView(
    rows: List<ToolTableRow>,
    theme: com.hackeros.app.data.model.AppTheme,
    onOpenDetail: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            val key = row.detailKey
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(theme.cardColor())
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                    .then(if (key != null) Modifier.clickable { onOpenDetail(key) } else Modifier)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        row.tool, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                        fontSize = 12.sp, color = theme.primaryColor(), modifier = Modifier.weight(1f)
                    )
                    if (key != null) {
                        Icon(Icons.Default.ArrowForward, null, tint = theme.primaryColor(), modifier = Modifier.size(13.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(row.description, fontSize = 12.sp, color = theme.textColor(), lineHeight = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(row.installation, fontSize = 10.sp, color = theme.mutedColor())
            }
        }
    }
}

/**
 * A table from the website's docs, shown phone-friendly: one compact card per row. The first cell
 * is the row's title; when a table has 3+ columns the remaining cells get their column header as
 * a small label so nothing loses its meaning.
 */
@Composable
private fun DocTableView(
    headers: List<String>,
    rows: List<List<String>>,
    theme: com.hackeros.app.data.model.AppTheme
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { cells ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(theme.cardColor())
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                cells.forEachIndexed { idx, cell ->
                    if (cell.isNotBlank()) {
                        if (idx == 0) {
                            InlineHtmlText(
                                html = "<strong>$cell</strong>", color = theme.primaryColor(), fontSize = 12.sp,
                                lineHeight = 17.sp, linkColor = theme.primaryColor(), codeColor = theme.primaryColor()
                            )
                        } else {
                            val header = headers.getOrNull(idx).orEmpty()
                            if (cells.size > 2 && header.isNotBlank()) {
                                Text(
                                    header, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                    color = theme.mutedColor(), modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                            InlineHtmlText(
                                html = cell, color = theme.textColor(), fontSize = 12.sp, lineHeight = 17.sp,
                                linkColor = theme.primaryColor(), codeColor = theme.primaryColor(),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalloutView(kind: String, html: String, theme: com.hackeros.app.data.model.AppTheme) {
    val accent = when (kind) {
        "warn" -> Color(0xFFF59E0B)
        "tip" -> Color(0xFF22C55E)
        else -> theme.primaryColor()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.08f))
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            when (kind) {
                "warn" -> Icons.Default.Warning
                "tip" -> Icons.Default.Lightbulb
                else -> Icons.Default.Info
            },
            null, tint = accent, modifier = Modifier.size(16.dp).padding(top = 2.dp)
        )
        InlineHtmlText(
            html = html, color = theme.textColor(), fontSize = 12.sp, lineHeight = 17.sp,
            linkColor = accent, codeColor = accent, modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DetailCardView(
    title: String,
    description: String,
    detailKey: String,
    theme: com.hackeros.app.data.model.AppTheme,
    onOpenDetail: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(theme.cardColor())
            .border(1.dp, theme.primaryColor().copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .clickable { onOpenDetail(detailKey) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                fontSize = 14.sp, color = theme.primaryColor()
            )
            if (description.isNotBlank()) {
                Text(
                    description, fontSize = 12.sp, color = theme.textColor(), lineHeight = 16.sp,
                    maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        Icon(Icons.Default.ArrowForward, null, tint = theme.primaryColor(), modifier = Modifier.size(16.dp))
    }
}

private fun copyToClipboard(context: Context, text: String, translations: Translations) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("command", text))
    Toast.makeText(context, translations.toast_copied, Toast.LENGTH_SHORT).show()
}

/**
 * A fully native, in-app documentation page (H#, Hacker Lang, HackerScript, a tool's page, the
 * tools index, the download page ...), fetched live from the official HackerOS website (see [DocRemote]).
 * It never leaves the app, opens no browser tab and uses no WebView - it is plain Compose UI,
 * exactly like the rest of the Documentation tab, with the same tab bar and search. Links inside it
 * to other documentation pages open on top (the back arrow / back gesture goes up one level).
 */
@Composable
private fun DocDetailScreen(
    detailKey: String,
    currentLanguage: Language,
    translations: Translations,
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit
) {
    val theme = LocalAppTheme.current
    val t = translations
    val context = LocalContext.current
    val langCode = currentLanguage.code

    var loading by remember(detailKey, langCode) { mutableStateOf(true) }
    var result by remember(detailKey, langCode) { mutableStateOf<DocRemote.Page?>(null) }
    var reloadTick by remember(detailKey, langCode) { mutableIntStateOf(0) }
    // The page is fetched live from the official HackerOS website repository and converted on the
    // device (see DocRemote); if the network is down, the last offline copy is used.
    LaunchedEffect(detailKey, langCode, reloadTick) {
        loading = true
        result = DocRemote.load(context, detailKey, langCode, force = reloadTick > 0)
        loading = false
    }
    val page: DetailPage? = result?.page

    var selectedTabKey by remember(detailKey) { mutableStateOf<String?>(null) }
    var searchQuery by remember(detailKey) { mutableStateOf("") }
    LaunchedEffect(page) {
        val p = page
        if (p != null && (selectedTabKey == null || p.tabs.none { it.key == selectedTabKey })) {
            selectedTabKey = p.tabs.firstOrNull()?.key
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 24.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = t.doc_detail_back, tint = Color.White)
            }
            Column(modifier = Modifier.padding(start = 4.dp)) {
                Text(
                    text = page?.title ?: "",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = t.doc_detail_native_notice,
                    fontSize = 11.sp,
                    color = theme.mutedColor()
                )
            }
        }

        val p = page
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = theme.primaryColor())
            }
            p == null || p.tabs.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.WifiOff, null, tint = Color(0xFFEF4444), modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(t.error_signal, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(t.error_network, color = theme.mutedColor(), fontSize = 12.sp)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = { reloadTick++ }, colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor())) {
                        Icon(Icons.Default.Refresh, null, tint = theme.backgroundColor(), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(t.retry, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = theme.backgroundColor())
                    }
                }
            }
            else -> {
                if (result?.fromCache == true) {
                    Box(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                        OfflineBanner(text = t.offline_cached_banner, primaryColor = theme.primaryColor())
                    }
                }
                // The website's pages are written in one language (Polish or English). Tell the
                // reader when that differs from the app language instead of pretending otherwise.
                val uiLang = if (langCode == "pl") "pl" else "en"
                if (p.lang != uiLang) {
                    Box(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                        InfoBanner(if (p.lang == "pl") t.doc_detail_pl_only_notice else t.doc_detail_en_only_notice)
                    }
                }
                DocTabsBody(
                    tabs = p.tabs,
                    selectedTabKey = selectedTabKey,
                    onSelectTab = { selectedTabKey = it },
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    translations = t,
                    onOpenDetail = onOpenDetail
                )
            }
        }
    }
}
