package com.hackeros.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackeros.app.Constants
import com.hackeros.app.MainViewModel
import com.hackeros.app.data.model.Language
import com.hackeros.app.data.model.ThemeId
import com.hackeros.app.ui.theme.*
import com.hackeros.app.utils.Translations

@Composable
fun SettingsScreen(
    currentTheme: ThemeId,
    onThemeChange: (ThemeId) -> Unit,
    currentLanguage: Language,
    onLanguageChange: (Language) -> Unit,
    notificationsEnabled: Boolean,
    onToggleNotifications: () -> Unit,
    updateStatus: MainViewModel.UpdateStatus,
    remoteVersion: String,
    onCheckUpdate: () -> Unit,
    translations: Translations
) {
    val theme = LocalAppTheme.current
    val t = translations
    val context = LocalContext.current

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    val cardShape = RoundedCornerShape(18.dp)
    val sectionBg = theme.cardColor().copy(alpha = 0.5f)
    val sectionBorder = Color.White.copy(alpha = 0.05f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp)) {
            Text(
                text = t.header_config,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = Color.White
            )
            Text(
                text = t.sub_config,
                fontSize = 13.sp,
                color = theme.mutedColor(),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Language
        SectionCard(title = t.settings_lang, icon = Icons.Default.Translate, sectionBg, sectionBorder, cardShape, theme.primaryColor()) {
            val langs = listOf(
                Language.PL to ("🇵🇱" to "Polski"),
                Language.EN to ("🇺🇸" to "English"),
                Language.DE to ("🇩🇪" to "Deutsch"),
                Language.ES to ("🇪🇸" to "Español"),
                Language.FR to ("🇫🇷" to "Français"),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                langs.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (lang, pair) ->
                            val (flag, name) = pair
                            val isSelected = currentLanguage == lang
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) theme.primaryColor().copy(0.1f) else Color.White.copy(0.03f))
                                    .border(1.dp,
                                        if (isSelected) theme.primaryColor() else Color.White.copy(0.05f),
                                        RoundedCornerShape(12.dp))
                                    .clickable { onLanguageChange(lang) }
                                    .padding(horizontal = 12.dp, vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(flag, fontSize = 18.sp)
                                    Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else theme.mutedColor())
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // Themes
        SectionCard(title = t.settings_theme, icon = Icons.Default.Palette, sectionBg, sectionBorder, cardShape, theme.primaryColor()) {
            val themeItems = THEMES.values.toList()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                themeItems.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { appTheme ->
                            val isSelected = currentTheme == appTheme.id
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) theme.primaryColor().copy(0.1f) else Color.White.copy(0.03f))
                                    .border(1.dp,
                                        if (isSelected) theme.primaryColor() else Color.White.copy(0.05f),
                                        RoundedCornerShape(12.dp))
                                    .clickable { onThemeChange(appTheme.id) }
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(appTheme.primaryColor())
                                        )
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                                    .background(theme.primaryColor())
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = appTheme.id.themeName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else theme.mutedColor()
                                    )
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // Socials
        SectionCard(title = t.settings_social, icon = Icons.Default.Language, sectionBg, sectionBorder, cardShape, theme.primaryColor()) {
            val links = listOf(
                Triple("Discord Community", "Join the server", "https://discord.com/invite/8yHNcBaEKy"),
                Triple("X / Twitter", "@hackeros_linux", "https://x.com/hackeros_linux"),
                Triple("Linuxiarze.pl", null, "https://linuxiarze.pl/distro-hackeros/"),
                Triple("DistroWatch", null, "https://distrowatch.com/table.php?distribution=hackeros"),
                Triple("Reddit", "r/HackerOS_", "https://www.reddit.com/r/HackerOS_/"),
                Triple("YouTube", "Official Channel", "https://www.youtube.com/channel/UCB_b48f2diMH2JByN2OmgGw"),
            )
            Column {
                links.forEachIndexed { idx, (label, sub, url) ->
                    if (idx > 0) Divider(color = Color.White.copy(alpha = 0.04f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openUrl(url) }
                            .padding(horizontal = 4.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = theme.textColor())
                            if (sub != null) Text(sub, fontSize = 11.sp, color = theme.mutedColor())
                        }
                        Icon(Icons.Default.OpenInNew, null, tint = theme.mutedColor().copy(0.5f),
                            modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Preferences
        SectionCard(title = t.settings_pref, icon = Icons.Default.Layers, sectionBg, sectionBorder, cardShape, theme.primaryColor()) {
            Column {
                // Notifications toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleNotifications() }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(theme.backgroundColor())
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Default.Notifications, null, tint = theme.mutedColor(),
                                modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(t.pref_notifications, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                color = theme.textColor())
                            Text(t.pref_notifications_desc, fontSize = 11.sp, color = theme.mutedColor())
                        }
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { onToggleNotifications() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = theme.primaryColor(),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFF374151)
                        )
                    )
                }
            }
        }

        // About / Updates
        SectionCard(title = t.settings_info, icon = Icons.Default.Info, sectionBg, sectionBorder, cardShape, theme.primaryColor()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Source Code link
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openUrl("https://github.com/HackerOS-Linux-System/HackerOS-App") }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(theme.backgroundColor()).padding(8.dp)) {
                            Icon(Icons.Default.Code, null, tint = theme.mutedColor(), modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text("Source Code", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = theme.textColor())
                            Text("HackerOS-App", fontSize = 11.sp, color = theme.mutedColor())
                        }
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = theme.mutedColor(), modifier = Modifier.size(16.dp))
                }

                Divider(color = Color.White.copy(alpha = 0.04f))

                // Version + update
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(theme.backgroundColor()).padding(8.dp)) {
                            Icon(Icons.Default.Shield, null, tint = theme.mutedColor(), modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(t.settings_version_current, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                color = theme.textColor())
                            Text("v${Constants.APP_VERSION}", fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace, color = theme.mutedColor())
                        }
                    }
                    // Status badge
                    when (updateStatus) {
                        MainViewModel.UpdateStatus.CHECKING -> StatusBadge("SCANNING...", Color(0xFF10B981))
                        MainViewModel.UpdateStatus.UP_TO_DATE -> StatusBadge("LATEST", Color(0xFF22C55E))
                        MainViewModel.UpdateStatus.UPDATE_AVAILABLE -> StatusBadge("OUTDATED", Color(0xFFF59E0B))
                        MainViewModel.UpdateStatus.ERROR -> StatusBadge("ERROR", Color(0xFFEF4444))
                        else -> {}
                    }
                }

                Spacer(Modifier.height(4.dp))

                if (updateStatus == MainViewModel.UpdateStatus.UPDATE_AVAILABLE) {
                    Button(
                        onClick = {
                            val apkUrl = "https://github.com/HackerOS-Linux-System/HackerOS-App/releases/download/v${remoteVersion}/HackerOS-App-${remoteVersion}.apk"
                            openUrl(apkUrl)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor()),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(Icons.Default.Download, null, tint = theme.backgroundColor(),
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("UPDATE TO v$remoteVersion", fontWeight = FontWeight.Bold,
                            fontSize = 12.sp, color = theme.backgroundColor())
                    }
                } else {
                    OutlinedButton(
                        onClick = onCheckUpdate,
                        enabled = updateStatus != MainViewModel.UpdateStatus.CHECKING,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.primaryColor())
                    ) {
                        if (updateStatus == MainViewModel.UpdateStatus.CHECKING) {
                            CircularProgressIndicator(color = theme.primaryColor(),
                                modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (updateStatus == MainViewModel.UpdateStatus.CHECKING)
                                t.settings_checking else t.settings_check_update,
                            fontWeight = FontWeight.Bold, fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Text(
            text = "Designed for Hackers",
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = theme.mutedColor().copy(alpha = 0.3f),
            letterSpacing = 3.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    bg: Color,
    border: Color,
    shape: RoundedCornerShape,
    primaryColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalAppTheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(icon, null, tint = primaryColor, modifier = Modifier.size(18.dp))
                Text(
                    text = title.uppercase(),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = theme.mutedColor(),
                    letterSpacing = 1.sp
                )
            }
            Divider(color = Color.White.copy(alpha = 0.04f))
            Column(modifier = Modifier.padding(20.dp)) {
                content()
            }
        }
    }
}
