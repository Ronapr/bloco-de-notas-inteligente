package com.securenotes.app.security

interface CryptoProvider {
    suspend fun encrypt(data: ByteArray): ByteArray
    suspend fun decrypt(data: ByteArray): ByteArray
    fun isAvailable(): Boolean
}
