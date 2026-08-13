package com.hackeros.app.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AndroidColor
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.hackeros.app.Constants
import com.hackeros.app.data.model.Language
import com.hackeros.app.ui.theme.LocalAppTheme
import com.hackeros.app.ui.theme.backgroundColor
import com.hackeros.app.ui.theme.cardColor
import com.hackeros.app.ui.theme.mutedColor
import com.hackeros.app.ui.theme.primaryColor
import com.hackeros.app.ui.theme.textColor
import com.hackeros.app.utils.Translations
import org.json.JSONObject

/**
 * Shows the official HackerOS documentation natively, in-app - i.e. embedded directly in the
 * app's own UI (with the app's header/nav chrome around it, plus a native search bar) rather
 * than handing the user off to an external browser via an Intent. Under the hood it renders the
 * exact same live page the website serves (https://.../hackeros-documentation.html), which keeps
 * the docs content always perfectly in sync with the website with zero duplicated content.
 *
 * Two extra pieces of native integration on top of the plain WebView:
 *  - The WebView's HTTP cache is configured so a page visited once while online can still be
 *    re-opened (cache-only) while offline, instead of showing a blank error.
 *  - A native search field sits above the WebView and drives the site's own client-side search
 *    engine (`translations/doc-engine.js`'s `doSearch()` / `#search-input`) via `evaluateJavascript`,
 *    rather than reimplementing search - it also keeps the loaded page's language in sync with
 *    the app's language via the site's own `window.__hackeros_applyLang()`.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DocumentationScreen(translations: Translations, currentLanguage: Language) {
    val theme = LocalAppTheme.current
    val t = translations
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val isEnglishOnlySection = currentLanguage.code !in Constants.DOCUMENTATION_CONTENT_LANGUAGES

    fun runSearch(query: String) {
        val webView = webViewRef ?: return
        val escaped = JSONObject.quote(query)
        webView.evaluateJavascript(
            """
            (function() {
                var el = document.getElementById('search-input');
                if (el) {
                    el.value = $escaped;
                    el.dispatchEvent(new Event('input', {bubbles:true}));
                } else if (window.doSearch) {
                    window.doSearch($escaped);
                }
            })();
            """.trimIndent(), null
        )
    }

    fun syncLanguage() {
        val webView = webViewRef ?: return
        val langCode = JSONObject.quote(currentLanguage.code)
        webView.evaluateJavascript(
            """
            (function() {
                if (window.HackerLang && window.HackerLang.setLang) window.HackerLang.setLang($langCode);
                if (window.__hackeros_applyLang) window.__hackeros_applyLang($langCode);
            })();
            """.trimIndent(), null
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 12.dp)) {
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

        // Native search bar, driving the site's own doc-engine.js search.
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                runSearch(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 10.dp),
            placeholder = { Text(t.doc_search_placeholder, fontSize = 13.sp, color = theme.mutedColor()) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = theme.mutedColor(), modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = ""; runSearch("") }) {
                        Icon(Icons.Default.Close, t.doc_search_clear, tint = theme.mutedColor(), modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { runSearch(searchQuery) }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = theme.primaryColor(),
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedTextColor = theme.textColor(),
                unfocusedTextColor = theme.textColor()
            )
        )

        if (isEnglishOnlySection) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 10.dp)
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

        Box(modifier = Modifier.fillMaxSize().padding(bottom = 90.dp)) {
            if (!hasError) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(AndroidColor.TRANSPARENT)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            // Enable the WebView's own HTTP cache so a page visited once while
                            // online remains available (cache-only) when there's no connection.
                            settings.cacheMode = if (isNetworkAvailable(ctx))
                                WebSettings.LOAD_DEFAULT else WebSettings.LOAD_CACHE_ELSE_NETWORK
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(
                                    view: WebView?, url: String?, favicon: android.graphics.Bitmap?
                                ) {
                                    isLoading = true
                                    hasError = false
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                    syncLanguage()
                                    if (searchQuery.isNotEmpty()) runSearch(searchQuery)
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    if (request?.isForMainFrame != false) {
                                        isLoading = false
                                        hasError = true
                                    }
                                }
                            }
                            loadUrl(Constants.DOCUMENTATION_URL)
                            webViewRef = this
                        }
                    },
                    update = { webView ->
                        webViewRef = webView
                    }
                )
            }

            if (isLoading && !hasError) {
                Box(
                    modifier = Modifier.fillMaxSize().background(theme.backgroundColor()),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = theme.primaryColor(), modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = t.decrypting,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = theme.primaryColor()
                        )
                    }
                }
            }

            if (hasError) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.WifiOff, null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(t.error_signal, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(t.error_network, color = theme.mutedColor(), fontSize = 12.sp)
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = {
                                hasError = false
                                isLoading = true
                                webViewRef?.loadUrl(Constants.DOCUMENTATION_URL)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor())
                        ) {
                            Icon(Icons.Default.Refresh, null, tint = theme.backgroundColor(), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(t.retry, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = theme.backgroundColor())
                        }
                    }
                }
            }
        }
    }
}

private fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
