package com.securenotes.app.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Criptografia baseada no Android Keystore.
 *
 * A chave AES é gerada dentro do próprio Keystore e não pode ser exportada;
 * o PIN do usuário é protegido por essa chave antes de ser persistido.
 */
class CryptoProviderImpl : CryptoProvider {

    override suspend fun encrypt(data: ByteArray): ByteArray = withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(data)
        iv + ciphertext
    }

    override suspend fun decrypt(data: ByteArray): ByteArray = withContext(Dispatchers.IO) {
        require(data.size > IV_SIZE) { "ciphertext inválido" }
        val iv = data.copyOfRange(0, IV_SIZE)
        val ciphertext = data.copyOfRange(IV_SIZE, data.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.doFinal(ciphertext)
    }

    override fun isAvailable(): Boolean = runCatching {
        val keyStore = KeyStore.getInstance(KEYSTORE)
        keyStore.load(null)
        keyStore.containsAlias(KEY_ALIAS)
    }.getOrDefault(false)

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE)
        keyStore.load(null)
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        private const val KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "secure_notes_master_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val GCM_TAG_BITS = 128
    }
}