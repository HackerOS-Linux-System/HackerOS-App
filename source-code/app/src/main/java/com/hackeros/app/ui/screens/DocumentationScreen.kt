package com.hackeros.app.ui.screens

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.hackeros.app.Constants
import com.hackeros.app.ui.theme.LocalAppTheme
import com.hackeros.app.ui.theme.backgroundColor
import com.hackeros.app.ui.theme.mutedColor
import com.hackeros.app.ui.theme.primaryColor
import com.hackeros.app.utils.Translations

/**
 * Shows the official HackerOS documentation natively, in-app - i.e. embedded directly in the
 * app's own UI (with the app's header/nav chrome around it) rather than handing the user off to
 * an external browser via an Intent. Under the hood it renders the exact same live page the
 * website serves (https://.../hackeros-documentation.html), which already includes its own
 * search, tab navigation, and language switching driven by translations/doc-engine.js. This
 * keeps the docs always perfectly in sync with the website with zero duplicated content.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DocumentationScreen(translations: Translations) {
    val theme = LocalAppTheme.current
    val t = translations
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

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
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(
                                    view: WebView?, url: String?, favicon: android.graphics.Bitmap?
                                ) {
                                    isLoading = true
                                    hasError = false
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
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
                                reloadKey++
                                webViewRef?.loadUrl(Constants.DOCUMENTATION_URL)
                                    ?: run { webViewRef = null }
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
