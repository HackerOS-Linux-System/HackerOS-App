package com.hackeros.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackeros.app.data.docs.DocBlock
import com.hackeros.app.data.docs.DocPage
import com.hackeros.app.data.docs.DocTab
import com.hackeros.app.data.model.Language
import com.hackeros.app.ui.components.InlineHtmlText
import com.hackeros.app.ui.theme.LocalAppTheme
import com.hackeros.app.ui.theme.backgroundColor
import com.hackeros.app.ui.theme.cardColor
import com.hackeros.app.ui.theme.mutedColor
import com.hackeros.app.ui.theme.primaryColor
import com.hackeros.app.ui.theme.textColor
import com.hackeros.app.utils.Translations

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

    // Non-null while a native documentation "detail" sub-page is open (see LinkLineView) - e.g.
    // tapping "Official HackerScript documentation" pushes this instead of opening the external
    // browser, so the whole flow stays natively inside the app with no WebView involved and
    // nothing ever "teleports" the user away. Cleared whenever the underlying doc page changes
    // (e.g. a language switch) so a stale detail page is never shown on top of new content.
    var openDetailKey by remember { mutableStateOf<String?>(null) }

    // Reset the active tab whenever a new page loads (e.g. after a language change).
    LaunchedEffect(docPage) {
        if (docPage != null && (selectedTabKey == null || docPage.tabs.none { it.key == selectedTabKey })) {
            selectedTabKey = docPage.tabs.firstOrNull()?.key
        }
        openDetailKey = null
    }

    if (openDetailKey != null) {
        DocDetailScreen(
            detailKey = openDetailKey!!,
            currentLanguage = currentLanguage,
            translations = t,
            onBack = { openDetailKey = null }
        )
        return
    }

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
                            Text(t.doc_en_only_banner, fontSize = 11.sp, color = theme.primaryColor())
                        }
                    }
                }

                // Native search: filters the tab list by matching against tab label or any of
                // that tab's rendered text content - the same behavior as the website's own
                // doSearch(), just running natively instead of against a DOM.
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 8.dp),
                    placeholder = { Text(t.doc_tab_search_placeholder, fontSize = 13.sp, color = theme.mutedColor()) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = theme.mutedColor(), modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
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

                val filteredTabs = if (searchQuery.isBlank()) docPage.tabs else {
                    val q = searchQuery.trim().lowercase()
                    docPage.tabs.filter { tab ->
                        tab.label.lowercase().contains(q) || blockTextOf(tab).lowercase().contains(q)
                    }
                }

                if (searchQuery.isNotBlank() && filteredTabs.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(t.doc_no_search_results, color = theme.mutedColor(), fontSize = 12.sp)
                    }
                } else {
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
                                    .clickable { selectedTabKey = tab.key }
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
                            // Bumped from 100.dp: the last block in a tab (often exactly this
                            // kind of LinkLine) could end up positioned right underneath the
                            // floating bottom nav bar on some devices/insets, which both hid it
                            // visually and blocked its touches - this padding is now generous
                            // enough that the last item always clears the nav bar entirely.
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 150.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(activeTab.blocks) { block ->
                                DocBlockView(block, theme, t, onOpenNativeDetail = { key -> openDetailKey = key })
                            }
                        }
                    }
                }
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
            is DocBlock.ToolsTable -> b.rows.forEach { append(it.first).append(' ').append(it.second).append(' ') }
            DocBlock.Divider -> {}
        }
    }
}

@Composable
private fun DocBlockView(
    block: DocBlock,
    theme: com.hackeros.app.data.model.AppTheme,
    translations: Translations,
    onOpenNativeDetail: (String) -> Unit = {}
) {
    when (block) {
        is DocBlock.Heading2 -> Text(
            block.text, fontSize = 19.sp, fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace, color = Color.White,
            modifier = Modifier.padding(top = 6.dp)
        )
        is DocBlock.Heading3 -> Text(
            block.text, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            color = theme.primaryColor(), modifier = Modifier.padding(top = 4.dp)
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
                    onOpenNativeDetail(block.nativeDetailKey)
                    true
                } else false
            }
        )
        is DocBlock.ToolsTable -> ToolsTableView(block.rows, theme)
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
        Text(code, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = theme.textColor(), lineHeight = 16.sp)
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
                if (!onClick()) {
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
private fun ToolsTableView(rows: List<Triple<String, String, String>>, theme: com.hackeros.app.data.model.AppTheme) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { (tool, desc, inst) ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(theme.cardColor())
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(tool, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                    fontSize = 12.sp, color = theme.primaryColor())
                Spacer(Modifier.height(4.dp))
                Text(desc, fontSize = 12.sp, color = theme.textColor(), lineHeight = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(inst, fontSize = 10.sp, color = theme.mutedColor())
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String, translations: Translations) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("command", text))
    Toast.makeText(context, translations.toast_copied, Toast.LENGTH_SHORT).show()
}

/**
 * A fully native, in-app "detail" sub-page for a documentation link (see
 * DocBlock.LinkLine.nativeDetailKey / DocContentParser.detailTabsFor). Tapping the link that
 * opens this never leaves the app, never opens a browser tab, and doesn't use any WebView - this
 * whole screen is plain Compose UI, exactly like the rest of the Documentation tab. It has its
 * own small tab bar (reusing the same look as the main doc screen) plus a back button that
 * simply clears the local navigation state - nothing "teleports" anywhere.
 */
@Composable
private fun DocDetailScreen(
    detailKey: String,
    currentLanguage: Language,
    translations: Translations,
    onBack: () -> Unit
) {
    val theme = LocalAppTheme.current
    val t = translations
    val tabs = remember(detailKey) {
        com.hackeros.app.data.docs.DocContentParser.detailTabsFor(detailKey).orEmpty()
    }
    val title = remember(detailKey) { com.hackeros.app.data.docs.DocContentParser.detailTitleFor(detailKey) }
    var selectedTabKey by remember(detailKey) { mutableStateOf(tabs.firstOrNull()?.key) }
    val activeTab = tabs.find { it.key == selectedTabKey } ?: tabs.firstOrNull()

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
                    text = title,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Color.White
                )
                Text(
                    text = t.doc_detail_native_notice,
                    fontSize = 11.sp,
                    color = theme.mutedColor()
                )
            }
        }

        if (currentLanguage != Language.PL) {
            Box(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
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
                    Text(t.doc_detail_pl_only_notice, fontSize = 11.sp, color = theme.primaryColor())
                }
            }
        }

        if (tabs.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(t.doc_no_search_results, color = theme.mutedColor(), fontSize = 12.sp)
            }
            return@Column
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            items(tabs, key = { it.key }) { tab ->
                val active = tab.key == selectedTabKey
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (active) theme.primaryColor() else Color.White.copy(alpha = 0.06f))
                        .clickable { selectedTabKey = tab.key }
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

        if (activeTab != null) {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 150.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(activeTab.blocks) { block ->
                    DocBlockView(block, theme, t)
                }
            }
        }
    }
}
