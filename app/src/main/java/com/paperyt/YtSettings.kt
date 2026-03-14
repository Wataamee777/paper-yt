package com.paperyt

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "yt_settings")

data class YtSettings(
    val customCommandEnabled: Boolean = false,
    val customCommand: String = "",
    val downloadDirectory: String = "",
    val outputExtension: String = "mp4",
    val embedMetadata: Boolean = true,
    val useSponsorBlock: Boolean = false
)

class SettingsRepository(private val context: Context) {
    val settings: Flow<YtSettings> = context.dataStore.data.map { prefs ->
        YtSettings(
            customCommandEnabled = prefs[KEY_CUSTOM_COMMAND_ENABLED] ?: false,
            customCommand = prefs[KEY_CUSTOM_COMMAND] ?: "",
            downloadDirectory = prefs[KEY_DIR] ?: "",
            outputExtension = prefs[KEY_EXTENSION] ?: "mp4",
            embedMetadata = prefs[KEY_EMBED_METADATA] ?: true,
            useSponsorBlock = prefs[KEY_SPONSORBLOCK] ?: false
        )
    }

    suspend fun update(settings: YtSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CUSTOM_COMMAND_ENABLED] = settings.customCommandEnabled
            prefs[KEY_CUSTOM_COMMAND] = settings.customCommand
            prefs[KEY_DIR] = settings.downloadDirectory
            prefs[KEY_EXTENSION] = settings.outputExtension
            prefs[KEY_EMBED_METADATA] = settings.embedMetadata
            prefs[KEY_SPONSORBLOCK] = settings.useSponsorBlock
        }
    }

    companion object {
        private val KEY_CUSTOM_COMMAND_ENABLED = booleanPreferencesKey("custom_command_enabled")
        private val KEY_CUSTOM_COMMAND = stringPreferencesKey("custom_command")
        private val KEY_DIR = stringPreferencesKey("download_directory")
        private val KEY_EXTENSION = stringPreferencesKey("output_extension")
        private val KEY_EMBED_METADATA = booleanPreferencesKey("embed_metadata")
        private val KEY_SPONSORBLOCK = booleanPreferencesKey("sponsorblock")
    }
}
