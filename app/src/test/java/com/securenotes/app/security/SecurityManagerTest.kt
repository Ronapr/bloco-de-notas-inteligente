package com.securenotes.app.security

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SecurityManagerTest {

    private fun createSecurityManager(
        scope: CoroutineScope
    ): SecurityManager {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = {
                File(createTempDir(), "security_test.preferences_pb")
            }
        )
        return SecurityManager(InMemoryCryptoProvider(), dataStore)
    }

    @Test
    fun `setPin stores encrypted pin and never plaintext`() = runTest {
        val security = createSecurityManager(backgroundScope)

        assertFalse(security.hasPin.first())
        assertTrue(security.setPin("1234"))

        assertTrue(security.hasPin.first())
        val prefs = security.dataStore.data.first()
        val stored = prefs[stringPreferencesKey("pin_sealed")]
        assertTrue("PIN armazenado não deve ser texto puro", stored != null)
        assertNotEquals("1234", stored)
        assertFalse(stored!!.contains("1234"))
    }

    @Test
    fun `verifyPin accepts correct pin and rejects wrong pin`() = runTest {
        val security = createSecurityManager(backgroundScope)
        security.setPin("9876")

        assertTrue(security.verifyPin("9876"))
        assertFalse(security.verifyPin("9875"))
        assertFalse(security.verifyPin(""))
        assertFalse(security.verifyPin("98765"))
    }

    @Test
    fun `setPin rejects invalid pins`() = runTest {
        val security = createSecurityManager(backgroundScope)

        assertFalse(security.setPin("123"))
        assertFalse(security.setPin("123456789"))
        assertFalse(security.setPin("12a4"))
        assertFalse(security.verifyPin("123"))
        assertFalse(security.hasPin.first())
    }

    @Test
    fun `changePin requires valid current pin`() = runTest {
        val security = createSecurityManager(backgroundScope)
        security.setPin("1111")

        assertFalse(security.changePin("9999", "2222"))
        assertTrue(security.verifyPin("1111"))

        assertTrue(security.changePin("1111", "3333"))
        assertFalse("PIN antigo deve deixar de funcionar", security.verifyPin("1111"))
        assertTrue("Novo PIN deve funcionar", security.verifyPin("3333"))
    }

    @Test
    fun `lock and unlock change authenticated state`() = runTest {
        val security = createSecurityManager(backgroundScope)

        assertFalse(security.authenticated.value)
        security.unlock()
        assertTrue(security.authenticated.value)
        security.lock()
        assertFalse(security.authenticated.value)
    }

    @Test
    fun `setPin leaves user authenticated`() = runTest {
        val security = createSecurityManager(backgroundScope)
        val lockedBefore = security.authenticated.value

        security.setPin("4321")

        assertTrue(security.authenticated.value)
        assertEquals(false, lockedBefore)
    }

    @Test
    fun `biometric enabled flag persists`() = runTest {
        val security = createSecurityManager(backgroundScope)

        assertFalse(security.biometricEnabled.first())
        security.setBiometricEnabled(true)
        assertTrue(security.biometricEnabled.first())
        security.setBiometricEnabled(false)
        assertFalse(security.biometricEnabled.first())
    }

    @Test
    fun `verifyPin is false when no pin configured`() = runTest {
        val security = createSecurityManager(backgroundScope)
        assertFalse(security.verifyPin("1234"))
    }
}