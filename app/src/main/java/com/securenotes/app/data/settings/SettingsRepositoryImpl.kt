package com.securenotes.app.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.securenotes.app.domain.settings.NotificationSound
import com.securenotes.app.domain.settings.SettingsRepository
import com.securenotes.app.domain.settings.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CUSTOM_PHOTO_URI = stringPreferencesKey("custom_photo_uri")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val NOTIFICATION_SOUND = stringPreferencesKey("notification_sound")
        val CUSTOM_SOUND_URI = stringPreferencesKey("custom_sound_uri")
        val NOTIFICATION_VIBRATION_ENABLED = booleanPreferencesKey("notification_vibration_enabled")
        val NOTIFICATION_CHANNEL_VERSION = intPreferencesKey("notification_channel_version")

        val LEGACY_REMINDER_SOUND_ENABLED = booleanPreferencesKey("reminder_sound_enabled")
        val LEGACY_REMINDER_SOUND_URI = stringPreferencesKey("reminder_sound_uri")
    }

    override val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        preferences[Keys.THEME_MODE]
            ?.let { value -> ThemeMode.entries.find { it.name == value } }
            ?: ThemeMode.DARK
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = mode.name
        }
    }

    override val customPhotoUri: Flow<String> = dataStore.data.map { preferences ->
        preferences[Keys.CUSTOM_PHOTO_URI] ?: ""
    }

    override suspend fun setCustomPhotoUri(uri: String?) {
        dataStore.edit { preferences ->
            if (uri.isNullOrBlank()) {
                preferences.remove(Keys.CUSTOM_PHOTO_URI)
            } else {
                preferences[Keys.CUSTOM_PHOTO_URI] = uri
            }
        }
    }

    override val biometricEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[Keys.BIOMETRIC_ENABLED] ?: false
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.BIOMETRIC_ENABLED] = enabled
        }
    }

    override val notificationSound: Flow<NotificationSound> = dataStore.data.map { preferences ->
        preferences[Keys.NOTIFICATION_SOUND]
            ?.let { value -> NotificationSound.entries.find { it.name == value } }
            ?: legacySoundFallback(preferences)
    }

    override suspend fun setNotificationSound(sound: NotificationSound) {
        dataStore.edit { preferences ->
            preferences[Keys.NOTIFICATION_SOUND] = sound.name
        }
    }

    override val customSoundUri: Flow<String> = dataStore.data.map { preferences ->
        preferences[Keys.CUSTOM_SOUND_URI] ?: preferences[Keys.LEGACY_REMINDER_SOUND_URI] ?: ""
    }

    override suspend fun setCustomSoundUri(uri: String?) {
        dataStore.edit { preferences ->
            if (uri.isNullOrBlank()) {
                preferences.remove(Keys.CUSTOM_SOUND_URI)
            } else {
                preferences[Keys.CUSTOM_SOUND_URI] = uri
            }
        }
    }

    override val notificationVibrationEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[Keys.NOTIFICATION_VIBRATION_ENABLED] ?: true
    }

    override suspend fun setNotificationVibrationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.NOTIFICATION_VIBRATION_ENABLED] = enabled
        }
    }

    override val notificationChannelVersion: Flow<Int> = dataStore.data.map { preferences ->
        preferences[Keys.NOTIFICATION_CHANNEL_VERSION] ?: 2
    }

    override suspend fun setNotificationChannelVersion(version: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.NOTIFICATION_CHANNEL_VERSION] = version
        }
    }

    /**
     * Migração das configurações legadas de som do lembrete
     * (`reminder_sound_enabled`/`reminder_sound_uri`) para o novo modelo.
     */
    private fun legacySoundFallback(preferences: Preferences): NotificationSound {
        val legacyUri = preferences[Keys.LEGACY_REMINDER_SOUND_URI].orEmpty()
        val legacyEnabled = preferences[Keys.LEGACY_REMINDER_SOUND_ENABLED] ?: true
        return when {
            legacyUri.isNotBlank() -> NotificationSound.CUSTOM
            !legacyEnabled -> NotificationSound.SILENT
            else -> NotificationSound.SYSTEM
        }
    }
}
