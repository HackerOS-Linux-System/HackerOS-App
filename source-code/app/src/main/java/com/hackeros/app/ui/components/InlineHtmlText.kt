package com.hackeros.app.ui.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit

/**
 * Documentation content from the website only ever uses a small, fixed set of inline tags
 * (`<strong>`, `<code>`, `<a href="">`, `<br>`, `<em>`), unlike arbitrary rich HTML - so rather
 * than pulling in a full HTML/CSS engine (i.e. a WebView), this is a tiny, purpose-built
 * tag-to-AnnotatedString converter plus a clickable-link renderer.
 */
private const val LINK_TAG = "URL"

fun parseInlineHtml(html: String, linkColor: Color, codeColor: Color): AnnotatedString {
    val text = html.replace("<br>", "\n").replace("<br/>", "\n").replace("<br />", "\n")
    return buildAnnotatedString {
        var i = 0
        val n = text.length
        val styleStack = ArrayDeque<String>()
        var pendingLinkUrl: String? = null

        fun pushStyleFor(tag: String) {
            when (tag) {
                "strong", "b" -> pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                "em", "i" -> pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                "code" -> pushStyle(
                    SpanStyle(fontFamily = FontFamily.Monospace, color = codeColor, background = codeColor.copy(alpha = 0.12f))
                )
            }
        }

        while (i < n) {
            if (text[i] == '<') {
                val close = text.indexOf('>', i)
                if (close == -1) { append(text.substring(i)); break }
                val tagContent = text.substring(i + 1, close)
                val isClosing = tagContent.startsWith("/")
                val tagName = tagContent.removePrefix("/").trim().substringBefore(' ').lowercase()

                if (!isClosing) {
                    when (tagName) {
                        "a" -> {
                            val hrefMatch = Regex("href=[\"']([^\"']*)[\"']").find(tagContent)
                            pendingLinkUrl = hrefMatch?.groupValues?.get(1)
                            pushStringAnnotation(LINK_TAG, pendingLinkUrl ?: "")
                            pushStyle(SpanStyle(color = linkColor, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))
                            styleStack.addLast("a")
                        }
                        "strong", "b", "em", "i", "code" -> {
                            pushStyleFor(tagName)
                            styleStack.addLast(tagName)
                        }
                        // Unknown/unsupported tags are simply skipped (not rendered as literal text).
                    }
                } else {
                    if (styleStack.isNotEmpty() && (styleStack.last() == tagName ||
                            (tagName == "b" && styleStack.last() == "strong") ||
                            (tagName == "i" && styleStack.last() == "em"))
                    ) {
                        styleStack.removeLast()
                        pop()
                        if (tagName == "a") pendingLinkUrl = null
                    }
                }
                i = close + 1
            } else {
                val next = text.indexOf('<', i).let { if (it == -1) n else it }
                append(text.substring(i, next))
                i = next
            }
        }
    }
}

/**
 * Renders documentation text that may contain the limited inline HTML above, with clickable
 * links opening in the device's browser.
 */
@Composable
fun InlineHtmlText(
    html: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    linkColor: Color = Color(0xFF4A9EFF),
    codeColor: Color = Color(0xFF4A9EFF),
    fontSize: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    fontFamily: FontFamily? = null
) {
    val annotated = parseInlineHtml(html, linkColor, codeColor)
    val uriHandler = LocalUriHandler.current
    val hasLinks = annotated.getStringAnnotations(LINK_TAG, 0, annotated.length).isNotEmpty()

    val baseStyle = LocalTextStyle.current.merge(
        TextStyle(color = color, fontSize = fontSize, lineHeight = lineHeight, fontFamily = fontFamily)
    )

    if (hasLinks) {
        ClickableText(
            text = annotated,
            modifier = modifier,
            style = baseStyle,
            onClick = { offset ->
                annotated.getStringAnnotations(LINK_TAG, offset, offset).firstOrNull()?.let { ann ->
                    if (ann.item.isNotBlank()) {
                        try { uriHandler.openUri(ann.item) } catch (_: Exception) { }
                    }
                }
            }
        )
    } else {
        Text(text = annotated, modifier = modifier, style = baseStyle)
    }
}
