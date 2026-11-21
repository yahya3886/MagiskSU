package com.magisksu.manager.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** UI theme selection: follow system, always light or always dark. */
enum class ThemeMode(val stored: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun from(value: String?): ThemeMode =
            entries.firstOrNull { it.stored == value } ?: SYSTEM
    }
}

private val Context.magiskSUDataStore by preferencesDataStore(name = "magisk_su_settings")

/** Persistent UI preferences backed by Jetpack DataStore. */
class SettingsStore(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }

    val themeMode: Flow<ThemeMode> = context.magiskSUDataStore.data.map { prefs ->
        ThemeMode.from(prefs[Keys.THEME])
    }

    val dynamicColor: Flow<Boolean> = context.magiskSUDataStore.data.map { prefs ->
        prefs[Keys.DYNAMIC_COLOR] ?: true
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.magiskSUDataStore.edit { it[Keys.THEME] = mode.stored }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.magiskSUDataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }
}
