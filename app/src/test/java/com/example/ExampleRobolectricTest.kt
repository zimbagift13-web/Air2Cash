package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.security.SecurityHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Air2Cash", appName)
    }

    @Test
    fun testSecurityHelperPasswordHashing() {
        val salt = SecurityHelper.generateSalt()
        val hash = SecurityHelper.hashPassword("secret123", salt)
        assertTrue(SecurityHelper.verifyPassword("secret123", salt, hash))
        assertFalse(SecurityHelper.verifyPassword("wrongpass", salt, hash))
    }

    @Test
    fun testMalawiPhoneNormalizationAndValidation() {
        // Airtel Malawi
        val normalizedAirtel = SecurityHelper.normalizeMalawiPhone("0991234567")
        assertEquals("+265991234567", normalizedAirtel)
        assertTrue(SecurityHelper.isValidMalawiPhone(normalizedAirtel))
        assertEquals("AIRTEL", SecurityHelper.detectNetworkFromPhone(normalizedAirtel))

        // TNM Malawi
        val normalizedTnm = SecurityHelper.normalizeMalawiPhone("0881234567")
        assertEquals("+265881234567", normalizedTnm)
        assertTrue(SecurityHelper.isValidMalawiPhone(normalizedTnm))
        assertEquals("TNM", SecurityHelper.detectNetworkFromPhone(normalizedTnm))

        // Invalid number
        assertFalse(SecurityHelper.isValidMalawiPhone("+265112233445"))
    }
}
