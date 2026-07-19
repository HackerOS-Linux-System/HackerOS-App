package com.hackeros.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.hackeros.app.data.model.AppScreen
import com.hackeros.app.ui.components.HackerOSNavBar
import com.hackeros.app.ui.screens.*
import com.hackeros.app.ui.theme.*
import com.hackeros.app.utils.NotificationHelper
import com.hackeros.app.utils.getTranslations

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

        override fun onCreate(savedInstanceState: Bundle?) {
            installSplashScreen()
            super.onCreate(savedInstanceState)
            enableEdgeToEdge()

            setContent {
                val context = LocalContext.current
                val currentThemeId by viewModel.currentTheme.collectAsState()
                val currentLanguage by viewModel.currentLanguage.collectAsState()
                val currentScreen by viewModel.currentScreen.collectAsState()
                val releases by viewModel.releases.collectAsState()
                val releasesLoading by viewModel.releasesLoading.collectAsState()
                val releasesError by viewModel.releasesError.collectAsState()
                val gallery by viewModel.gallery.collectAsState()
                val galleryLoading by viewModel.galleryLoading.collectAsState()
                val galleryError by viewModel.galleryError.collectAsState()
                val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
                val updateStatus by viewModel.updateStatus.collectAsState()
                val remoteVersion by viewModel.remoteVersion.collectAsState()

                val appTheme = THEMES[currentThemeId] ?: THEMES[com.hackeros.app.data.model.ThemeId.HACKER]!!
                val translations = getTranslations(currentLanguage)

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    // Whatever the system dialog result is, reflect it exactly: if denied,
                    // notifications stay off rather than silently doing nothing.
                    viewModel.setNotificationsEnabled(granted)
                    if (!granted) {
                        android.widget.Toast.makeText(
                            context, translations.notif_permission_denied, android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                HackerOSTheme(appTheme = appTheme) {
                    Box(
                        modifier = Modifier
                        .fillMaxSize()
                        .background(Color(appTheme.background))
                        .windowInsetsPadding(WindowInsets.statusBars)
                    ) {
                        Box(
                            modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(appTheme.primary).copy(alpha = 0.04f),
                                                    Color.Transparent
                                    )
                                )
                            )
                        )

                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 12 })
                                .togetherWith(fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { -it / 12 })
                            },
                            modifier = Modifier.fillMaxSize(),
                                        label = "screenTransition"
                        ) { screen ->
                            Box(modifier = Modifier.fillMaxSize()) {
                                when (screen) {
                                    AppScreen.RELEASES -> ReleasesScreen(
                                        releases = releases,
                                        loading = releasesLoading,
                                        error = releasesError,
                                        translations = translations,
                                        onRetry = { viewModel.fetchReleases() }
                                    )
                                    AppScreen.WALLPAPERS -> WallpapersScreen(
                                        wallpapers = Constants.WALLPAPERS,
                                        translations = translations
                                    )
                                    AppScreen.GALLERY -> GalleryScreen(
                                        images = gallery,
                                        loading = galleryLoading,
                                        error = galleryError,
                                        translations = translations,
                                        onRetry = { viewModel.fetchGallery() }
                                    )
                                    AppScreen.TEAM -> TeamScreen(translations = translations)
                                    AppScreen.SETTINGS -> SettingsScreen(
                                        currentTheme = currentThemeId,
                                        onThemeChange = { viewModel.setTheme(it) },
                                                                         currentLanguage = currentLanguage,
                                                                         onLanguageChange = { viewModel.setLanguage(it) },
                                                                         notificationsEnabled = notificationsEnabled,
                                                                         onToggleNotifications = {
                                                                             val turningOn = !notificationsEnabled
                                                                             when {
                                                                                 !turningOn -> viewModel.setNotificationsEnabled(false)
                                                                                 Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                                                     !NotificationHelper.hasPermission(context) ->
                                                                                     notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                                                 else -> viewModel.setNotificationsEnabled(true)
                                                                             }
                                                                         },
                                                                         updateStatus = updateStatus,
                                                                         remoteVersion = remoteVersion,
                                                                         onCheckUpdate = { viewModel.checkForUpdates() },
                                                                         translations = translations
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                            HackerOSNavBar(
                                currentScreen = currentScreen,
                                onScreenChange = { viewModel.setScreen(it) },
                                           translations = translations
                            )
                        }
                    }
                }
            }
        }
}
