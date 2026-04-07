package com.hackeros.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackeros.app.data.model.ReleaseInfo
import com.hackeros.app.ui.components.VersionCard
import com.hackeros.app.ui.theme.LocalAppTheme
import com.hackeros.app.ui.theme.mutedColor
import com.hackeros.app.ui.theme.primaryColor
import com.hackeros.app.utils.Translations

@Composable
fun ReleasesScreen(
    releases: List<ReleaseInfo>,
    loading: Boolean,
    error: String?,
    translations: Translations,
    onRetry: () -> Unit
) {
    val theme = LocalAppTheme.current
    val t = translations

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
               verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "HackerOS",
                     fontFamily = FontFamily.Monospace,
                     fontWeight = FontWeight.Bold,
                     fontSize = 28.sp,
                     color = Color.White
                )
                Spacer(Modifier.width(12.dp))
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                        color = theme.primaryColor().copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, theme.primaryColor().copy(alpha = 0.5f)
                        )
                ) {
                    Text(
                        text = t.sub_releases,
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

        when {
            loading -> item {
                LoadingState(theme.primaryColor(), t.decrypting)
            }
            error != null -> item {
                ErrorState(
                    errorSignal = t.error_signal,
                    errorNetwork = t.error_network,
                    retry = t.retry,
                    onRetry = onRetry
                )
            }
            else -> {
                itemsIndexed(releases) { index, release ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(300 + index * 80)) +
                        slideInVertically(tween(300 + index * 80)) { it / 2 }
                    ) {
                        VersionCard(
                            release = release,
                            isLatest = index == 0,
                            translations = t
                        )
                    }
                }
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                    ) {
                        Icon(
                            Icons.Default.Terminal,
                             contentDescription = null,
                             tint = theme.mutedColor().copy(alpha = 0.4f),
                             modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = t.end_of_log.uppercase(),
                             fontSize = 9.sp,
                             fontFamily = FontFamily.Monospace,
                             color = theme.mutedColor().copy(alpha = 0.4f),
                             letterSpacing = 2.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState(primaryColor: Color, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
        .fillMaxWidth()
        .height(300.dp),
           verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = primaryColor, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                                               repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        Text(
            text = label,
             fontSize = 11.sp,
             fontFamily = FontFamily.Monospace,
             color = primaryColor.copy(alpha = alpha)
        )
    }
}

@Composable
private fun ErrorState(
    errorSignal: String,
    errorNetwork: String,
    retry: String,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
        .fillMaxWidth()
        .height(300.dp)
        .padding(horizontal = 32.dp),
           verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = Color(0xFFEF4444).copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, Color(0xFFEF4444).copy(alpha = 0.2f)
                ),
                modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.WifiOff,
                     contentDescription = null,
                     tint = Color(0xFFEF4444),
                     modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(errorSignal, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Text(errorNetwork, color = Color(0xFF94A3B8), fontSize = 12.sp)
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
        ) {
            Text(retry, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}
