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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LockUiState(
    val pin: String = "",
    @StringRes val error: Int? = null,
    val showBiometric: Boolean = false,
    val requestBiometric: Boolean = false,
    val busy: Boolean = false,
    val usePin: Boolean = false
)

class LockViewModel(
    private val securityManager: SecurityManager,
    private val biometricAuthenticator: BiometricAuthenticator
) : ViewModel() {

    private val _uiState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val enabled = securityManager.biometricEnabled.first()
            val biometric = enabled && biometricAuthenticator.isAvailable()
            _uiState.update {
                it.copy(
                    showBiometric = biometric,
                    usePin = !biometric
                )
            }
        }
    }

    fun onPinChange(pin: String) {
        _uiState.update { it.copy(pin = pin, error = null) }
    }

    fun unlock() {
        val state = _uiState.value
        if (state.busy || state.pin.isEmpty()) return
        _uiState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val valid = securityManager.verifyPin(state.pin)
            _uiState.update { it.copy(busy = false) }
            if (valid) {
                securityManager.unlock()
            } else {
                _uiState.update {
                    it.copy(pin = "", error = R.string.pin_incorreto)
                }
            }
        }
    }

    fun useBiometric() {
        if (!_uiState.value.showBiometric || _uiState.value.busy) return
        _uiState.update { it.copy(requestBiometric = true, error = null) }
    }

    /** Mostra o formulário de PIN como alternativa à biometria. */
    fun usePin() {
        _uiState.update { it.copy(usePin = true) }
    }

    fun onBiometricResult(success: Boolean) {
        _uiState.update { it.copy(requestBiometric = false) }
        if (success) {
            securityManager.unlock()
        }
    }
}