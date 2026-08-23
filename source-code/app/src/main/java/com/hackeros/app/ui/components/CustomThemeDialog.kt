package com.hackeros.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hackeros.app.data.model.AppTheme
import com.hackeros.app.utils.Translations

private enum class ColorRole { PRIMARY, BACKGROUND, CARD }

// A curated swatch grid spanning the hue spectrum plus a grayscale row, so picking a starting
// point is one tap; the hex field next to it covers exact/precise colors.
private val SWATCHES = listOf(
    0xFFEF4444, 0xFFF97316, 0xFFF59E0B, 0xFFEAB308, 0xFFA3E635, 0xFF22C55E,
    0xFF10B981, 0xFF14B8A6, 0xFF06B6D4, 0xFF0EA5E9, 0xFF3B82F6, 0xFF6366F1,
    0xFF8B5CF6, 0xFFA855F7, 0xFFD946EF, 0xFFEC4899, 0xFFF43F5E, 0xFFDC2626,
    0xFFFFFFFF, 0xFFE4E4E7, 0xFF9CA3AF, 0xFF4B5563, 0xFF1F2937, 0xFF000000
)

@Composable
fun CustomThemeDialog(
    initial: AppTheme?,
    translations: Translations,
    onDismiss: () -> Unit,
    onSave: (primary: Long, background: Long, card: Long) -> Unit
) {
    val t = translations
    var primary by remember { mutableStateOf(initial?.primary ?: 0xFF10B981) }
    var background by remember { mutableStateOf(initial?.background ?: 0xFF0A0A0C) }
    var card by remember { mutableStateOf(initial?.card ?: 0xFF17171C) }
    var role by remember { mutableStateOf(ColorRole.PRIMARY) }

    val currentValue = when (role) { ColorRole.PRIMARY -> primary; ColorRole.BACKGROUND -> background; ColorRole.CARD -> card }
    var hexText by remember(role, primary, background, card) { mutableStateOf(longToHex(currentValue)) }
    var hexError by remember(role) { mutableStateOf(false) }

    fun setCurrent(value: Long) {
        when (role) {
            ColorRole.PRIMARY -> primary = value
            ColorRole.BACKGROUND -> background = value
            ColorRole.CARD -> card = value
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(background))
                .border(1.dp, Color(primary).copy(alpha = 0.25f), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp).heightIn(max = 620.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, null, tint = Color(primary), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(t.custom_theme_title, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace, color = Color.White, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.6f)) }
                }

                Spacer(Modifier.height(14.dp))

                // Live preview: a mini mock-up of the app card using the three chosen colors.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(card))
                        .border(1.dp, Color(primary).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Text(t.custom_theme_preview, fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(primary)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("HackerOS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(background))
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Role selector
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    RoleChip(t.custom_theme_primary_label, role == ColorRole.PRIMARY, Color(primary)) { role = ColorRole.PRIMARY }
                    RoleChip(t.custom_theme_background_label, role == ColorRole.BACKGROUND, Color(background)) { role = ColorRole.BACKGROUND }
                    RoleChip(t.custom_theme_card_label, role == ColorRole.CARD, Color(card)) { role = ColorRole.CARD }
                }

                Spacer(Modifier.height(14.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier.heightIn(max = 160.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SWATCHES) { swatch ->
                        val selected = swatch == currentValue
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(Color(swatch))
                                .border(
                                    if (selected) 2.dp else 1.dp,
                                    if (selected) Color.White else Color.White.copy(alpha = 0.15f),
                                    CircleShape
                                )
                                .clickable { setCurrent(swatch) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    Icons.Default.Check, null,
                                    tint = if (isLightColor(swatch)) Color.Black else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = hexText,
                    onValueChange = { input ->
                        hexText = input
                        val parsed = hexToLong(input)
                        if (parsed != null) {
                            hexError = false
                            setCurrent(parsed)
                        } else {
                            hexError = true
                        }
                    },
                    label = { Text(t.custom_theme_hex_hint, fontSize = 11.sp) },
                    leadingIcon = { Text("#", color = Color.White.copy(alpha = 0.6f)) },
                    isError = hexError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(primary),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(Modifier.height(18.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) { Text(t.custom_theme_cancel, fontWeight = FontWeight.Bold, fontSize = 12.sp) }

                    Button(
                        onClick = { onSave(primary, background, card) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(primary)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(
                            t.custom_theme_save, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            color = if (isLightColor(primary)) Color.Black else Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.RoleChip(label: String, active: Boolean, swatch: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) Color.White.copy(alpha = 0.1f) else Color.Transparent)
            .border(1.dp, if (active) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(swatch))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = if (active) 1f else 0.6f))
    }
}

private fun longToHex(value: Long): String {
    val argb = value and 0xFFFFFFFFL
    return "%06X".format(argb and 0xFFFFFF)
}

/** Parses a user-typed hex string (with or without leading #, 3 or 6 digits) into an ARGB Long. */
private fun hexToLong(input: String): Long? {
    val clean = input.removePrefix("#").trim()
    val expanded = when (clean.length) {
        6 -> clean
        3 -> clean.map { "$it$it" }.joinToString("")
        else -> return null
    }
    return try {
        val rgb = expanded.toLong(16)
        0xFF000000L or rgb
    } catch (_: Exception) {
        null
    }
}

private fun isLightColor(value: Long): Boolean {
    val r = (value shr 16) and 0xFF
    val g = (value shr 8) and 0xFF
    val b = value and 0xFF
    val luminance = (0.299 * r + 0.587 * g + 0.114 * b)
    return luminance > 150
}
