package com.hackeros.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hackeros.app.data.cache.OfflineCacheCodec
import com.hackeros.app.data.model.AppScreen
import com.hackeros.app.data.model.GalleryImage
import com.hackeros.app.data.model.Language
import com.hackeros.app.data.model.ReleaseInfo
import com.hackeros.app.data.model.ThemeId
import com.hackeros.app.data.model.editionNames
import com.hackeros.app.data.parser.ReleaseParser
import com.hackeros.app.data.parser.WebsiteReleaseParser
import com.hackeros.app.data.repository.PreferencesRepository
import com.hackeros.app.utils.ApkUpdater
import com.hackeros.app.worker.ReleaseNotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.URL

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesRepository(application)

    // --- Navigation ---
    private val _currentScreen = MutableStateFlow(AppScreen.RELEASES)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // --- Theme & Language ---
    private val _currentTheme = MutableStateFlow(ThemeId.MONOCHROME)
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

    // True when [releases] is being served from the local offline cache (i.e. the live fetch
    // failed but a previously cached copy exists), so the UI can show a small "offline" notice
    // instead of pretending this is fresh data.
    private val _releasesFromCache = MutableStateFlow(false)
    val releasesFromCache: StateFlow<Boolean> = _releasesFromCache.asStateFlow()

    // --- Gallery ---
    private val _gallery = MutableStateFlow<List<GalleryImage>>(emptyList())
    val gallery: StateFlow<List<GalleryImage>> = _gallery.asStateFlow()

    private val _galleryLoading = MutableStateFlow(true)
    val galleryLoading: StateFlow<Boolean> = _galleryLoading.asStateFlow()

    private val _galleryError = MutableStateFlow(false)
    val galleryError: StateFlow<Boolean> = _galleryError.asStateFlow()

    private val _galleryFromCache = MutableStateFlow(false)
    val galleryFromCache: StateFlow<Boolean> = _galleryFromCache.asStateFlow()

    // --- Notifications ---
    private val _notificationsEnabled = MutableStateFlow(false)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    // Which release editions (e.g. "HackerOS Official", "HackerOS Cybersecurity") the user
    // wants to be notified about. Null means "all editions" (the default).
    private val _watchedEditions = MutableStateFlow<Set<String>?>(null)
    val watchedEditions: StateFlow<Set<String>?> = _watchedEditions.asStateFlow()

    // --- "What's new" dialog ---
    // True exactly once per app update (until dismissed): the previously-recorded app version
    // differs from the one currently running. The dialog reads its content straight from the
    // already-fetched [releases] list - if that list is empty (e.g. fetch failed), the dialog
    // still shows with just the bare version number rather than blocking on network.
    private val _showWhatsNew = MutableStateFlow(false)
    val showWhatsNew: StateFlow<Boolean> = _showWhatsNew.asStateFlow()

    // --- Update check ---
    enum class UpdateStatus { IDLE, CHECKING, UP_TO_DATE, UPDATE_AVAILABLE, ERROR }
    private val _updateStatus = MutableStateFlow(UpdateStatus.IDLE)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    private val _remoteVersion = MutableStateFlow(Constants.APP_VERSION)
    val remoteVersion: StateFlow<String> = _remoteVersion.asStateFlow()

    // --- In-app APK update download/verify/install ---
    sealed class ApkUpdateState {
        object Idle : ApkUpdateState()
        data class Downloading(val progress: Int) : ApkUpdateState()
        object Verifying : ApkUpdateState()
        data class ReadyToInstall(val file: File, val verified: Boolean, val checksumAvailable: Boolean) : ApkUpdateState()
        object Error : ApkUpdateState()
    }

    private val _apkUpdateState = MutableStateFlow<ApkUpdateState>(ApkUpdateState.Idle)
    val apkUpdateState: StateFlow<ApkUpdateState> = _apkUpdateState.asStateFlow()

    init {
        loadPreferencesThenFetch()
        fetchGallery()
    }

    private fun loadPreferencesThenFetch() {
        viewModelScope.launch {
            _currentTheme.value = prefs.themeFlow.first()
            _currentLanguage.value = prefs.languageFlow.first()
            _notificationsEnabled.value = prefs.notificationsFlow.first()
            _watchedEditions.value = prefs.watchedEditionsFlow.first()
            // Keep the background worker in sync with the saved preference - important after
            // an app reinstall/update, where WorkManager's own schedule may have been reset.
            if (_notificationsEnabled.value) {
                ReleaseNotificationScheduler.enable(getApplication())
            } else {
                ReleaseNotificationScheduler.disable(getApplication())
            }
            // Fetch releases only after the saved language preference is known, so the very
            // first load already shows releases in the user's preferred language.
            fetchReleases()
            maybeShowWhatsNew()
        }
    }

    /**
     * Shows the "What's new" dialog exactly once per app update: if the app version we
     * previously recorded differs from the version currently running, we show the newest
     * already-fetched release as a summary, then remember this version so it never reappears.
     */
    private suspend fun maybeShowWhatsNew() {
        val lastSeenAppVersion = prefs.lastSeenAppVersionFlow.first()
        if (lastSeenAppVersion != null && lastSeenAppVersion != Constants.APP_VERSION) {
            _showWhatsNew.value = true
        }
        prefs.saveLastSeenAppVersion(Constants.APP_VERSION)
    }

    fun dismissWhatsNew() {
        _showWhatsNew.value = false
    }

    fun setScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setTheme(themeId: ThemeId) {
        _currentTheme.value = themeId
        viewModelScope.launch { prefs.saveTheme(themeId) }
    }

    fun setLanguage(lang: Language) {
        val changed = _currentLanguage.value != lang
        _currentLanguage.value = lang
        viewModelScope.launch { prefs.saveLanguage(lang) }
        // Releases are localized (e.g. Polish changelog text vs. English), so re-fetch/re-parse
        // them whenever the language changes so the releases screen updates immediately.
        if (changed) fetchReleases()
    }

    /**
     * Sets the notifications preference explicitly (rather than a blind toggle) so the caller
     * -MainActivity- can first resolve the Android 13+ POST_NOTIFICATIONS runtime permission
     * and only turn things on if it was actually granted.
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        viewModelScope.launch { prefs.saveNotifications(enabled) }
        if (enabled) {
            ReleaseNotificationScheduler.enable(getApplication())
        } else {
            ReleaseNotificationScheduler.disable(getApplication())
        }
    }

    /** All edition names known from the currently loaded releases (used to build the filter UI). */
    fun knownEditionNames(): List<String> =
        _releases.value.flatMap { it.editionNames() }.distinct()

    fun toggleEditionWatch(edition: String) {
        val current = _watchedEditions.value ?: knownEditionNames().toSet()
        val updated = if (edition in current) current - edition else current + edition
        _watchedEditions.value = updated
        viewModelScope.launch { prefs.saveWatchedEditions(updated) }
    }

    fun resetEditionFilterToAll() {
        _watchedEditions.value = null
        viewModelScope.launch { prefs.saveWatchedEditions(null) }
    }

    fun fetchReleases() {
        viewModelScope.launch {
            _releasesLoading.value = true
            _releasesError.value = null
            try {
                // Primary source: read releases directly from the official HackerOS website's
                // real per-language release data file - the same data the "Releases" page on
                // https://hackeros-linux-system.github.io/HackerOS-Website/releases.html merges
                // in at runtime via JS. See Constants.releasesUrlFor()/WebsiteReleaseParser for why.
                val langCode = _currentLanguage.value.code
                val text = withContext(Dispatchers.IO) {
                    URL("${Constants.releasesUrlFor(langCode)}?t=${System.currentTimeMillis()}")
                        .readText()
                }
                val parsed = WebsiteReleaseParser.parse(text, _currentLanguage.value)
                if (parsed.isEmpty()) throw IllegalStateException("No release data found")
                _releases.value = parsed
                _releasesFromCache.value = false
                rememberLastSeenVersion(parsed)
                prefs.saveCachedReleases(OfflineCacheCodec.releasesToJson(parsed), _currentLanguage.value)
            } catch (e: Exception) {
                // Fallback #1: legacy .hacker release file, kept only for offline/outage resilience.
                try {
                    val legacyText = withContext(Dispatchers.IO) {
                        URL("${Constants.LEGACY_RELEASE_INFO_URL}?t=${System.currentTimeMillis()}")
                            .readText()
                    }
                    val legacyParsed = ReleaseParser.parse(legacyText)
                    if (legacyParsed.isEmpty()) throw IllegalStateException("No legacy release data found")
                    _releases.value = legacyParsed
                    _releasesFromCache.value = false
                    rememberLastSeenVersion(legacyParsed)
                } catch (e2: Exception) {
                    // Fallback #2: no connection at all reached either source - fall back to the
                    // last successfully fetched copy from local storage, if we have one for this
                    // language, so the user still sees their releases instead of a blank error.
                    val cached = prefs.cachedReleasesFlow.first()
                    val cachedParsed = OfflineCacheCodec.releasesFromJson(cached.json)
                    if (cachedParsed.isNotEmpty() && cached.language == _currentLanguage.value) {
                        _releases.value = cachedParsed
                        _releasesFromCache.value = true
                    } else {
                        _releasesError.value = "Connection failed. Check your uplink."
                        _releasesFromCache.value = false
                    }
                }
            } finally {
                _releasesLoading.value = false
            }
        }
    }

    /**
     * Whenever the user has actually seen the releases list in-app, remember the newest
     * version as "already known" so the background worker doesn't fire a redundant
     * notification for something the user just looked at.
     */
    private fun rememberLastSeenVersion(releases: List<ReleaseInfo>) {
        val latest = releases.firstOrNull() ?: return
        viewModelScope.launch { prefs.saveLastKnownVersion(latest.version) }
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
                _galleryFromCache.value = false
                prefs.saveCachedGallery(OfflineCacheCodec.galleryToJson(images))
            } catch (e: Exception) {
                // Fall back to the last successfully fetched gallery from local storage.
                val cachedJson = prefs.cachedGalleryJsonFlow.first()
                val cached = OfflineCacheCodec.galleryFromJson(cachedJson)
                if (cached.isNotEmpty()) {
                    _gallery.value = cached
                    _galleryFromCache.value = true
                    _galleryError.value = false
                } else {
                    _galleryError.value = true
                    _galleryFromCache.value = false
                }
            } finally {
                _galleryLoading.value = false
            }
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _updateStatus.value = UpdateStatus.CHECKING
            _apkUpdateState.value = ApkUpdateState.Idle
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

    /**
     * Downloads the update APK for [remoteVersion] with live progress, then attempts to verify
     * it against a published SHA-256 checksum before marking it ready to install. Verification
     * is best-effort: if no checksum was published for this release, the app still allows
     * installing but clearly marks the result as unverified rather than pretending it checked.
     */
    fun downloadUpdate() {
        val version = _remoteVersion.value
        viewModelScope.launch {
            _apkUpdateState.value = ApkUpdateState.Downloading(0)
            val file = ApkUpdater.downloadApk(
                context = getApplication(),
                apkUrl = Constants.apkUrlFor(version),
                version = version
            ) { percent -> _apkUpdateState.value = ApkUpdateState.Downloading(percent) }

            if (file == null) {
                _apkUpdateState.value = ApkUpdateState.Error
                return@launch
            }

            _apkUpdateState.value = ApkUpdateState.Verifying
            val result = withContext(Dispatchers.IO) {
                ApkUpdater.verifyChecksum(file, Constants.apkChecksumUrlFor(version))
            }
            when (result) {
                is ApkUpdater.VerifyResult.Verified -> _apkUpdateState.value =
                    ApkUpdateState.ReadyToInstall(file, verified = true, checksumAvailable = true)
                is ApkUpdater.VerifyResult.ChecksumUnavailable -> _apkUpdateState.value =
                    ApkUpdateState.ReadyToInstall(file, verified = false, checksumAvailable = false)
                is ApkUpdater.VerifyResult.Mismatch -> {
                    // Checksum mismatch is treated as a hard failure - never offer to install a
                    // file that doesn't match its published signature.
                    file.delete()
                    _apkUpdateState.value = ApkUpdateState.Error
                }
            }
        }
    }

    fun installDownloadedUpdate() {
        val state = _apkUpdateState.value
        if (state is ApkUpdateState.ReadyToInstall) {
            ApkUpdater.install(getApplication(), state.file)
        }
    }

    fun resetApkUpdateState() {
        _apkUpdateState.value = ApkUpdateState.Idle
    }
}
