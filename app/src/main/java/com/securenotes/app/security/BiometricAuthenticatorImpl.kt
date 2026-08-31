package com.securenotes.app.security

import android.app.Activity
import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Autenticação biométrica usando a API oficial do Android (BiometricPrompt).
 *
 * Nenhum dado biométrico é capturado ou armazenado pelo aplicativo: o Android
 * cuida de todo o processo de verificação.
 */
class BiometricAuthenticatorImpl(
    private val context: Context,
    private val activityProvider: () -> FragmentActivity?
) : BiometricAuthenticator {

    override fun isAvailable(): Boolean {
        return availability() == BiometricAvailability.AVAILABLE
    }

    override fun availability(): BiometricAvailability {
        return when (BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NO_ENROLLMENT
            else -> BiometricAvailability.UNAVAILABLE
        }
    }

    override suspend fun authenticate(): Boolean = suspendCancellableCoroutine { continuation ->
        val activity = activityProvider()
        if (activity == null || activity.isFinishing) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    continuation.resume(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    continuation.resume(false)
                }

                override fun onAuthenticationFailed() {
                    // permite nova tentativa sem encerrar a autenticação
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(com.securenotes.app.R.string.biometria_prompt_titulo))
            .setSubtitle(context.getString(com.securenotes.app.R.string.biometria_prompt_subtitulo))
            .setNegativeButtonText(context.getString(com.securenotes.app.R.string.cancelar))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        prompt.authenticate(promptInfo)
    }
}