package com.hackeros.app.ui.screens

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.hackeros.app.WallpaperItem
import com.hackeros.app.ui.theme.LocalAppTheme
import com.hackeros.app.ui.theme.backgroundColor
import com.hackeros.app.ui.theme.cardColor
import com.hackeros.app.ui.theme.mutedColor
import com.hackeros.app.ui.theme.primaryColor
import com.hackeros.app.ui.theme.textColor
import com.hackeros.app.utils.Translations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Local install state for a single wallpaper item, tracked per-wallpaper by id. */
private sealed class WallpaperInstallState {
    object Idle : WallpaperInstallState()
    data class Installing(val progress: Int) : WallpaperInstallState()
    data class Installed(val bitmap: Bitmap) : WallpaperInstallState()
    object Error : WallpaperInstallState()
}

/** Which surface(s) to apply a wallpaper to, mirrors Android's WallpaperManager FLAG_* set. */
private enum class WallpaperTarget { HOME, LOCK, BOTH }

@Composable
fun WallpapersScreen(
    wallpapers: List<WallpaperItem>,
    loading: Boolean,
    error: Boolean,
    fromCache: Boolean,
    translations: Translations,
    onRetry: () -> Unit
) {
    val theme = LocalAppTheme.current
    val t = translations
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedWallpaper by remember { mutableStateOf<WallpaperItem?>(null) }

    // Per-wallpaper install state (download progress / installed bitmap / error), keyed by the
    // wallpaper's stable id (its GitHub blob sha as of v0.7, so it stays correct across refetches
    // even if the folder's file list is reordered - unlike a plain positional index).
    val installStates = remember { mutableStateMapOf<String, WallpaperInstallState>() }
    // When non-null, shows the "where should this wallpaper be set?" target-picker dialog.
    var pendingSetWallpaper by remember { mutableStateOf<Pair<WallpaperItem, Bitmap>?>(null) }
    var settingWallpaper by remember { mutableStateOf(false) }

    // Restore previously-installed wallpapers from the local on-disk cache on first composition,
    // so a wallpaper installed while online is still viewable/settable entirely offline later -
    // this is the local cache for wallpapers (releases/gallery use DataStore; a raw file cache
    // is the natural fit here since the cached data is the image bytes themselves).
    LaunchedEffect(wallpapers) {
        withContext(Dispatchers.IO) {
            wallpapers.forEach { wp ->
                val cached = wallpaperCacheFile(context, wp.id)
                if (cached.exists()) {
                    val bitmap = BitmapFactory.decodeFile(cached.absolutePath)
                    if (bitmap != null) {
                        installStates[wp.id] = WallpaperInstallState.Installed(bitmap)
                    }
                }
            }
        }
    }

    fun installWallpaper(wp: WallpaperItem) {
        scope.launch {
            installStates[wp.id] = WallpaperInstallState.Installing(0)
            try {
                val bitmap = downloadBitmapWithProgress(wp.url) { percent ->
                    installStates[wp.id] = WallpaperInstallState.Installing(percent)
                }
                if (bitmap != null) {
                    // Also save a copy to the device's gallery, same as the existing "save"
                    // flow, so the install is visible outside the app too (e.g. in Photos).
                    saveBitmapToGallery(context, bitmap, wp.name)
                    // And cache it in app-private storage so it's available fully offline on a
                    // future visit (see the LaunchedEffect above that restores from this cache).
                    withContext(Dispatchers.IO) { cacheWallpaperLocally(context, wp.id, bitmap) }
                    installStates[wp.id] = WallpaperInstallState.Installed(bitmap)
                } else {
                    installStates[wp.id] = WallpaperInstallState.Error
                }
            } catch (_: Exception) {
                installStates[wp.id] = WallpaperInstallState.Error
            }
        }
    }

    fun applyWallpaper(bitmap: Bitmap, target: WallpaperTarget) {
        scope.launch {
            settingWallpaper = true
            val success = withContext(Dispatchers.IO) {
                try {
                    val wm = WallpaperManager.getInstance(context)
                    val flags = when (target) {
                        WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
                        WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
                        WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                    }
                    wm.setBitmap(bitmap, null, true, flags)
                    true
                } catch (_: Exception) {
                    false
                }
            }
            settingWallpaper = false
            pendingSetWallpaper = null
            Toast.makeText(
                context,
                if (success) t.wallpaper_set_success else t.wallpaper_set_error,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 20.dp)) {
            Text(
                text = t.header_wallpapers,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = Color.White
            )
            Text(
                text = t.sub_wallpapers,
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
            wallpapers.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Wallpaper, null, tint = theme.mutedColor().copy(alpha = 0.2f), modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(t.wallpapers_empty, color = theme.mutedColor(), fontSize = 13.sp)
                }
            }
            else -> {
                if (fromCache) {
                    Box(Modifier.padding(start = 20.dp, top = 0.dp, end = 20.dp, bottom = 12.dp)) {
                        OfflineBanner(text = t.offline_cached_banner, primaryColor = theme.primaryColor())
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(wallpapers, key = { it.id }) { wp ->
                        WallpaperThumbnail(
                            wallpaper = wp,
                            hdLabel = t.hd_asset,
                            primaryColor = theme.primaryColor(),
                            installState = installStates[wp.id] ?: WallpaperInstallState.Idle,
                            onClick = { selectedWallpaper = wp }
                        )
                    }
                }
            }
        }
    }

    // Fullscreen preview modal
    selectedWallpaper?.let { wp ->
        val state = installStates[wp.id] ?: WallpaperInstallState.Idle
        Dialog(
            onDismissRequest = { selectedWallpaper = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.96f)),
                contentAlignment = Alignment.Center
            ) {
                // Close button
                IconButton(
                    onClick = { selectedWallpaper = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.8f))
                }

                // Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(9f / 16f)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                ) {
                    AsyncImage(
                        model = wp.url,
                        contentDescription = wp.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Action area: install-with-progress -> set-as-default, stacked at the bottom.
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(0.85f)
                        .padding(bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (state) {
                        is WallpaperInstallState.Idle, is WallpaperInstallState.Error -> {
                            if (state is WallpaperInstallState.Error) {
                                Text(
                                    text = t.wallpaper_install_error,
                                    color = Color(0xFFEF4444),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                            Button(
                                onClick = { installWallpaper(wp) },
                                colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor()),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Icon(Icons.Default.Download, null, tint = theme.backgroundColor(),
                                    modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(t.wallpaper_install, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                                    color = theme.backgroundColor())
                            }
                        }
                        is WallpaperInstallState.Installing -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(theme.cardColor())
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        t.wallpaper_installing,
                                        color = theme.textColor(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        "${state.progress}%",
                                        color = theme.primaryColor(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = { state.progress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = theme.primaryColor(),
                                    trackColor = Color.White.copy(alpha = 0.08f)
                                )
                            }
                        }
                        is WallpaperInstallState.Installed -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = theme.primaryColor(),
                                    modifier = Modifier.size(14.dp))
                                Text(
                                    t.wallpaper_installed,
                                    color = theme.primaryColor(),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Button(
                                onClick = { pendingSetWallpaper = wp to state.bitmap },
                                colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor()),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Icon(Icons.Default.Wallpaper, null, tint = theme.backgroundColor(),
                                    modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(t.wallpaper_set_default, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                                    color = theme.backgroundColor())
                            }
                        }
                    }
                }
            }
        }
    }

    // "Where should this wallpaper be set?" target picker
    pendingSetWallpaper?.let { (wp, bitmap) ->
        Dialog(onDismissRequest = { if (!settingWallpaper) pendingSetWallpaper = null }) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(theme.cardColor())
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Wallpaper, null, tint = theme.primaryColor(),
                        modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        t.wallpaper_choose_target_title,
                        color = theme.textColor(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(Modifier.height(20.dp))

                    if (settingWallpaper) {
                        CircularProgressIndicator(color = theme.primaryColor(), modifier = Modifier.size(32.dp))
                    } else {
                        val targets = listOf(
                            Triple(WallpaperTarget.HOME, Icons.Default.Home, t.wallpaper_target_home),
                            Triple(WallpaperTarget.LOCK, Icons.Default.Lock, t.wallpaper_target_lock),
                            Triple(WallpaperTarget.BOTH, Icons.Default.PhoneAndroid, t.wallpaper_target_both),
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            targets.forEach { (target, icon, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.04f))
                                        .clickable { applyWallpaper(bitmap, target) }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(icon, null, tint = theme.primaryColor(), modifier = Modifier.size(18.dp))
                                    Text(label, color = theme.textColor(), fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { pendingSetWallpaper = null }) {
                            Text(t.wallpaper_target_cancel, color = theme.mutedColor(), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WallpaperThumbnail(
    wallpaper: WallpaperItem,
    hdLabel: String,
    primaryColor: Color,
    installState: WallpaperInstallState,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = wallpaper.url,
            contentDescription = wallpaper.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                        startY = 200f
                    )
                )
        )

        // "Installed" badge, shown once this wallpaper has been downloaded successfully.
        AnimatedVisibility(
            visible = installState is WallpaperInstallState.Installed,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(4.dp)
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = primaryColor, modifier = Modifier.size(14.dp))
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            Text(
                text = wallpaper.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Fullscreen, null,
                    tint = primaryColor, modifier = Modifier.size(9.dp))
                Text(
                    text = hdLabel,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = primaryColor
                )
            }
        }
    }
}

