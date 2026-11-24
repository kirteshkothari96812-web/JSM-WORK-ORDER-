package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "jsm_settings")

class AppPreferences(private val context: Context) {
    private object Keys {
        val WEB_APP_URL = stringPreferencesKey("web_app_url")
        val PARTNER_ONE = stringPreferencesKey("partner_one_name")
        val PARTNER_TWO = stringPreferencesKey("partner_two_name")
        val LAST_SYNCED_AT = longPreferencesKey("last_synced_at")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            webAppUrl = prefs[Keys.WEB_APP_URL] ?: AppSettings.DEFAULT_WEB_APP_URL,
            partnerOneName = prefs[Keys.PARTNER_ONE]?.takeIf { it.isNotBlank() } ?: "Kirtesh",
            partnerTwoName = prefs[Keys.PARTNER_TWO]?.takeIf { it.isNotBlank() } ?: "Swapnil",
            lastSyncedAt = prefs[Keys.LAST_SYNCED_AT] ?: 0L
        )
    }

    suspend fun getSettings(): AppSettings = settingsFlow.first()

    suspend fun saveSettings(
        webAppUrl: String,
        partnerOneName: String,
        partnerTwoName: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.WEB_APP_URL] = webAppUrl.trim().ifEmpty { AppSettings.DEFAULT_WEB_APP_URL }
            prefs[Keys.PARTNER_ONE] = partnerOneName.trim().ifEmpty { "Kirtesh" }
            prefs[Keys.PARTNER_TWO] = partnerTwoName.trim().ifEmpty { "Swapnil" }
        }
    }

    suspend fun updateLastSyncedAt(timestampMillis: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SYNCED_AT] = timestampMillis
        }
    }
}
