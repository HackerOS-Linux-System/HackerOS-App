package com.hackeros.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hackeros.app.data.cache.OfflineCacheCodec
import com.hackeros.app.data.docs.DocContentParser
import com.hackeros.app.data.docs.DocPage
import com.hackeros.app.data.games.CommunityGame
import com.hackeros.app.data.games.GamesStoreParser
import com.hackeros.app.data.model.AppScreen
import com.hackeros.app.data.model.AppTheme
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

    private val _customThemeColors = MutableStateFlow<AppTheme?>(null)
    val customThemeColors: StateFlow<AppTheme?> = _customThemeColors.asStateFlow()

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

    // --- Section visibility (Settings toggles) ---
    private val _docsSectionEnabled = MutableStateFlow(true)
    val docsSectionEnabled: StateFlow<Boolean> = _docsSectionEnabled.asStateFlow()

    private val _gamesStoreSectionEnabled = MutableStateFlow(true)
    val gamesStoreSectionEnabled: StateFlow<Boolean> = _gamesStoreSectionEnabled.asStateFlow()

    // --- Documentation (native, parsed from the website's own data file - no WebView) ---
    private val _docPage = MutableStateFlow<DocPage?>(null)
    val docPage: StateFlow<DocPage?> = _docPage.asStateFlow()

    private val _docLoading = MutableStateFlow(true)
    val docLoading: StateFlow<Boolean> = _docLoading.asStateFlow()

    private val _docError = MutableStateFlow(false)
    val docError: StateFlow<Boolean> = _docError.asStateFlow()

    private val _docFromCache = MutableStateFlow(false)
    val docFromCache: StateFlow<Boolean> = _docFromCache.asStateFlow()

    // --- Games Store ---
    private val _gamesStore = MutableStateFlow<List<CommunityGame>>(emptyList())
    val gamesStore: StateFlow<List<CommunityGame>> = _gamesStore.asStateFlow()

    private val _gamesStoreLoading = MutableStateFlow(true)
    val gamesStoreLoading: StateFlow<Boolean> = _gamesStoreLoading.asStateFlow()

    private val _gamesStoreError = MutableStateFlow(false)
    val gamesStoreError: StateFlow<Boolean> = _gamesStoreError.asStateFlow()

    private val _gamesStoreFromCache = MutableStateFlow(false)
    val gamesStoreFromCache: StateFlow<Boolean> = _gamesStoreFromCache.asStateFlow()

    // Per-game install/download state, keyed by game id, mirroring ApkUpdateState.
    private val _gameInstallStates = MutableStateFlow<Map<String, ApkUpdateState>>(emptyMap())
    val gameInstallStates: StateFlow<Map<String, ApkUpdateState>> = _gameInstallStates.asStateFlow()

    init {
        loadPreferencesThenFetch()
        fetchGallery()
        fetchDocs()
        fetchGamesStore()
    }

    private fun loadPreferencesThenFetch() {
        viewModelScope.launch {
            _currentTheme.value = prefs.themeFlow.first()
            _customThemeColors.value = prefs.customThemeColorsFlow.first()?.let {
                AppTheme(id = ThemeId.CUSTOM, primary = it.primary, background = it.background, card = it.card)
            }
            _currentLanguage.value = prefs.languageFlow.first()
            _notificationsEnabled.value = prefs.notificationsFlow.first()
            _watchedEditions.value = prefs.watchedEditionsFlow.first()
            _docsSectionEnabled.value = prefs.docsSectionEnabledFlow.first()
            _gamesStoreSectionEnabled.value = prefs.gamesStoreSectionEnabledFlow.first()
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

    /** Saves the user's custom theme colors and immediately switches to it. */
    fun saveCustomTheme(primary: Long, background: Long, card: Long) {
        val theme = AppTheme(id = ThemeId.CUSTOM, primary = primary, background = background, card = card)
        _customThemeColors.value = theme
        _currentTheme.value = ThemeId.CUSTOM
        viewModelScope.launch {
            prefs.saveCustomThemeColors(primary, background, card)
            prefs.saveTheme(ThemeId.CUSTOM)
        }
    }

    fun setLanguage(lang: Language) {
        val changed = _currentLanguage.value != lang
        _currentLanguage.value = lang
        viewModelScope.launch { prefs.saveLanguage(lang) }
        // Releases are localized (e.g. Polish changelog text vs. English), so re-fetch/re-parse
        // them whenever the language changes so the releases screen updates immediately.
        if (changed) {
            fetchReleases()
            fetchDocs()
        }
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

    fun setDocsSectionEnabled(enabled: Boolean) {
        _docsSectionEnabled.value = enabled
        viewModelScope.launch { prefs.saveDocsSectionEnabled(enabled) }
        // If the user is currently looking at a section they just hid, bounce back to Releases.
        if (!enabled && _currentScreen.value == AppScreen.DOCS) _currentScreen.value = AppScreen.RELEASES
    }

    fun setGamesStoreSectionEnabled(enabled: Boolean) {
        _gamesStoreSectionEnabled.value = enabled
        viewModelScope.launch { prefs.saveGamesStoreSectionEnabled(enabled) }
        if (!enabled && _currentScreen.value == AppScreen.GAMES_STORE) _currentScreen.value = AppScreen.RELEASES
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

    // --- Documentation (native parsing, no WebView) -----------------------------------------

    fun fetchDocs() {
        viewModelScope.launch {
            _docLoading.value = true
            _docError.value = false
            try {
                val js = withContext(Dispatchers.IO) {
                    URL("${Constants.DOCUMENTATION_JS_URL}?t=${System.currentTimeMillis()}").readText()
                }
                val page = DocContentParser.buildPage(js, _currentLanguage.value.code)
                    ?: throw IllegalStateException("Could not parse documentation")
                _docPage.value = page
                _docFromCache.value = false
                prefs.saveCachedDocJs(js)
            } catch (e: Exception) {
                val cachedJs = prefs.cachedDocJsFlow.first()
                val cachedPage = cachedJs?.let { DocContentParser.buildPage(it, _currentLanguage.value.code) }
                if (cachedPage != null) {
                    _docPage.value = cachedPage
                    _docFromCache.value = true
                    _docError.value = false
                } else {
                    _docError.value = true
                    _docFromCache.value = false
                }
            } finally {
                _docLoading.value = false
            }
        }
    }

    // --- Games Store -------------------------------------------------------------------------

    fun fetchGamesStore() {
        viewModelScope.launch {
            _gamesStoreLoading.value = true
            _gamesStoreError.value = false
            try {
                val json = withContext(Dispatchers.IO) {
                    URL("${Constants.GAMES_STORE_JSON_URL}?t=${System.currentTimeMillis()}").readText()
                }
                val games = GamesStoreParser.parse(json)
                if (games.isEmpty() && json.isBlank()) throw IllegalStateException("Empty catalog")
                _gamesStore.value = games
                _gamesStoreFromCache.value = false
                prefs.saveCachedGamesStoreJson(json)
            } catch (e: Exception) {
                val cachedJson = prefs.cachedGamesStoreJsonFlow.first()
                val cachedGames = cachedJson?.let { GamesStoreParser.parse(it) } ?: emptyList()
                if (cachedGames.isNotEmpty()) {
                    _gamesStore.value = cachedGames
                    _gamesStoreFromCache.value = true
                    _gamesStoreError.value = false
                } else {
                    _gamesStoreError.value = true
                    _gamesStoreFromCache.value = false
                }
            } finally {
                _gamesStoreLoading.value = false
            }
        }
    }

    fun downloadGame(game: CommunityGame) {
        viewModelScope.launch {
            _gameInstallStates.value = _gameInstallStates.value + (game.id to ApkUpdateState.Downloading(0))
            val file = ApkUpdater.downloadToFile(
                context = getApplication(),
                url = game.downloadUrl,
                subDir = "games",
                fileName = "${game.id}-${game.version}.apk"
            ) { percent ->
                _gameInstallStates.value = _gameInstallStates.value + (game.id to ApkUpdateState.Downloading(percent))
            }

            if (file == null) {
                _gameInstallStates.value = _gameInstallStates.value + (game.id to ApkUpdateState.Error)
                return@launch
            }

            _gameInstallStates.value = _gameInstallStates.value + (game.id to ApkUpdateState.Verifying)
            val result = withContext(Dispatchers.IO) { ApkUpdater.verifyChecksum(file, game.checksumUrl) }
            val newState = when (result) {
                is ApkUpdater.VerifyResult.Verified ->
                    ApkUpdateState.ReadyToInstall(file, verified = true, checksumAvailable = true)
                is ApkUpdater.VerifyResult.ChecksumUnavailable ->
                    ApkUpdateState.ReadyToInstall(file, verified = false, checksumAvailable = false)
                is ApkUpdater.VerifyResult.Mismatch -> {
                    file.delete()
                    ApkUpdateState.Error
                }
            }
            _gameInstallStates.value = _gameInstallStates.value + (game.id to newState)
        }
    }

    fun installDownloadedGame(gameId: String) {
        val state = _gameInstallStates.value[gameId]
        if (state is ApkUpdateState.ReadyToInstall) {
            ApkUpdater.install(getApplication(), state.file)
        }
    }

    fun isGameInstalled(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        return try {
            getApplication<Application>().packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openInstalledGame(packageName: String?) {
        if (packageName.isNullOrBlank()) return
        val app: Application = getApplication()
        val intent = app.packageManager.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        app.startActivity(intent)
    }
}
