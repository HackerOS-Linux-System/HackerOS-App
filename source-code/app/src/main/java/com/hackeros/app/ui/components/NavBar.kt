package com.hackeros.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hackeros.app.data.model.AppScreen
import com.hackeros.app.ui.theme.LocalAppTheme
import com.hackeros.app.ui.theme.mutedColor
import com.hackeros.app.ui.theme.primaryColor
import com.hackeros.app.ui.theme.textColor
import com.hackeros.app.utils.Translations

data class NavItem(
    val screen: AppScreen,
    val icon: ImageVector,
    val label: String
)

@Composable
fun HackerOSNavBar(
    currentScreen: AppScreen,
    onScreenChange: (AppScreen) -> Unit,
    translations: Translations
) {
    val theme = LocalAppTheme.current

    val navItems = listOf(
        NavItem(AppScreen.RELEASES, Icons.Default.List, translations.nav_releases),
        NavItem(AppScreen.WALLPAPERS, Icons.Default.Image, translations.nav_wallpapers),
        NavItem(AppScreen.GALLERY, Icons.Default.CameraAlt, translations.nav_gallery),
        NavItem(AppScreen.TEAM, Icons.Default.Group, translations.nav_team),
        NavItem(AppScreen.SETTINGS, Icons.Default.Settings, translations.nav_config),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.backgroundColor().copy(alpha = 0.92f))
    ) {
        // Top border line
        Divider(
            color = Color.White.copy(alpha = 0.05f),
            thickness = 1.dp,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            navItems.forEach { item ->
                val isActive = currentScreen == item.screen
                val iconColor by animateColorAsState(
                    targetValue = if (isActive) theme.primaryColor() else theme.mutedColor(),
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "iconColor"
                )

                NavigationBarItem(
                    selected = isActive,
                    onClick = { onScreenChange(item.screen) },
                    icon = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                    .background(
                                        if (isActive) theme.primaryColor().copy(alpha = 0.15f)
                                        else Color.Transparent
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = iconColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            if (isActive) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(theme.primaryColor())
                                )
                            }
                        }
                    },
                    label = {
                        Text(
                            text = item.label.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) theme.primaryColor() else theme.mutedColor().copy(alpha = 0.6f),
                            letterSpacing = 0.5.sp,
                            maxLines = 1
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent
                    ),
                    interactionSource = remember { MutableInteractionSource() }
                )
            }
        }
    }
}
