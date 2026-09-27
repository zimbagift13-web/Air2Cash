package com.example

import com.example.data.security.SecurityHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testOtpGeneration() {
        val otp = SecurityHelper.generateOtp()
        assertEquals(6, otp.length)
        assertTrue(otp.all { it.isDigit() })
    }

    @Test
    fun testTransactionRefFormat() {
        val ref = SecurityHelper.generateTransactionRef("AIR")
        assertTrue(ref.startsWith("A2C-MW-AIR-"))
    }

    @Test
    fun testReferralCodeGeneration() {
        val code = SecurityHelper.generateReferralCode()
        assertTrue(code.startsWith("A2C-"))
    }

    @Test
    fun testAirtimeMath() {
        val airtimeAmount = 5000.0
        val payoutRate = 78.0 // 78%
        val expectedNetCash = airtimeAmount * (payoutRate / 100.0)
        assertEquals(3900.0, expectedNetCash, 0.001)
    }

    @Test
    fun testWithdrawalFeeMath() {
        val amount = 10000.0
        val percentFee = 1.5 // 1.5%
        val fee = amount * (percentFee / 100.0)
        val netPayout = amount - fee
        assertEquals(150.0, fee, 0.001)
        assertEquals(9850.0, netPayout, 0.001)
    }
}
