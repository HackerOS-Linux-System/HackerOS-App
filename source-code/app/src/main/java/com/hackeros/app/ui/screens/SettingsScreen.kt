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
import androidx.compose.runtime.*
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
    watchedEditions: Set<String>?,
    knownEditions: List<String>,
    onToggleEdition: (String) -> Unit,
    onResetEditionFilter: () -> Unit,
    updateStatus: MainViewModel.UpdateStatus,
    remoteVersion: String,
    onCheckUpdate: () -> Unit,
    apkUpdateState: MainViewModel.ApkUpdateState,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    docsSectionEnabled: Boolean,
    onToggleDocsSection: (Boolean) -> Unit,
    gamesStoreSectionEnabled: Boolean,
    onToggleGamesStoreSection: (Boolean) -> Unit,
    customThemeColors: com.hackeros.app.data.model.AppTheme?,
    onSaveCustomTheme: (primary: Long, background: Long, card: Long) -> Unit,
    translations: Translations
) {
    val theme = LocalAppTheme.current
    val t = translations
    val context = LocalContext.current
    var showCustomThemeDialog by remember { mutableStateOf(false) }

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
            val langs = Language.entries.map { it to (it.flag to it.displayName) }
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

                Spacer(Modifier.height(8.dp))

                // Custom theme: either a "create your own" prompt, or (once created) a
                // selectable tile like the built-in themes plus an edit affordance.
                if (customThemeColors != null) {
                    val isCustomSelected = currentTheme == com.hackeros.app.data.model.ThemeId.CUSTOM
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isCustomSelected) theme.primaryColor().copy(0.1f) else Color.White.copy(0.03f))
                                .border(
                                    1.dp,
                                    if (isCustomSelected) theme.primaryColor() else Color.White.copy(0.05f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onThemeChange(com.hackeros.app.data.model.ThemeId.CUSTOM) }
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
                                            .background(Color(customThemeColors.primary))
                                    )
                                    if (isCustomSelected) {
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
                                    text = t.theme_custom_label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCustomSelected) Color.White else theme.mutedColor()
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(0.03f))
                                .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(12.dp))
                                .clickable { showCustomThemeDialog = true }
                                .padding(12.dp)
                                .align(Alignment.CenterVertically)
                        ) {
                            Icon(Icons.Default.Edit, null, tint = theme.mutedColor(), modifier = Modifier.size(20.dp))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(0.03f))
                            .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(12.dp))
                            .clickable { showCustomThemeDialog = true }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, null, tint = theme.primaryColor(), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(t.theme_create_custom, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = theme.primaryColor())
                    }
                }
            }
        }

        // Socials
        SectionCard(title = t.settings_social, icon = Icons.Default.Language, sectionBg, sectionBorder, cardShape, theme.primaryColor()) {
            val links = listOf(
                Triple("Discord Community", t.social_join_server, "https://discord.com/invite/8yHNcBaEKy"),
                Triple("X / Twitter", "@hackeros_linux", "https://x.com/hackeros_linux"),
                Triple("Linuxiarze.pl", null, "https://linuxiarze.pl/distro-hackeros/"),
                Triple("DistroWatch", null, "https://distrowatch.com/table.php?distribution=hackeros"),
                Triple("Reddit", "r/HackerOS_", "https://www.reddit.com/r/HackerOS_/"),
                Triple("YouTube", t.social_official_channel, "https://www.youtube.com/channel/UCB_b48f2diMH2JByN2OmgGw"),
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

                // Per-edition notification filter, only relevant once notifications are on.
                if (notificationsEnabled && knownEditions.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Divider(color = Color.White.copy(alpha = 0.04f))
                    Spacer(Modifier.height(14.dp))
                    Text(t.pref_edition_filter_title, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = theme.textColor())
                    Text(t.pref_edition_filter_desc, fontSize = 10.sp, color = theme.mutedColor(),
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))

                    val isAll = watchedEditions == null
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isAll) theme.primaryColor().copy(0.1f) else Color.White.copy(0.03f))
                            .clickable { onResetEditionFilter() }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(t.pref_edition_filter_all, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                            color = if (isAll) Color.White else theme.mutedColor())
                        if (isAll) Icon(Icons.Default.Check, null, tint = theme.primaryColor(), modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    knownEditions.forEach { edition ->
                        val checked = if (isAll) true else watchedEditions?.contains(edition) == true
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onToggleEdition(edition) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(edition, fontSize = 12.sp, color = theme.textColor())
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { onToggleEdition(edition) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = theme.primaryColor(),
                                    uncheckedColor = theme.mutedColor()
                                )
                            )
                        }
                    }
                }
            }
        }

        // Sections visibility
        SectionCard(title = t.pref_sections_title, icon = Icons.Default.ViewModule, sectionBg, sectionBorder, cardShape, theme.primaryColor()) {
            Column {
                Text(t.pref_sections_desc, fontSize = 10.sp, color = theme.mutedColor(), modifier = Modifier.padding(bottom = 10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(t.pref_show_docs_section, fontSize = 12.sp, color = theme.textColor())
                    Switch(
                        checked = docsSectionEnabled,
                        onCheckedChange = onToggleDocsSection,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = theme.primaryColor(),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFF374151)
                        )
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(t.pref_show_games_store_section, fontSize = 12.sp, color = theme.textColor())
                    Switch(
                        checked = gamesStoreSectionEnabled,
                        onCheckedChange = onToggleGamesStoreSection,
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
                            Text(t.settings_source_code, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = theme.textColor())
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
                        MainViewModel.UpdateStatus.CHECKING -> StatusBadge(t.status_scanning, Color(0xFF10B981))
                        MainViewModel.UpdateStatus.UP_TO_DATE -> StatusBadge(t.status_latest, Color(0xFF22C55E))
                        MainViewModel.UpdateStatus.UPDATE_AVAILABLE -> StatusBadge(t.status_outdated, Color(0xFFF59E0B))
                        MainViewModel.UpdateStatus.ERROR -> StatusBadge(t.status_error, Color(0xFFEF4444))
                        else -> {}
                    }
                }

                Spacer(Modifier.height(4.dp))

                if (updateStatus == MainViewModel.UpdateStatus.UPDATE_AVAILABLE) {
                    UpdateFlowSection(
                        remoteVersion = remoteVersion,
                        apkUpdateState = apkUpdateState,
                        onDownloadUpdate = onDownloadUpdate,
                        onInstallUpdate = onInstallUpdate,
                        translations = t,
                        theme = theme
                    )
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
            text = t.settings_tagline,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = theme.mutedColor().copy(alpha = 0.3f),
            letterSpacing = 3.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }

    if (showCustomThemeDialog) {
        com.hackeros.app.ui.components.CustomThemeDialog(
            initial = customThemeColors,
            translations = t,
            onDismiss = { showCustomThemeDialog = false },
            onSave = { primary, background, card ->
                onSaveCustomTheme(primary, background, card)
                showCustomThemeDialog = false
            }
        )
    }
}

@Composable
private fun UpdateFlowSection(
    remoteVersion: String,
    apkUpdateState: MainViewModel.ApkUpdateState,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    translations: Translations,
    theme: com.hackeros.app.data.model.AppTheme
) {
    val t = translations
    when (apkUpdateState) {
        is MainViewModel.ApkUpdateState.Idle -> {
            Button(
                onClick = onDownloadUpdate,
                colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor()),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Icon(Icons.Default.Download, null, tint = theme.backgroundColor(), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("${t.update_button_prefix} v$remoteVersion", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = theme.backgroundColor())
            }
        }
        is MainViewModel.ApkUpdateState.Downloading -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .padding(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(t.update_downloading, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace, color = theme.textColor())
                    Text("${apkUpdateState.progress}%", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace, color = theme.primaryColor())
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { apkUpdateState.progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = theme.primaryColor(),
                    trackColor = Color.White.copy(alpha = 0.08f)
                )
            }
        }
        is MainViewModel.ApkUpdateState.Verifying -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(color = theme.primaryColor(), modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(t.update_verifying, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = theme.textColor())
            }
        }
        is MainViewModel.ApkUpdateState.ReadyToInstall -> {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        if (apkUpdateState.verified) Icons.Default.VerifiedUser else Icons.Default.Info,
                        null,
                        tint = if (apkUpdateState.verified) Color(0xFF22C55E) else Color(0xFFF59E0B),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        if (apkUpdateState.verified) t.update_verified else t.update_unverified,
                        fontSize = 10.sp,
                        color = if (apkUpdateState.verified) Color(0xFF22C55E) else Color(0xFFF59E0B)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onInstallUpdate,
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor()),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Icon(Icons.Default.InstallMobile, null, tint = theme.backgroundColor(), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(t.update_install, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = theme.backgroundColor())
                }
            }
        }
        is MainViewModel.ApkUpdateState.Error -> {
            Column {
                Text(t.update_error, color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDownloadUpdate,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(t.update_retry, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
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
