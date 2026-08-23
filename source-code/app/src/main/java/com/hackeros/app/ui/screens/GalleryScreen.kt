package com.hackeros.app.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.hackeros.app.data.model.GalleryImage
import com.hackeros.app.ui.theme.LocalAppTheme
import com.hackeros.app.ui.theme.backgroundColor
import com.hackeros.app.ui.theme.mutedColor
import com.hackeros.app.ui.theme.primaryColor
import com.hackeros.app.utils.Translations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.URL

@Composable
fun GalleryScreen(
    images: List<GalleryImage>,
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
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var downloadingAll by remember { mutableStateOf(false) }
    var downloadAllProgress by remember { mutableStateOf(0 to 0) } // (done, total)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = t.header_gallery,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = Color.White
                )
                Text(
                    text = t.sub_gallery,
                    fontSize = 13.sp,
                    color = theme.mutedColor(),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (images.isNotEmpty()) {
                IconButton(
                    onClick = {
                        if (!downloadingAll) {
                            scope.launch {
                                downloadingAll = true
                                downloadAllProgress = 0 to images.size
                                images.forEachIndexed { idx, img ->
                                    downloadImageToGallery(context, img)
                                    downloadAllProgress = (idx + 1) to images.size
                                }
                                downloadingAll = false
                                Toast.makeText(context, t.gallery_download_all_done, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(theme.primaryColor().copy(alpha = 0.1f))
                ) {
                    if (downloadingAll) {
                        CircularProgressIndicator(color = theme.primaryColor(), modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Download, null, tint = theme.primaryColor(), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        if (downloadingAll) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${t.gallery_downloading_all} ${downloadAllProgress.first}/${downloadAllProgress.second}",
                    fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = theme.mutedColor()
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        when {
            loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = theme.primaryColor(), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse),
                        label = "alpha"
                    )
                    Text(t.gallery_loading, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                        color = theme.primaryColor().copy(alpha = alpha))
                }
            }
            error -> Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(shape = CircleShape, color = Color(0xFFEF4444).copy(alpha = 0.1f)) {
                        Icon(Icons.Default.WifiOff, null, tint = Color(0xFFEF4444),
                            modifier = Modifier.size(40.dp).padding(8.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(t.error_signal, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))) {
                        Text(t.retry, fontWeight = FontWeight.Bold)
                    }
                }
            }
            images.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PhotoCamera, null, tint = theme.mutedColor().copy(alpha = 0.2f),
                        modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(t.gallery_empty, color = theme.mutedColor(), fontSize = 13.sp)
                }
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (fromCache) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                        OfflineBanner(text = t.offline_cached_banner, primaryColor = theme.primaryColor())
                    }
                }
                items(images, key = { it.sha }) { img ->
                    val index = images.indexOf(img)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                            .clickable { selectedIndex = index }
                    ) {
                        AsyncImage(
                            model = img.download_url,
                            contentDescription = img.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            alpha = 0.85f
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(0.65f))
                                    )
                                )
                                .padding(8.dp)
                        ) {
                            Text(text = img.name, fontSize = 9.sp, fontFamily = FontFamily.Monospace,
                                color = Color.White, maxLines = 1)
                        }
                    }
                }
            }
        }
    }

    // Fullscreen pager: swipe between images, pinch-to-zoom each one.
    selectedIndex?.let { initialIndex ->
        GalleryPagerDialog(
            images = images,
            initialIndex = initialIndex,
            translations = t,
            onDismiss = { selectedIndex = null }
        )
    }
}

@Composable
private fun GalleryPagerDialog(
    images: List<GalleryImage>,
    initialIndex: Int,
    translations: Translations,
    onDismiss: () -> Unit
) {
    val theme = LocalAppTheme.current
    val t = translations
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloading by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(initialPage = initialIndex) { images.size }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.97f)),
            contentAlignment = Alignment.Center
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                ZoomableImage(url = images[page].download_url, contentDescription = images[page].name)
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.8f))
            }

            // Page indicator
            Text(
                text = "${pagerState.currentPage + 1} / ${images.size}",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp)
            )

            // Save + share actions
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.85f)
                    .padding(bottom = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            downloading = true
                            downloadImageToGallery(context, images[pagerState.currentPage])
                            downloading = false
                            Toast.makeText(context, t.toast_saved, Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !downloading,
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor()),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    if (downloading) {
                        CircularProgressIndicator(color = theme.backgroundColor(), modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Download, null, tint = theme.backgroundColor(), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(t.download_save, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = theme.backgroundColor())
                    }
                }
                OutlinedButton(
                    onClick = {
                        scope.launch { shareImage(context, images[pagerState.currentPage]) }
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(t.gallery_share, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

/** A pannable/pinch-zoomable image, resetting its transform whenever [url] changes (new page). */
@Composable
private fun ZoomableImage(url: String, contentDescription: String?) {
    var scale by remember(url) { mutableStateOf(1f) }
    var offset by remember(url) { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(url) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    offset = if (scale <= 1f) Offset.Zero else offset + pan
                }
            }
    ) {
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale, scaleY = scale,
                    translationX = offset.x, translationY = offset.y
                )
        )
    }
}

private suspend fun downloadImageToGallery(context: Context, img: GalleryImage) {
    withContext(Dispatchers.IO) {
        try {
            val stream: InputStream = URL(img.download_url).openStream()
            val bytes = stream.readBytes()
            stream.close()
            val fname = "HackerOS_Gallery_${img.name}"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fname)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                uri?.let { context.contentResolver.openOutputStream(it)?.use { os -> os.write(bytes) } }
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                dir.mkdirs()
                File(dir, fname).writeBytes(bytes)
            }
        } catch (_: Exception) {
        }
    }
}

private suspend fun shareImage(context: Context, img: GalleryImage) {
    withContext(Dispatchers.IO) {
        try {
            val bytes = URL(img.download_url).openStream().use { it.readBytes() }
            val dir = File(context.cacheDir, "shared").apply { mkdirs() }
            val file = File(dir, img.name)
            file.writeBytes(bytes)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            withContext(Dispatchers.Main) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, img.name))
            }
        } catch (_: Exception) {
        }
    }
}
