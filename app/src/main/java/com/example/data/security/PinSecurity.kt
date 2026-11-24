package com.example.data.security

import java.security.MessageDigest
import java.security.SecureRandom

object PinSecurity {
    private val random = SecureRandom()

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.toHex()
    }

    fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = "$salt:$pin".toByteArray(Charsets.UTF_8)
        val hashed = digest.digest(input)
        return hashed.toHex()
    }

    fun verifyPin(pin: String, salt: String, expectedHash: String): Boolean {
        val computed = hashPin(pin, salt).toByteArray(Charsets.UTF_8)
        val expected = expectedHash.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(computed, expected)
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
