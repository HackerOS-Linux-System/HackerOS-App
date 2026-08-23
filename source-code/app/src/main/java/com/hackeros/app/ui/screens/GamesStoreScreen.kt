package com.hackeros.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.hackeros.app.MainViewModel
import com.hackeros.app.data.games.CommunityGame
import com.hackeros.app.ui.theme.LocalAppTheme
import com.hackeros.app.ui.theme.backgroundColor
import com.hackeros.app.ui.theme.cardColor
import com.hackeros.app.ui.theme.mutedColor
import com.hackeros.app.ui.theme.primaryColor
import com.hackeros.app.ui.theme.textColor
import com.hackeros.app.utils.Translations
import kotlin.math.roundToInt

@Composable
fun GamesStoreScreen(
    games: List<CommunityGame>,
    loading: Boolean,
    error: Boolean,
    fromCache: Boolean,
    installStates: Map<String, MainViewModel.ApkUpdateState>,
    isGameInstalled: (String?) -> Boolean,
    onDownload: (CommunityGame) -> Unit,
    onInstall: (String) -> Unit,
    onOpen: (String?) -> Unit,
    translations: Translations,
    onRetry: () -> Unit
) {
    val theme = LocalAppTheme.current
    val t = translations
    var selectedGame by remember { mutableStateOf<CommunityGame?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val categories = remember(games) { games.map { it.category }.distinct().sorted() }
    val filtered = games.filter { game ->
        (selectedCategory == null || game.category == selectedCategory) &&
            (searchQuery.isBlank() ||
                game.name.contains(searchQuery, ignoreCase = true) ||
                game.shortDescription.contains(searchQuery, ignoreCase = true))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 10.dp)) {
            Text(
                text = t.header_games_store,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = Color.White
            )
            Text(
                text = t.sub_games_store,
                fontSize = 13.sp,
                color = theme.mutedColor(),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = theme.primaryColor())
            }
            error -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.WifiOff, null, tint = Color(0xFFEF4444), modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(t.error_signal, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor())) {
                        Icon(Icons.Default.Refresh, null, tint = theme.backgroundColor(), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(t.retry, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = theme.backgroundColor())
                    }
                }
            }
            games.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SportsEsports, null, tint = theme.mutedColor().copy(alpha = 0.2f), modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(t.games_store_empty, color = theme.mutedColor(), fontSize = 13.sp)
                }
            }
            else -> {
                if (fromCache) {
                    Box(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                        OfflineBanner(text = t.offline_cached_banner, primaryColor = theme.primaryColor())
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 8.dp),
                    placeholder = { Text(t.games_store_search_placeholder, fontSize = 13.sp, color = theme.mutedColor()) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = theme.mutedColor(), modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.primaryColor(),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                    )
                )

                if (categories.size > 1) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        item {
                            CategoryChip(t.games_store_category_all, selectedCategory == null, theme.primaryColor(), theme.mutedColor()) {
                                selectedCategory = null
                            }
                        }
                        items(categories) { cat ->
                            CategoryChip(cat, selectedCategory == cat, theme.primaryColor(), theme.mutedColor()) {
                                selectedCategory = cat
                            }
                        }
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { game ->
                        GameRow(
                            game = game,
                            installState = installStates[game.id],
                            installed = isGameInstalled(game.packageName),
                            translations = t,
                            onClick = { selectedGame = game }
                        )
                    }
                }
            }
        }
    }

    selectedGame?.let { game ->
        GameDetailDialog(
            game = game,
            installState = installStates[game.id],
            installed = isGameInstalled(game.packageName),
            translations = t,
            onDismiss = { selectedGame = null },
            onDownload = { onDownload(game) },
            onInstall = { onInstall(game.id) },
            onOpen = { onOpen(game.packageName) }
        )
    }
}

@Composable
private fun CategoryChip(label: String, active: Boolean, primary: Color, muted: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) primary else Color.White.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (active) Color.Black else muted)
    }
}

@Composable
private fun GameRow(
    game: CommunityGame,
    installState: MainViewModel.ApkUpdateState?,
    installed: Boolean,
    translations: Translations,
    onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    val t = translations
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(theme.cardColor())
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(theme.backgroundColor())
        ) {
            if (game.iconUrl.isNotBlank()) {
                AsyncImage(model = game.iconUrl, contentDescription = game.name,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Default.SportsEsports, null, tint = theme.mutedColor(),
                    modifier = Modifier.align(Alignment.Center).size(24.dp))
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(game.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = theme.textColor(), maxLines = 1)
            Text(
                "${t.games_store_by} ${game.author}",
                fontSize = 10.sp, color = theme.mutedColor(), maxLines = 1
            )
            if (game.shortDescription.isNotBlank()) {
                Text(game.shortDescription, fontSize = 11.sp, color = theme.mutedColor(), maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp))
            }
        }

        GameActionButton(
            small = true,
            installState = installState,
            installed = installed,
            translations = t,
            onDownload = { },
            onInstall = { },
            onOpen = { },
            disabled = true
        )
    }
}

