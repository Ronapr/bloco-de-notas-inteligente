package com.securenotes.app.security

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.util.Base64
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Gerencia a segurança do aplicativo: PIN, biometria e estado de bloqueio.
 *
 * O PIN nunca é salvo em texto puro: ele é cifrado com a chave do Android
 * Keystore (CryptoProvider) antes de ser persistido no DataStore local.
 */
class SecurityManager(
    private val cryptoProvider: CryptoProvider,
    internal val dataStore: DataStore<Preferences>
) {

    private object Keys {
        val PIN_SEALED = stringPreferencesKey("pin_sealed")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    }

    /** Verdadeiro quando o usuário já cadastrou um PIN. */
    val hasPin: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[Keys.PIN_SEALED] != null
    }

    /** Verdadeiro quando o desbloqueio por biometria está habilitado. */
    val biometricEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[Keys.BIOMETRIC_ENABLED] ?: false
    }

    private val _authenticated = MutableStateFlow(false)

    /** Estado de autenticação: falso (bloqueado) enquanto o usuário não se autenticar. */
    val authenticated: StateFlow<Boolean> = _authenticated.asStateFlow()

    /** Bloqueia o acesso (usado quando o aplicativo sai de primeiro plano). */
    fun lock() {
        _authenticated.value = false
    }

    /** Libera o acesso após autenticação válida. */
    fun unlock() {
        _authenticated.value = true
    }

    /**
     * Cadastra um novo PIN. Retorna false quando o PIN é inválido
     * (deve ter entre 4 e 8 dígitos).
     */
    suspend fun setPin(pin: String): Boolean {
        if (!isValidPin(pin)) return false
        val sealed = Base64.getEncoder().withoutPadding()
            .encodeToString(cryptoProvider.encrypt(pin.toByteArray(Charsets.UTF_8)))
        dataStore.edit { preferences ->
            preferences[Keys.PIN_SEALED] = sealed
        }
        _authenticated.value = true
        return true
    }

    /** Verifica o PIN informado contra o PIN armazenado (de forma segura). */
    suspend fun verifyPin(pin: String): Boolean {
        val sealed = dataStore.data.first()[Keys.PIN_SEALED] ?: return false
        val sealedBytes = runCatching {
            Base64.getDecoder().decode(sealed)
        }.getOrNull() ?: return false
        val expected = runCatching {
            cryptoProvider.decrypt(sealedBytes).toString(Charsets.UTF_8)
        }.getOrNull() ?: return false
        return constantTimeEquals(expected, pin)
    }

    /**
     * Altera o PIN. Exige o PIN atual válido; o novo PIN substitui o anterior.
     */
    suspend fun changePin(currentPin: String, newPin: String): Boolean {
        if (!verifyPin(currentPin)) return false
        return setPin(newPin)
    }

    /** Ativa/desativa o desbloqueio por biometria. */
    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.BIOMETRIC_ENABLED] = enabled
        }
    }

    private fun isValidPin(pin: String): Boolean =
        pin.length in MIN_PIN_LENGTH..MAX_PIN_LENGTH && pin.all { it.isDigit() }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        val bytesA = a.toByteArray(Charsets.UTF_8)
        val bytesB = b.toByteArray(Charsets.UTF_8)
        var diff = bytesA.size xor bytesB.size
        for (i in bytesA.indices) {
            diff = diff or (bytesA[i].toInt() xor bytesB.getOrElse(i) { 0 }.toInt())
        }
        return diff == 0
    }

    companion object {
        const val MIN_PIN_LENGTH = 4
        const val MAX_PIN_LENGTH = 8
    }
}