package com.securenotes.app.security

import java.io.File
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

/**
 * CryptoProvider de teste: AES-GCM na JVM (sem Android Keystore),
 * mantendo o comportamento de cifrar/decifrar idêntico ao de produção.
 */
class InMemoryCryptoProvider : CryptoProvider {

    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    override suspend fun encrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher.iv + cipher.doFinal(data)
    }

    override suspend fun decrypt(data: ByteArray): ByteArray {
        require(data.size > 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(128, data.copyOfRange(0, 12))
        )
        return cipher.doFinal(data.copyOfRange(12, data.size))
    }

    override fun isAvailable(): Boolean = true
}