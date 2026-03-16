package com.hackeros.app.ui.screens

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
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
import java.io.InputStream
import java.net.URL

@Composable
fun WallpapersScreen(
    wallpapers: List<WallpaperItem>,
    translations: Translations
) {
    val theme = LocalAppTheme.current
    val t = translations
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedWallpaper by remember { mutableStateOf<WallpaperItem?>(null) }
    var downloading by remember { mutableStateOf(false) }

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

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(wallpapers) { wp ->
                WallpaperThumbnail(
                    wallpaper = wp,
                    hdLabel = t.hd_asset,
                    primaryColor = theme.primaryColor(),
                    onClick = { selectedWallpaper = wp }
                )
            }
        }
    }

    // Fullscreen preview modal
    selectedWallpaper?.let { wp ->
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
                        .clip(androidx.compose.foundation.shape.CircleShape)
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

                // Download button
                Button(
                    onClick = {
                        scope.launch {
                            downloading = true
                            downloadImage(context, wp.url, wp.name)
                            downloading = false
                            Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !downloading,
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor()),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(0.85f)
                        .padding(bottom = 40.dp)
                        .height(56.dp)
                ) {
                    if (downloading) {
                        CircularProgressIndicator(
                            color = theme.backgroundColor(),
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(t.downloading, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            color = theme.backgroundColor())
                    } else {
                        Icon(Icons.Default.Download, null, tint = theme.backgroundColor(),
                            modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(t.download_save, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            color = theme.backgroundColor())
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

private suspend fun downloadImage(context: Context, url: String, name: String) {
    withContext(Dispatchers.IO) {
        try {
            val stream: InputStream = URL(url).openStream()
            val bytes = stream.readBytes()
            stream.close()

            val filename = "HackerOS_${name.replace(" ", "_")}_${System.currentTimeMillis()}.png"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { os -> os.write(bytes) }
                }
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                java.io.File(dir, filename).writeBytes(bytes)
            }
        } catch (_: Exception) {}
    }
}
