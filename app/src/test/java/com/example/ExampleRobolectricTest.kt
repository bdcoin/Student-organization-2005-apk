package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.security.CryptoVault
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context and verify AES-256-GCM encryption roundtrip`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Friendship Zone", appName)

        val sensitiveNid = "19881234567890123"
        val ciphertext = CryptoVault.encrypt(sensitiveNid)
        assertTrue(ciphertext.startsWith("ENCv1:"))
        assertNotEquals(sensitiveNid, ciphertext)
        assertEquals(sensitiveNid, CryptoVault.decrypt(ciphertext))
    }
}
