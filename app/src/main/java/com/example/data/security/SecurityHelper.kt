package com.example.data.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

object SecurityHelper {
    private val secureRandom = SecureRandom()

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPassword(password: String, salt: String): String {
        val input = password + salt
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(password: String, salt: String, expectedHash: String): Boolean {
        val hash = hashPassword(password, salt)
        return hash.equals(expectedHash, ignoreCase = true)
    }

    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(("air2cash_pin_" + pin).toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun generateOtp(): String {
        val otpNum = 100000 + secureRandom.nextInt(900000)
        return otpNum.toString()
    }

    fun generateReferralCode(prefix: String = "A2C"): String {
        val allowedChars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        val code = (1..6)
            .map { allowedChars[secureRandom.nextInt(allowedChars.length)] }
            .joinToString("")
        return "$prefix-$code"
    }

    fun generateTransactionRef(prefix: String): String {
        val timestamp = System.currentTimeMillis() % 1000000
        val randomPart = 1000 + secureRandom.nextInt(9000)
        return "A2C-MW-$prefix-$timestamp$randomPart"
    }

    /**
     * Sanitizes and standardizes Malawi phone numbers into standard format:
     * e.g. 0991234567 -> +265991234567
     * 0881234567 -> +265881234567
     * 991234567 -> +265991234567
     */
    fun normalizeMalawiPhone(raw: String): String {
        val cleaned = raw.replace("\\s+".toRegex(), "").replace("-", "")
        return when {
            cleaned.startsWith("+265") -> cleaned
            cleaned.startsWith("265") -> "+$cleaned"
            cleaned.startsWith("0") && cleaned.length == 10 -> "+265" + cleaned.substring(1)
            cleaned.length == 9 -> "+265$cleaned"
            else -> cleaned
        }
    }

    fun isValidMalawiPhone(phone: String): Boolean {
        val normalized = normalizeMalawiPhone(phone)
        // Malawi mobile numbers have 9 digits after +265:
        // Airtel: 99xxxxxxx, 98xxxxxxx, 90xxxxxxx
        // TNM: 88xxxxxxx, 89xxxxxxx, 80xxxxxxx
        val regex = "^\\+265(99|98|90|88|89|80)\\d{7}$".toRegex()
        return regex.matches(normalized)
    }

    fun detectNetworkFromPhone(phone: String): String? {
        val normalized = normalizeMalawiPhone(phone)
        return when {
            normalized.startsWith("+26599") || normalized.startsWith("+26598") || normalized.startsWith("+26590") -> "AIRTEL"
            normalized.startsWith("+26588") || normalized.startsWith("+26589") || normalized.startsWith("+26580") -> "TNM"
            else -> null
        }
    }
}
