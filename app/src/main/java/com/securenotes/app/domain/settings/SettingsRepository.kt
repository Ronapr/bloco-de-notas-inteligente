package com.securenotes.app.domain.settings

import kotlinx.coroutines.flow.Flow

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    VOLCANIC,
    FLORAL,
    GALLERY
}

enum class NotificationSound {
    SYSTEM,
    RINGTONE,
    CUSTOM,
    SILENT
}

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)

    val customPhotoUri: Flow<String>
    suspend fun setCustomPhotoUri(uri: String?)

    val biometricEnabled: Flow<Boolean>
    suspend fun setBiometricEnabled(enabled: Boolean)

    val notificationSound: Flow<NotificationSound>
    suspend fun setNotificationSound(sound: NotificationSound)

    val customSoundUri: Flow<String>
    suspend fun setCustomSoundUri(uri: String?)

    val notificationVibrationEnabled: Flow<Boolean>
    suspend fun setNotificationVibrationEnabled(enabled: Boolean)

    val notificationChannelVersion: Flow<Int>
    suspend fun setNotificationChannelVersion(version: Int)
}
