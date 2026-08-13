package com.hackeros.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hackeros.app.Constants
import com.hackeros.app.data.model.ReleaseInfo
import com.hackeros.app.ui.theme.LocalAppTheme
import com.hackeros.app.ui.theme.backgroundColor
import com.hackeros.app.ui.theme.cardColor
import com.hackeros.app.ui.theme.mutedColor
import com.hackeros.app.ui.theme.primaryColor
import com.hackeros.app.ui.theme.textColor
import com.hackeros.app.utils.Translations

@Composable
fun WhatsNewDialog(
    release: ReleaseInfo?,
    translations: Translations,
    onDismiss: () -> Unit
) {
    val theme = LocalAppTheme.current
    val t = translations

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(theme.cardColor())
                .border(1.dp, theme.primaryColor().copy(alpha = 0.15f), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.AutoAwesome, null, tint = theme.primaryColor(), modifier = Modifier.size(22.dp))
                    Column {
                        Text(t.whats_new_title, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                            fontSize = 18.sp, color = theme.textColor())
                        Text(
                            "${t.whats_new_subtitle} v${Constants.APP_VERSION}",
                            fontSize = 11.sp, color = theme.mutedColor()
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                if (release != null) {
                    Text(release.version, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = theme.primaryColor())
                    if (release.description.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(release.description, fontSize = 12.sp, color = theme.mutedColor())
                    }
                    val newsItems = release.news.split("\n").filter { it.isNotBlank() }
                    if (newsItems.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 260.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(newsItems) { line ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("•", color = theme.primaryColor(), fontWeight = FontWeight.Bold)
                                    Text(line, fontSize = 12.sp, color = theme.textColor(), lineHeight = 16.sp)
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        "v${Constants.APP_VERSION}",
                        fontSize = 12.sp, color = theme.mutedColor()
                    )
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primaryColor()),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(t.whats_new_dismiss, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = theme.backgroundColor())
                }
            }
        }
    }
}
