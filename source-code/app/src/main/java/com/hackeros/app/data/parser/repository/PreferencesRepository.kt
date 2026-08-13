package com.hackeros.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hackeros.app.data.model.Language
import com.hackeros.app.data.model.ThemeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hackeros_prefs")

class PreferencesRepository(private val context: Context) {

    companion object {
        val THEME_KEY = stringPreferencesKey("hackeros_theme")
        val LANG_KEY = stringPreferencesKey("hackeros_lang")
        val NOTIFICATIONS_KEY = booleanPreferencesKey("hackeros_notifications")
        val LAST_KNOWN_VERSION_KEY = stringPreferencesKey("hackeros_last_known_version")

        // Offline cache: last successfully fetched releases/gallery, so the app can still show
        // *something* instead of an empty error screen when there's no connection.
        val CACHED_RELEASES_JSON_KEY = stringPreferencesKey("hackeros_cached_releases_json")
        val CACHED_RELEASES_LANG_KEY = stringPreferencesKey("hackeros_cached_releases_lang")
        val CACHED_GALLERY_JSON_KEY = stringPreferencesKey("hackeros_cached_gallery_json")

        // Per-edition release-notification filter. Absent key = "notify for every edition"
        // (the original, backward-compatible behavior). Once the user customizes it, this
        // holds exactly the edition names (e.g. "HackerOS Official") they want to hear about.
        val WATCHED_EDITIONS_KEY = stringSetPreferencesKey("hackeros_watched_editions")
        val HAS_CUSTOM_EDITION_FILTER_KEY = booleanPreferencesKey("hackeros_has_custom_edition_filter")

        // Tracks which app version the "What's new" dialog was last shown for, so it only
        // appears once per update (not on every launch).
        val LAST_SEEN_APP_VERSION_KEY = stringPreferencesKey("hackeros_last_seen_app_version")
    }

    val themeFlow: Flow<ThemeId> = context.dataStore.data.map { prefs ->
        val saved = prefs[THEME_KEY]
        ThemeId.entries.find { it.name == saved } ?: ThemeId.MONOCHROME
    }

    val languageFlow: Flow<Language> = context.dataStore.data.map { prefs ->
        val saved = prefs[LANG_KEY]
        Language.entries.find { it.name == saved } ?: Language.PL
    }

    val notificationsFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[NOTIFICATIONS_KEY] ?: false
    }

    // Version of the release that was last seen (either shown in-app or already notified
    // about), used by the background worker to detect genuinely *new* releases and avoid
    // notifying about a version the user has already seen.
    val lastKnownVersionFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[LAST_KNOWN_VERSION_KEY]
    }

    // --- Offline cache --------------------------------------------------------------------

    data class CachedReleases(val json: String, val language: Language?)

    val cachedReleasesFlow: Flow<CachedReleases> = context.dataStore.data.map { prefs ->
        val lang = Language.entries.find { it.name == prefs[CACHED_RELEASES_LANG_KEY] }
        CachedReleases(prefs[CACHED_RELEASES_JSON_KEY] ?: "", lang)
    }

    val cachedGalleryJsonFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[CACHED_GALLERY_JSON_KEY]
    }

    suspend fun saveCachedReleases(json: String, language: Language) {
        context.dataStore.edit {
            it[CACHED_RELEASES_JSON_KEY] = json
            it[CACHED_RELEASES_LANG_KEY] = language.name
        }
    }

    suspend fun saveCachedGallery(json: String) {
        context.dataStore.edit { it[CACHED_GALLERY_JSON_KEY] = json }
    }

    // --- Per-edition notification filter ---------------------------------------------------

    /** Null = notify for every edition (default/backward-compatible behavior). */
    val watchedEditionsFlow: Flow<Set<String>?> = context.dataStore.data.map { prefs ->
        if (prefs[HAS_CUSTOM_EDITION_FILTER_KEY] == true) prefs[WATCHED_EDITIONS_KEY] ?: emptySet()
        else null
    }

    suspend fun saveWatchedEditions(editions: Set<String>?) {
        context.dataStore.edit {
            if (editions == null) {
                it[HAS_CUSTOM_EDITION_FILTER_KEY] = false
                it.remove(WATCHED_EDITIONS_KEY)
            } else {
                it[HAS_CUSTOM_EDITION_FILTER_KEY] = true
                it[WATCHED_EDITIONS_KEY] = editions
            }
        }
    }

    // --- "What's new" dialog ---------------------------------------------------------------

    val lastSeenAppVersionFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[LAST_SEEN_APP_VERSION_KEY]
    }

    suspend fun saveLastSeenAppVersion(version: String) {
        context.dataStore.edit { it[LAST_SEEN_APP_VERSION_KEY] = version }
    }

    suspend fun saveTheme(themeId: ThemeId) {
        context.dataStore.edit { it[THEME_KEY] = themeId.name }
    }

    suspend fun saveLanguage(lang: Language) {
        context.dataStore.edit { it[LANG_KEY] = lang.name }
    }

    suspend fun saveNotifications(enabled: Boolean) {
        context.dataStore.edit { it[NOTIFICATIONS_KEY] = enabled }
    }

    suspend fun saveLastKnownVersion(version: String) {
        context.dataStore.edit { it[LAST_KNOWN_VERSION_KEY] = version }
    }
}
