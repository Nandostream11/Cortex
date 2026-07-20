package com.cortex.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cortex.app.domain.model.AutonomyMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "cortex_settings")

/**
 * Holds only non-sensitive preferences — autonomy mode, feature toggles, onboarding
 * state. API keys and connector tokens must never be written here; they belong in
 * [com.cortex.app.security.SecretStore]. DataStore's file is not encrypted.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val AUTONOMY_MODE = stringPreferencesKey("autonomy_mode")
        val OPENROUTER_ENABLED = booleanPreferencesKey("openrouter_enabled")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            autonomyMode = prefs[Keys.AUTONOMY_MODE]
                ?.let { runCatching { AutonomyMode.valueOf(it) }.getOrNull() }
                ?: AutonomyMode.PASSIVE,
            openRouterEnabled = prefs[Keys.OPENROUTER_ENABLED] ?: false,
            hasCompletedOnboarding = prefs[Keys.ONBOARDING_COMPLETE] ?: false
        )
    }

    suspend fun setAutonomyMode(mode: AutonomyMode) {
        context.settingsDataStore.edit { it[Keys.AUTONOMY_MODE] = mode.name }
    }

    suspend fun setOpenRouterEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.OPENROUTER_ENABLED] = enabled }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.settingsDataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }
}
