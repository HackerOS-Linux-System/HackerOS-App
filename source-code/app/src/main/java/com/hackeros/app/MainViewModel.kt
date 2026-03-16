package com.hackeros.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hackeros.app.data.model.AppScreen
import com.hackeros.app.data.model.GalleryImage
import com.hackeros.app.data.model.Language
import com.hackeros.app.data.model.ReleaseInfo
import com.hackeros.app.data.model.ThemeId
import com.hackeros.app.data.parser.ReleaseParser
import com.hackeros.app.data.repository.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesRepository(application)

    // --- Navigation ---
    private val _currentScreen = MutableStateFlow(AppScreen.RELEASES)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // --- Theme & Language ---
    private val _currentTheme = MutableStateFlow(ThemeId.HACKER)
    val currentTheme: StateFlow<ThemeId> = _currentTheme.asStateFlow()

    private val _currentLanguage = MutableStateFlow(Language.PL)
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()

    // --- Releases ---
    private val _releases = MutableStateFlow<List<ReleaseInfo>>(emptyList())
    val releases: StateFlow<List<ReleaseInfo>> = _releases.asStateFlow()

    private val _releasesLoading = MutableStateFlow(true)
    val releasesLoading: StateFlow<Boolean> = _releasesLoading.asStateFlow()

    private val _releasesError = MutableStateFlow<String?>(null)
    val releasesError: StateFlow<String?> = _releasesError.asStateFlow()

    // --- Gallery ---
    private val _gallery = MutableStateFlow<List<GalleryImage>>(emptyList())
    val gallery: StateFlow<List<GalleryImage>> = _gallery.asStateFlow()

    private val _galleryLoading = MutableStateFlow(true)
    val galleryLoading: StateFlow<Boolean> = _galleryLoading.asStateFlow()

    private val _galleryError = MutableStateFlow(false)
    val galleryError: StateFlow<Boolean> = _galleryError.asStateFlow()

    // --- Notifications ---
    private val _notificationsEnabled = MutableStateFlow(false)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    // --- Update check ---
    enum class UpdateStatus { IDLE, CHECKING, UP_TO_DATE, UPDATE_AVAILABLE, ERROR }
    private val _updateStatus = MutableStateFlow(UpdateStatus.IDLE)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    private val _remoteVersion = MutableStateFlow(Constants.APP_VERSION)
    val remoteVersion: StateFlow<String> = _remoteVersion.asStateFlow()

    init {
        loadPreferences()
        fetchReleases()
        fetchGallery()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            _currentTheme.value = prefs.themeFlow.first()
            _currentLanguage.value = prefs.languageFlow.first()
            _notificationsEnabled.value = prefs.notificationsFlow.first()
        }
    }

    fun setScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setTheme(themeId: ThemeId) {
        _currentTheme.value = themeId
        viewModelScope.launch { prefs.saveTheme(themeId) }
    }

    fun setLanguage(lang: Language) {
        _currentLanguage.value = lang
        viewModelScope.launch { prefs.saveLanguage(lang) }
    }

    fun toggleNotifications() {
        val new = !_notificationsEnabled.value
        _notificationsEnabled.value = new
        viewModelScope.launch { prefs.saveNotifications(new) }
    }

    fun fetchReleases() {
        viewModelScope.launch {
            _releasesLoading.value = true
            _releasesError.value = null
            try {
                val text = withContext(Dispatchers.IO) {
                    URL("${Constants.RELEASE_INFO_URL}?t=${System.currentTimeMillis()}")
                        .readText()
                }
                _releases.value = ReleaseParser.parse(text)
            } catch (e: Exception) {
                _releasesError.value = "Connection failed. Check your uplink."
            } finally {
                _releasesLoading.value = false
            }
        }
    }

    fun fetchGallery() {
        viewModelScope.launch {
            _galleryLoading.value = true
            _galleryError.value = false
            try {
                val json = withContext(Dispatchers.IO) {
                    URL(Constants.GALLERY_API_URL).readText()
                }
                val arr = JSONArray(json)
                val images = mutableListOf<GalleryImage>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val name = obj.getString("name")
                    if (obj.getString("type") == "file" &&
                        name.matches(Regex(".*\\.(jpg|jpeg|png|gif|webp)", RegexOption.IGNORE_CASE))
                    ) {
                        images.add(
                            GalleryImage(
                                name = name,
                                sha = obj.getString("sha"),
                                size = obj.getLong("size"),
                                url = obj.getString("url"),
                                html_url = obj.getString("html_url"),
                                git_url = obj.getString("git_url"),
                                download_url = obj.getString("download_url"),
                                type = obj.getString("type")
                            )
                        )
                    }
                }
                _gallery.value = images
            } catch (e: Exception) {
                _galleryError.value = true
            } finally {
                _galleryLoading.value = false
            }
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _updateStatus.value = UpdateStatus.CHECKING
            try {
                val text = withContext(Dispatchers.IO) {
                    URL("${Constants.VERSION_CHECK_URL}?t=${System.currentTimeMillis()}").readText()
                }
                val match = Regex("""(?:v|ver|version|\[)?\s*([\d.]+)\s*(?:\])?""", RegexOption.IGNORE_CASE)
                    .find(text)
                val remoteVer = match?.groupValues?.get(1)
                if (remoteVer != null) {
                    _remoteVersion.value = remoteVer
                    _updateStatus.value = if (
                        remoteVer != Constants.APP_VERSION &&
                        remoteVer.toDoubleOrNull() ?: 0.0 > Constants.APP_VERSION.toDoubleOrNull() ?: 0.0
                    ) UpdateStatus.UPDATE_AVAILABLE else UpdateStatus.UP_TO_DATE
                } else {
                    _updateStatus.value = UpdateStatus.ERROR
                }
            } catch (e: Exception) {
                _updateStatus.value = UpdateStatus.ERROR
            }
        }
    }
}
