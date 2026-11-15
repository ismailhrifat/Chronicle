package com.chronicle.expense.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

class Settings(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme")

    /** "system", "light" or "dark". */
    val theme: Flow<String> = context.dataStore.data.map { it[themeKey] ?: "system" }

    suspend fun setTheme(mode: String) {
        context.dataStore.edit { it[themeKey] = mode }
    }
}
