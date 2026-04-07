package com.hackeros.app.ui.screens

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.WifiOff
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
import com.hackeros.app.data.model.GalleryImage
import com.hackeros.app.ui.theme.LocalAppTheme
import com.hackeros.app.ui.theme.backgroundColor
import com.hackeros.app.ui.theme.mutedColor
import com.hackeros.app.ui.theme.primaryColor
import com.hackeros.app.utils.Translations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.URL

@Composable
fun GalleryScreen(
    images: List<GalleryImage>,
    loading: Boolean,
    error: Boolean,
    translations: Translations,
    onRetry: () -> Unit
) {
    val theme = LocalAppTheme.current
    val t = translations
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedImage by remember { mutableStateOf<GalleryImage?>(null) }
    var downloading by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 20.dp)) {
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

        when {
            loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = theme.primaryColor(),
                                              modifier = Modifier.size(48.dp)
                    )
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
                        text = t.gallery_loading,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = theme.primaryColor().copy(alpha = alpha)
                    )
                }
            }
            error -> Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                         contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(shape = CircleShape, color = Color(0xFFEF4444).copy(alpha = 0.1f)) {
                        Icon(Icons.Default.WifiOff, null,
                             tint = Color(0xFFEF4444),
                             modifier = Modifier.size(40.dp).padding(8.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(t.error_signal, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRetry,
                           colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))) {
                        Text(t.retry, fontWeight = FontWeight.Bold)
                           }
                }
            }
            images.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PhotoCamera, null,
                         tint = theme.mutedColor().copy(alpha = 0.2f),
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
                items(images, key = { it.sha }) { img ->
                    Box(
                        modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                        .clickable { selectedImage = img }
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
                            Text(
                                text = img.name,
                                 fontSize = 9.sp,
                                 fontFamily = FontFamily.Monospace,
                                 color = Color.White,
                                 maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    // Fullscreen modal
    selectedImage?.let { img ->
        Dialog(
            onDismissRequest = { selectedImage = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.97f)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { selectedImage = null },
                    modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.8f))
                }

                Box(
                    modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.72f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                ) {
                    AsyncImage(
                        model = img.download_url,
                        contentDescription = img.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Button(
                    onClick = {
                        scope.launch {
                            downloading = true
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
                                            put(MediaStore.Images.Media.RELATIVE_PATH,
                                                Environment.DIRECTORY_DOWNLOADS)
                                        }
                                        val uri = context.contentResolver.insert(
                                            MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                                        uri?.let {
                                            context.contentResolver.openOutputStream(it)
                                            ?.use { os -> os.write(bytes) }
                                        }
                                    } else {
                                        val dir = Environment.getExternalStoragePublicDirectory(
                                            Environment.DIRECTORY_DOWNLOADS)
                                        dir.mkdirs()
                                        java.io.File(dir, fname).writeBytes(bytes)
                                    }
                                } catch (_: Exception) {}
                            }
                            downloading = false
                            Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
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
                        CircularProgressIndicator(color = theme.backgroundColor(),
                                                  modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
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
