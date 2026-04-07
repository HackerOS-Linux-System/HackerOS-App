package com.hackeros.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackeros.app.data.model.ReleaseInfo
import com.hackeros.app.ui.theme.LocalAppTheme
import com.hackeros.app.ui.theme.cardColor
import com.hackeros.app.ui.theme.mutedColor
import com.hackeros.app.ui.theme.primaryColor
import com.hackeros.app.ui.theme.textColor
import com.hackeros.app.utils.Translations

@Composable
fun VersionCard(
    release: ReleaseInfo,
    isLatest: Boolean,
    translations: Translations
) {
    val theme = LocalAppTheme.current
    val t = translations

    fun parseEditionLine(line: String): Pair<String, String> {
        val idx = line.indexOf(':')
        return if (idx != -1) line.substring(0, idx).trim() to line.substring(idx + 1).trim()
        else line to ""
    }

    val editionsList = if (release.editions.isNotEmpty())
    release.editions.split("\n").map { parseEditionLine(it) }
    else emptyList()

        val infiniteTransition = rememberInfiniteTransition(label = "ping")
        val pingAlpha by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                                               repeatMode = RepeatMode.Restart
            ),
            label = "pingAlpha"
        )

        val cardBorderColor = if (isLatest) {
            theme.primaryColor().copy(alpha = 0.4f)
        } else {
            Color.White.copy(alpha = 0.05f)
        }
        val cardBg = if (isLatest) {
            theme.cardColor().copy(alpha = 0.7f)
        } else {
            theme.cardColor().copy(alpha = 0.35f)
        }

        Box(
            modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(1.dp, cardBorderColor, RoundedCornerShape(20.dp))
        ) {
            Column {
                // Latest banner
                if (isLatest) {
                    Row(
                        modifier = Modifier
                        .fillMaxWidth()
                        .background(theme.primaryColor().copy(alpha = 0.1f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(theme.primaryColor().copy(alpha = pingAlpha))
                                )
                                Box(
                                    modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(theme.primaryColor())
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = t.latest_build.uppercase(),
                                 fontSize = 9.sp,
                                 fontFamily = FontFamily.Monospace,
                                 fontWeight = FontWeight.Bold,
                                 color = theme.primaryColor(),
                                 letterSpacing = 1.sp
                            )
                        }
                    }
                    HorizontalDivider(color = theme.primaryColor().copy(alpha = 0.1f), thickness = 1.dp)
                }

                Column(modifier = Modifier.padding(20.dp)) {
                    // Version header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val iconBg = if (isLatest) theme.primaryColor() else Color.White.copy(alpha = 0.05f)
                        val iconTint = if (isLatest) theme.cardColor() else theme.mutedColor()

                        Box(
                            modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(iconBg)
                            .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                 contentDescription = null,
                                 tint = iconTint,
                                 modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = release.version,
                                 fontFamily = FontFamily.Monospace,
                                 fontWeight = FontWeight.Bold,
                                 fontSize = 22.sp,
                                 color = theme.textColor()
                            )
                            if (release.description.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                         contentDescription = null,
                                         tint = theme.mutedColor().copy(alpha = 0.7f),
                                         modifier = Modifier
                                         .size(14.dp)
                                         .padding(top = 2.dp)
                                    )
                                    Text(
                                        text = release.description,
                                         fontSize = 13.sp,
                                         color = theme.mutedColor().copy(alpha = 0.9f),
                                         lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }

                    // Editions / Roadmap
                    if (editionsList.isNotEmpty()) {
                        Spacer(Modifier.height(20.dp))
                        Column(
                            modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(theme.cardColor().copy(alpha = 0.5f))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.04f))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CalendarToday, null,
                                     tint = theme.primaryColor(), modifier = Modifier.size(12.dp))
                                Text(
                                    text = t.roadmap.uppercase(),
                                     fontSize = 9.sp,
                                     fontFamily = FontFamily.Monospace,
                                     fontWeight = FontWeight.Bold,
                                     color = theme.mutedColor(),
                                     letterSpacing = 1.sp
                                )
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                            editionsList.forEachIndexed { idx, (name, date) ->
                                if (idx > 0) HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                    Row(
                                        modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(theme.primaryColor().copy(alpha = 0.5f))
                                            )
                                            Text(
                                                text = name,
                                                 fontSize = 12.sp,
                                                 fontWeight = FontWeight.Medium,
                                                 color = theme.textColor()
                                            )
                                        }
                                        if (date.isNotEmpty()) {
                                            Row(
                                                modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(theme.cardColor().copy(alpha = 0.7f))
                                                .border(
                                                    1.dp,
                                                    theme.primaryColor().copy(alpha = 0.15f),
                                                        RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Schedule, null,
                                                     tint = theme.primaryColor(),
                                                     modifier = Modifier.size(10.dp)
                                                )
                                                Text(
                                                    text = date,
                                                     fontSize = 10.sp,
                                                     fontFamily = FontFamily.Monospace,
                                                     color = theme.primaryColor()
                                                )
                                            }
                                        }
                                    }
                            }
                        }
                    }

                    // Changelog
                    Spacer(Modifier.height(20.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                    ) {
                        Icon(Icons.Default.ShowChart, null,
                             tint = theme.mutedColor().copy(alpha = 0.5f),
                             modifier = Modifier.size(12.dp))
                        Text(
                            text = t.changelog.uppercase(),
                             fontSize = 9.sp,
                             fontFamily = FontFamily.Monospace,
                             fontWeight = FontWeight.Bold,
                             color = theme.mutedColor().copy(alpha = 0.5f),
                             letterSpacing = 1.sp
                        )
                    }

                    Column(
                        modifier = Modifier.padding(start = 22.dp),
                           verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (release.news.isNotEmpty()) {
                            release.news.split("\n").forEach { line ->
                                Text(
                                    text = "• $line",
                                     fontSize = 12.sp,
                                     fontFamily = FontFamily.Monospace,
                                     color = theme.textColor().copy(alpha = 0.8f),
                                     lineHeight = 18.sp
                                )
                            }
                        } else {
                            Text(
                                text = t.no_changes,
                                 fontSize = 12.sp,
                                 color = theme.mutedColor(),
                                 fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }
}