@Composable
private fun GameActionButton(
    small: Boolean,
    installState: MainViewModel.ApkUpdateState?,
    installed: Boolean,
    translations: Translations,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onOpen: () -> Unit,
    disabled: Boolean = false
) {
    val theme = LocalAppTheme.current
    val t = translations
    val height = if (small) 32.dp else 46.dp
    val fontSize = if (small) 10.sp else 12.sp

    when {
        installState is MainViewModel.ApkUpdateState.Downloading -> {
            Box(
                modifier = Modifier.size(if (small) 32.dp else 46.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { installState.progress / 100f },
                    color = theme.primaryColor(), strokeWidth = 2.dp,
                    modifier = Modifier.size(if (small) 24.dp else 32.dp)
                )
                if (!small) Text("${installState.progress}", fontSize = 9.sp, color = theme.primaryColor())
            }
        }
        installState is MainViewModel.ApkUpdateState.Verifying -> {
            CircularProgressIndicator(color = theme.primaryColor(), strokeWidth = 2.dp,
                modifier = Modifier.size(if (small) 24.dp else 32.dp))
        }
        installState is MainViewModel.ApkUpdateState.ReadyToInstall -> {
            Button(
                onClick = onInstall, enabled = !disabled,
                colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor()),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.height(height)
            ) {
                Text(t.games_store_install, fontWeight = FontWeight.Bold, fontSize = fontSize, color = theme.backgroundColor())
            }
        }
        installed -> {
            Button(
                onClick = onOpen, enabled = !disabled,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.height(height)
            ) {
                Text(t.games_store_open, fontWeight = FontWeight.Bold, fontSize = fontSize, color = theme.textColor())
            }
        }
        else -> {
            Button(
                onClick = onDownload, enabled = !disabled,
                colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor()),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.height(height)
            ) {
                Text(t.games_store_install, fontWeight = FontWeight.Bold, fontSize = fontSize, color = theme.backgroundColor())
            }
        }
    }
}

@Composable
private fun GameDetailDialog(
    game: CommunityGame,
    installState: MainViewModel.ApkUpdateState?,
    installed: Boolean,
    translations: Translations,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onOpen: () -> Unit
) {
    val theme = LocalAppTheme.current
    val t = translations

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .background(theme.cardColor())
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(theme.backgroundColor())
                    ) {
                        if (game.iconUrl.isNotBlank()) {
                            AsyncImage(model = game.iconUrl, contentDescription = game.name,
                                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Icon(Icons.Default.SportsEsports, null, tint = theme.mutedColor(),
                                modifier = Modifier.align(Alignment.Center).size(28.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(game.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = theme.textColor())
                        Text("${t.games_store_by} ${game.author}", fontSize = 11.sp, color = theme.mutedColor())
                        if (game.rating > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, null, tint = Color(0xFFFACC15), modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("${(game.rating * 10).roundToInt() / 10f}", fontSize = 11.sp, color = theme.mutedColor())
                            }
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = theme.mutedColor())
                    }
                }

                if (game.screenshots.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(game.screenshots) { shot ->
                            AsyncImage(
                                model = shot, contentDescription = null, contentScale = ContentScale.Crop,
                                modifier = Modifier.size(width = 140.dp, height = 90.dp).clip(RoundedCornerShape(10.dp))
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    game.description.ifBlank { game.shortDescription },
                    fontSize = 13.sp, color = theme.textColor(), lineHeight = 18.sp,
                    modifier = Modifier.heightIn(max = 140.dp)
                )

                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    MetaLabel(t.games_store_version_label, game.version, theme.mutedColor(), theme.textColor())
                    if (game.downloadSize > 0) {
                        MetaLabel(t.games_store_size_label, formatSize(game.downloadSize), theme.mutedColor(), theme.textColor())
                    }
                }

                Spacer(Modifier.height(18.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    GameActionButton(
                        small = false,
                        installState = installState,
                        installed = installed,
                        translations = t,
                        onDownload = onDownload,
                        onInstall = onInstall,
                        onOpen = onOpen
                    )
                }
                if (installState is MainViewModel.ApkUpdateState.ReadyToInstall && !installState.verified) {
                    Spacer(Modifier.height(8.dp))
                    Text("⚠ " + t.update_unverified, fontSize = 9.sp, color = Color(0xFFF59E0B))
                }
                if (installState is MainViewModel.ApkUpdateState.Error) {
                    Spacer(Modifier.height(8.dp))
                    Text(t.wallpaper_install_error, fontSize = 10.sp, color = Color(0xFFEF4444))
                }
            }
        }
    }
}

@Composable
private fun MetaLabel(label: String, value: String, mutedColor: Color, textColor: Color) {
    Column {
        Text(label, fontSize = 9.sp, color = mutedColor)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}

private fun formatSize(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1) "%.1f MB".format(mb) else "%.0f KB".format(bytes / 1024.0)
}
