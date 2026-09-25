package com.hackeros.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackeros.app.Constants
import com.hackeros.app.data.articles.Article
import com.hackeros.app.data.articles.ArticleBlock
import com.hackeros.app.data.articles.ArticlesData
import com.hackeros.app.data.model.AppTheme
import com.hackeros.app.data.model.Language
import com.hackeros.app.ui.components.InlineHtmlText
import com.hackeros.app.ui.theme.LocalAppTheme
import com.hackeros.app.ui.theme.cardColor
import com.hackeros.app.ui.theme.mutedColor
import com.hackeros.app.ui.theme.primaryColor
import com.hackeros.app.ui.theme.textColor
import com.hackeros.app.utils.Translations
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Articles section: a searchable, tag-filterable list plus a native article reader. Everything
 * shown here is fetched from the website's own `articles/` data files (see Constants.ARTICLES_*
 * and data/articles/ArticleParser.kt) - no WebView. Which article is open lives in the
 * ViewModel, and the system back gesture closes the reader before leaving the tab.
 */
@Composable
fun ArticlesScreen(
    data: ArticlesData?,
    loading: Boolean,
    error: Boolean,
    fromCache: Boolean,
    openArticleId: String?,
    currentLanguage: Language,
    translations: Translations,
    onOpenArticle: (String) -> Unit,
    onCloseArticle: () -> Unit,
    onRetry: () -> Unit
) {
    val theme = LocalAppTheme.current
    val lang = currentLanguage.code
    val open = data?.articles?.firstOrNull { it.id == openArticleId }

    BackHandler(enabled = open != null) { onCloseArticle() }

    // `open` can only be non-null when `data` is (it's looked up from data.articles), but the
    // compiler can't see that through the elvis/firstOrNull chain, hence the explicit `!!`
    // instead of a redundant `data != null` check.
    if (open != null) {
        ArticleReader(open, data!!, lang, theme, translations, onCloseArticle)
    } else {
        ArticleList(data, loading, error, fromCache, lang, theme, translations, onOpenArticle, onRetry)
    }
}

// ------------------------------------------------------------------------------------------
// List
// ------------------------------------------------------------------------------------------

@Composable
private fun ArticleList(
    data: ArticlesData?,
    loading: Boolean,
    error: Boolean,
    fromCache: Boolean,
    lang: String,
    theme: AppTheme,
    t: Translations,
    onOpen: (String) -> Unit,
    onRetry: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var tag by rememberSaveable { mutableStateOf<String?>(null) }
    val articles = data?.articles.orEmpty()
    val usedTags = remember(articles) { articles.flatMap { it.tags }.distinct() }
    val filtered = remember(articles, query, tag, lang) {
        val q = query.trim().lowercase()
        articles.filter { a ->
            (tag == null || tag in a.tags) && (q.isEmpty() || a.searchText(lang).contains(q))
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ScreenHeader(t.nav_articles, t.sub_articles, theme) }

        when {
            loading && data == null -> item { CenteredLoading(theme.primaryColor(), t.decrypting) }
            error && data == null -> item { CenteredError(t, onRetry) }
            else -> {
                if (fromCache) item { OfflineBanner(text = t.offline_cached_banner, primaryColor = theme.primaryColor()) }

                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(t.articles_search_placeholder, fontSize = 13.sp, color = theme.mutedColor()) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = theme.mutedColor()) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Clear, null, tint = theme.mutedColor())
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
                }

                if (usedTags.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { TagChip(t.articles_all_tags, tag == null, theme) { tag = null } }
                            items(usedTags) { id ->
                                TagChip(data?.tagLabel(id, lang) ?: id, tag == id, theme) { tag = if (tag == id) null else id }
                            }
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    item {
                        Text(
                            t.articles_empty,
                            color = theme.mutedColor(),
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                items(filtered, key = { it.id }) { a ->
                    ArticleCard(a, data, lang, theme, t) { onOpen(a.id) }
                }
            }
        }
    }
}

@Composable
private fun ScreenHeader(title: String, badge: String, theme: AppTheme) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
        Text(title, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color.White)
        Spacer(Modifier.width(12.dp))
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = theme.primaryColor().copy(alpha = 0.15f),
            border = BorderStroke(1.dp, theme.primaryColor().copy(alpha = 0.5f))
        ) {
            Text(
                badge,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = theme.primaryColor(),
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun TagChip(label: String, selected: Boolean, theme: AppTheme, onClick: () -> Unit) {
    val primary = theme.primaryColor()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) primary else Color.Transparent)
            .border(1.dp, if (selected) primary else Color.White.copy(alpha = 0.15f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) theme.cardColor() else theme.mutedColor()
        )
    }
}

@Composable
private fun ArticleCard(a: Article, data: ArticlesData?, lang: String, theme: AppTheme, t: Translations, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(theme.cardColor())
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(a.icon, fontSize = 28.sp)
            Text(
                a.titleFor(lang),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                color = theme.textColor(),
                modifier = Modifier.weight(1f)
            )
        }
        Text(a.summaryFor(lang), fontSize = 13.sp, lineHeight = 19.sp, color = theme.mutedColor())
        MetaLine(a, data, lang, theme, t)
    }
}

