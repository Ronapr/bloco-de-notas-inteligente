package com.securenotes.app.security

/** Situação da biometria no dispositivo. */
enum class BiometricAvailability {
    /** Dispositivo possui biometria e há biometria cadastrada no sistema. */
    AVAILABLE,

    /** Dispositivo possui biometria, mas nenhuma impressão digital/rosto cadastrada. */
    NO_ENROLLMENT,

    /** Dispositivo não possui biometria disponível (sem hardware ou indisponível). */
    UNAVAILABLE
}

interface BiometricAuthenticator {
    /** True se a biometria está disponível E há biometria cadastrada. */
    fun isAvailable(): Boolean

    /** Situação detalhada da biometria no dispositivo. */
    fun availability(): BiometricAvailability

    suspend fun authenticate(): Boolean
}
