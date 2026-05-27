package com.conkeep.util

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SensitiveLogMaskerTest {
    @Test
    fun `Authorization bearer token is masked`() {
        val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
        val message = "Authorization: Bearer $token"

        val masked = SensitiveLogMasker.mask(message)

        assertFalse(masked.contains(token))
        assertTrue(masked.contains("Authorization: Bearer eyJh...VCJ9"))
    }

    @Test
    fun `known secret fields are masked`() {
        val masterKey = "abcdefghijklmnopqrstuvwxyz123456"
        val fcmToken = "fcm-token-value-1234567890"
        val message = """{"masterKey":"$masterKey","p_fcm_token":"$fcmToken"}"""

        val masked = SensitiveLogMasker.mask(message)

        assertFalse(masked.contains(masterKey))
        assertFalse(masked.contains(fcmToken))
        assertTrue(masked.contains("abcd...3456"))
        assertTrue(masked.contains("fcm-...7890"))
    }
}