@Composable
private fun MetaLine(a: Article, data: ArticlesData?, lang: String, theme: AppTheme, t: Translations) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "${formatDate(a.date, lang)}  ·  ${a.readingMinutes} ${t.articles_min_read}",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = theme.mutedColor().copy(alpha = 0.8f)
        )
        if (a.tags.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                a.tags.forEach { id ->
                    Text(
                        data?.tagLabel(id, lang) ?: id,
                        fontSize = 10.sp,
                        color = theme.primaryColor(),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(theme.primaryColor().copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

private fun formatDate(iso: String, lang: String): String = try {
    LocalDate.parse(iso).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.forLanguageTag(lang)))
} catch (_: Exception) {
    iso
}

// ------------------------------------------------------------------------------------------
// Reader
// ------------------------------------------------------------------------------------------

@Composable
private fun ArticleReader(
    a: Article,
    data: ArticlesData,
    lang: String,
    theme: AppTheme,
    t: Translations,
    onBack: () -> Unit
) {
    val blocks = a.blocksFor(lang)
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = theme.primaryColor(), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(t.articles_back, color = theme.primaryColor(), fontSize = 13.sp)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(a.icon, fontSize = 40.sp)
                Text(
                    a.titleFor(lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                    color = Color.White
                )
                Text(a.summaryFor(lang), fontSize = 14.sp, lineHeight = 21.sp, color = theme.mutedColor())
                MetaLine(a, data, lang, theme, t)
                if (a.author.isNotBlank()) {
                    Text(a.author, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = theme.mutedColor().copy(alpha = 0.8f))
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            }
        }
        if (a.isFallback(lang)) {
            item { NoteBox("info", t.articles_fallback_notice, theme) }
        }
        items(blocks.size) { i -> BlockView(blocks[i], theme, t) }
        item {
            TextButton(onClick = {
                try { uriHandler.openUri(Constants.ARTICLES_WEB_URL + "#/" + a.id) } catch (_: Exception) { }
            }) {
                Icon(Icons.Default.OpenInNew, null, tint = theme.mutedColor(), modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(t.articles_open_website, color = theme.mutedColor(), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun BlockView(block: ArticleBlock, theme: AppTheme, t: Translations) {
    when (block) {
        is ArticleBlock.Heading -> Text(
            block.text,
            fontWeight = FontWeight.Bold,
            fontSize = if (block.level == 2) 20.sp else 16.sp,
            color = Color.White,
            modifier = Modifier.padding(top = if (block.level == 2) 10.dp else 4.dp)
        )
        is ArticleBlock.Paragraph -> Html(block.html, theme)
        is ArticleBlock.Quote -> Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.2f)))
            Spacer(Modifier.width(12.dp))
            Html(block.html, theme, muted = true)
        }
        is ArticleBlock.Bullets -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            block.items.forEachIndexed { i, item ->
                Row {
                    Text(
                        if (block.ordered) "${i + 1}." else "•",
                        color = theme.primaryColor(),
                        fontSize = 15.sp,
                        modifier = Modifier.width(if (block.ordered) 26.dp else 18.dp)
                    )
                    Html(item, theme, modifier = Modifier.weight(1f))
                }
            }
        }
        is ArticleBlock.Code -> CodeBox(block, theme, t)
        is ArticleBlock.Note -> NoteBox(block.kind, block.html, theme)
        is ArticleBlock.Table -> TableBox(block, theme)
    }
}

@Composable
private fun Html(html: String, theme: AppTheme, modifier: Modifier = Modifier, muted: Boolean = false, size: Int = 15) {
    InlineHtmlText(
        html = html,
        modifier = modifier,
        color = if (muted) theme.mutedColor() else theme.textColor().copy(alpha = 0.92f),
        linkColor = Color(0xFF4A9EFF),
        codeColor = theme.primaryColor(),
        fontSize = size.sp,
        lineHeight = (size * 1.55f).sp
    )
}

@Composable
private fun CodeBox(block: ArticleBlock.Code, theme: AppTheme, t: Translations) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.35f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.04f))
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(block.title ?: block.lang, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = theme.mutedColor())
            IconButton(onClick = { copyCode(context, block.code, t) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ContentCopy, t.articles_copy, tint = theme.mutedColor(), modifier = Modifier.size(15.dp))
            }
        }
        Text(
            block.code,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = theme.textColor(),
            softWrap = false,
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(12.dp)
        )
    }
}

private fun copyCode(context: Context, text: String, t: Translations) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("code", text))
    Toast.makeText(context, t.articles_copied, Toast.LENGTH_SHORT).show()
}

@Composable
private fun NoteBox(kind: String, html: String, theme: AppTheme) {
    val (icon, accent) = when (kind) {
        "tip" -> "💡" to Color(0xFF5FD08A)
        "warn" -> "⚠️" to Color(0xFFF0B070)
        else -> "ℹ️" to Color(0xFF4A9EFF)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.08f))
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(accent))
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(icon, fontSize = 16.sp)
            Html(html, theme, modifier = Modifier.weight(1f), size = 14)
        }
    }
}

@Composable
private fun TableBox(table: ArticleBlock.Table, theme: AppTheme) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
    ) {
        if (table.head.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.05f)).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                table.head.forEach { h ->
                    Text(h.replace(Regex("<[^>]+>"), ""), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White, modifier = Modifier.weight(1f))
                }
            }
        }
        table.rows.forEach { row ->
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
            Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { cell -> Html(cell, theme, modifier = Modifier.weight(1f), size = 12) }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------
// Loading / error
// ------------------------------------------------------------------------------------------

@Composable
private fun CenteredLoading(primary: Color, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().height(300.dp)
    ) {
        CircularProgressIndicator(color = primary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(label, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = primary)
    }
}

@Composable
private fun CenteredError(t: Translations, onRetry: () -> Unit) {
    val red = Color(0xFFEF4444)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().height(300.dp).padding(horizontal = 32.dp)
    ) {
        Icon(Icons.Default.WifiOff, null, tint = red, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(16.dp))
        Text(t.error_signal, color = red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Text(t.error_network, color = Color(0xFF94A3B8), fontSize = 12.sp)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = red)) {
            Text(t.retry, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}
