package com.securenotes.app.ui.screens.security

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securenotes.app.R
import com.securenotes.app.security.BiometricAuthenticator
import com.securenotes.app.security.SecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SecuritySetupUiState(
    val pin: String = "",
    val confirmPin: String = "",
    val biometricEnabledCandidate: Boolean = false,
    val biometricAvailable: Boolean = false,
    @StringRes val error: Int? = null,
    val busy: Boolean = false,
    val requestBiometricAuth: Boolean = false
)

class SecuritySetupViewModel(
    private val securityManager: SecurityManager,
    private val biometricAuthenticator: BiometricAuthenticator
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SecuritySetupUiState(biometricAvailable = biometricAuthenticator.isAvailable())
    )
    val uiState: StateFlow<SecuritySetupUiState> = _uiState.asStateFlow()

    private var onSetupCompleted: (() -> Unit)? = null

    fun onPinChange(pin: String) {
        _uiState.update { it.copy(pin = pin, error = null) }
    }

    fun onConfirmPinChange(confirmPin: String) {
        _uiState.update { it.copy(confirmPin = confirmPin, error = null) }
    }

    fun onBiometricCandidateChange(enabled: Boolean) {
        _uiState.update { it.copy(biometricEnabledCandidate = enabled) }
    }

    fun save(onComplete: () -> Unit) {
        val state = _uiState.value
        if (state.busy) return
        if (state.pin.length < SecurityManager.MIN_PIN_LENGTH) {
            _uiState.update { it.copy(error = R.string.pin_invalido_tamanho) }
            return
        }
        if (state.pin != state.confirmPin) {
            _uiState.update { it.copy(error = R.string.pins_nao_coincidem) }
            return
        }
        onSetupCompleted = onComplete
        _uiState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val saved = securityManager.setPin(state.pin)
            if (!saved) {
                _uiState.update { it.copy(busy = false, error = R.string.pin_invalido_tamanho) }
                return@launch
            }
            if (state.biometricEnabledCandidate && state.biometricAvailable) {
                _uiState.update { it.copy(requestBiometricAuth = true) }
            } else {
                finishSetup()
            }
        }
    }

    fun onBiometricAuthResult(success: Boolean) {
        _uiState.update { it.copy(requestBiometricAuth = false) }
        viewModelScope.launch {
            if (success) {
                securityManager.setBiometricEnabled(true)
            }
            finishSetup()
        }
    }

    private fun finishSetup() {
        onSetupCompleted?.invoke()
        onSetupCompleted = null
    }
}