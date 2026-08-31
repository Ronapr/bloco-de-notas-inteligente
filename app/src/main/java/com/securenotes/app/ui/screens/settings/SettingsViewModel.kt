package com.securenotes.app.ui.screens.settings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securenotes.app.R
import com.securenotes.app.domain.settings.NotificationSound
import com.securenotes.app.domain.settings.SettingsRepository
import com.securenotes.app.domain.settings.ThemeMode
import com.securenotes.app.reminder.ReminderScheduler
import com.securenotes.app.security.BiometricAuthenticator
import com.securenotes.app.security.BiometricAvailability
import com.securenotes.app.security.SecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val biometricEnabled: Boolean = false,
    val biometricAvailability: BiometricAvailability = BiometricAvailability.UNAVAILABLE,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val customPhotoUri: String = "",
    val notificationSound: NotificationSound = NotificationSound.SYSTEM,
    val customSoundUri: String = "",
    val vibrationEnabled: Boolean = true,
    val notificationChannelVersion: Int = 2,
    val showChangePinForm: Boolean = false,
    val currentPin: String = "",
    val newPin: String = "",
    val confirmNewPin: String = "",
    val showBiometricOffConfirm: Boolean = false,
    val confirmPinToDisable: String = "",
    @StringRes val error: Int? = null,
    @StringRes val message: Int? = null,
    val requestBiometricAuth: Boolean = false,
    val busy: Boolean = false
)

class SettingsViewModel(
    private val securityManager: SecurityManager,
    private val biometricAuthenticator: BiometricAuthenticator,
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(biometricAvailability = biometricAuthenticator.availability())
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val enabled = securityManager.biometricEnabled.first()
            val theme = settingsRepository.themeMode.first()
            val photoUri = settingsRepository.customPhotoUri.first()
            val sound = settingsRepository.notificationSound.first()
            val soundUri = settingsRepository.customSoundUri.first()
            val vibration = settingsRepository.notificationVibrationEnabled.first()
            val channelVersion = settingsRepository.notificationChannelVersion.first()
            _uiState.update {
                it.copy(
                    biometricEnabled = enabled,
                    themeMode = theme,
                    customPhotoUri = photoUri,
                    notificationSound = sound,
                    customSoundUri = soundUri,
                    vibrationEnabled = vibration,
                    notificationChannelVersion = channelVersion
                )
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
            _uiState.update { it.copy(themeMode = mode) }
        }
    }

    fun setCustomPhotoUri(uri: String?) {
        viewModelScope.launch {
            settingsRepository.setCustomPhotoUri(uri)
            _uiState.update { it.copy(customPhotoUri = uri.orEmpty()) }
            if (!uri.isNullOrBlank()) {
                settingsRepository.setThemeMode(ThemeMode.GALLERY)
                _uiState.update { it.copy(themeMode = ThemeMode.GALLERY) }
            }
        }
    }

    fun setSound(sound: NotificationSound) {
        viewModelScope.launch {
            settingsRepository.setNotificationSound(sound)
            _uiState.update { it.copy(notificationSound = sound) }
            reminderScheduler.refreshChannel(force = true)
        }
    }

    fun setCustomSoundUri(uri: String?) {
        viewModelScope.launch {
            settingsRepository.setCustomSoundUri(uri)
            _uiState.update { it.copy(customSoundUri = uri.orEmpty()) }
            if (!uri.isNullOrBlank()) {
                settingsRepository.setNotificationSound(NotificationSound.CUSTOM)
                _uiState.update { it.copy(notificationSound = NotificationSound.CUSTOM) }
            }
            reminderScheduler.refreshChannel(force = true)
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationVibrationEnabled(enabled)
            _uiState.update { it.copy(vibrationEnabled = enabled) }
            reminderScheduler.refreshChannel(force = true)
        }
    }

    fun toggleChangePinForm() {
        _uiState.update {
            it.copy(
                showChangePinForm = !it.showChangePinForm,
                currentPin = "",
                newPin = "",
                confirmNewPin = "",
                error = null,
                message = null
            )
        }
    }

    fun onCurrentPinChange(value: String) {
        _uiState.update { it.copy(currentPin = value, error = null) }
    }

    fun onNewPinChange(value: String) {
        _uiState.update { it.copy(newPin = value, error = null) }
    }

    fun onConfirmNewPinChange(value: String) {
        _uiState.update { it.copy(confirmNewPin = value, error = null) }
    }

    fun savePinChange() {
        val state = _uiState.value
        if (state.busy) return
        if (state.currentPin.isEmpty() || state.newPin.isEmpty() || state.confirmNewPin.isEmpty()) {
            return
        }
        if (state.newPin.length < SecurityManager.MIN_PIN_LENGTH) {
            _uiState.update { it.copy(error = R.string.pin_invalido_tamanho) }
            return
        }
        if (state.newPin != state.confirmNewPin) {
            _uiState.update { it.copy(error = R.string.pins_nao_coincidem) }
            return
        }
        _uiState.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            val changed = securityManager.changePin(state.currentPin, state.newPin)
            _uiState.update {
                it.copy(
                    busy = false,
                    currentPin = "",
                    newPin = "",
                    confirmNewPin = "",
                    showChangePinForm = false,
                    error = if (changed) null else R.string.pin_incorreto,
                    message = if (changed) R.string.pin_alterado_sucesso else null
                )
            }
        }
    }

    fun onConfigureBiometric() {
        val state = _uiState.value
        if (state.busy) return
        when (state.biometricAvailability) {
            BiometricAvailability.AVAILABLE -> {
                if (!state.biometricEnabled) {
                    _uiState.update { it.copy(requestBiometricAuth = true, message = null) }
                }
            }
            BiometricAvailability.NO_ENROLLMENT -> {
                _uiState.update { it.copy(message = R.string.sem_biometria_cadastrada) }
            }
            BiometricAvailability.UNAVAILABLE -> {
                _uiState.update { it.copy(message = R.string.biometria_indisponivel) }
            }
        }
    }

    fun onBiometricToggleRequested(enabled: Boolean) {
        if (_uiState.value.busy) return
        if (enabled) {
            onConfigureBiometric()
        } else {
            _uiState.update {
                it.copy(
                    showBiometricOffConfirm = true,
                    confirmPinToDisable = "",
                    error = null,
                    message = null
                )
            }
        }
    }

    fun onBiometricAuthResult(success: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(requestBiometricAuth = false) }
            if (success) {
                securityManager.setBiometricEnabled(true)
                _uiState.update {
                    it.copy(biometricEnabled = true, message = R.string.biometria_ativada)
                }
            }
        }
    }

    fun onConfirmPinToDisableChange(value: String) {
        _uiState.update { it.copy(confirmPinToDisable = value, error = null) }
    }

    fun confirmDisableBiometric() {
        val pin = _uiState.value.confirmPinToDisable
        if (pin.isEmpty()) return
        viewModelScope.launch {
            val valid = securityManager.verifyPin(pin)
            if (valid) {
                securityManager.setBiometricEnabled(false)
                _uiState.update {
                    it.copy(
                        biometricEnabled = false,
                        showBiometricOffConfirm = false,
                        confirmPinToDisable = "",
                        message = R.string.biometria_desativada
                    )
                }
            } else {
                _uiState.update {
                    it.copy(confirmPinToDisable = "", error = R.string.pin_incorreto)
                }
            }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun cancelDisableBiometric() {
        _uiState.update {
            it.copy(
                showBiometricOffConfirm = false,
                confirmPinToDisable = "",
                error = null
            )
        }
    }
}
