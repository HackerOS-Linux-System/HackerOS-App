package com.hackeros.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hackeros.app.ui.theme.LocalAppTheme
import com.hackeros.app.ui.theme.mutedColor
import com.hackeros.app.ui.theme.primaryColor
import com.hackeros.app.utils.Translations

@Composable
fun TeamScreen(translations: Translations) {
    val theme = LocalAppTheme.current
    val t = translations
    val redColor = Color(0xFFEF4444)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp)
    ) {
        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp)) {
            Text(
                text = t.header_team,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = Color.White
            )
            Text(
                text = t.sub_team,
                fontSize = 13.sp,
                color = theme.mutedColor(),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Team member card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
        ) {
            // Red glow top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(redColor.copy(alpha = 0.15f), Color.Transparent)
                        )
                    )
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp)
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .border(2.dp, redColor.copy(alpha = 0.5f), CircleShape)
                ) {
                    AsyncImage(
                        model = "https://raw.githubusercontent.com/HackerOS-Linux-System/HackerOS-Website/main/michal92299.png",
                        contentDescription = "Michal92299",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Icon badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Color(0xFF17171C))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                            .padding(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = redColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Michal92299",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = 0.5.sp,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Michał Kaczmarzyk",
                    fontFamily = FontFamily.Monospace,
                    color = theme.mutedColor(),
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = t.role_founder.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = redColor,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Contact card (v0.7) - replaces the old generic "HackerOS is a project..." blurb with
        // an actual way to reach the team. Sending a message straight from this screen is a
        // planned future addition; for now this opens the device's email app pre-addressed.
        val context = androidx.compose.ui.platform.LocalContext.current
        val contactEmail = "hackeros068@gmail.com"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        tint = theme.primaryColor(),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = t.team_contact_title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = t.team_contact_desc,
                    fontSize = 12.sp,
                    color = theme.mutedColor(),
                    lineHeight = 17.sp
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(theme.primaryColor().copy(alpha = 0.08f))
                        .border(1.dp, theme.primaryColor().copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .clickable {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                    data = android.net.Uri.parse("mailto:$contactEmail")
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                cm.setPrimaryClip(android.content.ClipData.newPlainText("email", contactEmail))
                                android.widget.Toast.makeText(context, t.toast_copied, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = contactEmail,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.primaryColor(),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = theme.primaryColor(),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = t.team_contact_future_note,
                    fontSize = 10.sp,
                    color = theme.mutedColor().copy(alpha = 0.7f)
                )
            }
        }
    }
}
