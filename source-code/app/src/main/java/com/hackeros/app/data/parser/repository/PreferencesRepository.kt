package com.hackeros.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
    }

    val themeFlow: Flow<ThemeId> = context.dataStore.data.map { prefs ->
        val saved = prefs[THEME_KEY]
        ThemeId.entries.find { it.name == saved } ?: ThemeId.HACKER
    }

    val languageFlow: Flow<Language> = context.dataStore.data.map { prefs ->
        val saved = prefs[LANG_KEY]
        Language.entries.find { it.name == saved } ?: Language.PL
    }

    val notificationsFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[NOTIFICATIONS_KEY] ?: false
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
}