/**
 * Downloads [url] into memory, reporting integer 0-100 progress via [onProgress] as bytes
 * arrive (based on the response's Content-Length), then decodes and returns the resulting
 * Bitmap. Returns null if decoding fails. [onProgress] is invoked from a background thread;
 * Compose's MutableState is snapshot-safe for writes from any thread, so callers can update UI
 * state directly from it.
 */
private suspend fun downloadBitmapWithProgress(
    url: String,
    onProgress: (Int) -> Unit
): Bitmap? = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
        connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            connect()
        }
        val contentLength = connection.contentLength
        val output = ByteArrayOutputStream()
        connection.inputStream.use { input ->
            val buffer = ByteArray(8 * 1024)
            var totalRead = 0
            var lastPercent = -1
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                output.write(buffer, 0, read)
                totalRead += read
                if (contentLength > 0) {
                    val percent = ((totalRead.toLong() * 100) / contentLength).toInt().coerceIn(0, 100)
                    if (percent != lastPercent) {
                        lastPercent = percent
                        onProgress(percent)
                    }
                }
            }
        }
        onProgress(100)
        val bytes = output.toByteArray()
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
        null
    } finally {
        connection?.disconnect()
    }
}

/** Local on-disk cache file for a given wallpaper id, under the app's private files dir. */
private fun wallpaperCacheFile(context: Context, wallpaperId: String): File =
    File(context.filesDir, "wallpapers").apply { mkdirs() }.let { File(it, "$wallpaperId.png") }

private fun cacheWallpaperLocally(context: Context, wallpaperId: String, bitmap: Bitmap) {
    try {
        wallpaperCacheFile(context, wallpaperId).outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    } catch (_: Exception) {
        // Non-fatal: offline availability is a bonus, not a hard requirement for installing.
    }
}

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, name: String) {
    try {
        val filename = "HackerOS_${name.replace(" ", "_")}_${System.currentTimeMillis()}.png"
        val bytesOut = ByteArrayOutputStream().apply {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, this)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/HackerOS")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { os -> os.write(bytesOut.toByteArray()) }
            }
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            dir.mkdirs()
            java.io.File(dir, filename).writeBytes(bytesOut.toByteArray())
        }
    } catch (_: Exception) {
        // Non-fatal: the in-memory bitmap is still available for "set as wallpaper" even if
        // the gallery copy fails (e.g. due to storage restrictions on some OEM skins).
    }
}
