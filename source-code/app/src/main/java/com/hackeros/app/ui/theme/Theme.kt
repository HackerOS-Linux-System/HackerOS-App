package com.hackeros.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.hackeros.app.data.model.AppTheme
import com.hackeros.app.data.model.ThemeId

val LocalAppTheme = compositionLocalOf<AppTheme> { THEMES[ThemeId.HACKER]!! }

@Composable
fun HackerOSTheme(
    appTheme: AppTheme,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = appTheme.primaryColor(),
        background = appTheme.backgroundColor(),
        surface = appTheme.cardColor(),
        onPrimary = appTheme.backgroundColor(),
        onBackground = appTheme.textColor(),
        onSurface = appTheme.textColor(),
        secondary = appTheme.primaryColor(),
        onSecondary = appTheme.backgroundColor(),
    )

    CompositionLocalProvider(LocalAppTheme provides appTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
