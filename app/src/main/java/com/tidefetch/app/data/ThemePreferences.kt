package com.tidefetch.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode(val displayName: String) {
    SYSTEM("System default"),
    LIGHT("Light"),
    DARK("Dark"),
}

private val Context.themeDataStore by preferencesDataStore(name = "appearance")

class ThemePreferences(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")

    val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { preferences ->
        preferences[themeKey]
            ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
            ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { preferences -> preferences[themeKey] = mode.name }
    }
}
