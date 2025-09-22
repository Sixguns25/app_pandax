package com.tesis.aplicacionpandax.data

import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

object PasswordUtils {
    private const val SALT_LEN = 16

    private fun generateSalt(): String {
        val sr = SecureRandom()
        val salt = ByteArray(SALT_LEN)
        sr.nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    private fun sha256(input: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(input)
    }

    fun hashPasswordWithSalt(password: String): Pair<String, String> {
        val salt = generateSalt()
        val combined = password.toByteArray(Charsets.UTF_8) + Base64.decode(salt, Base64.NO_WRAP)
        val hash = sha256(combined)
        return Pair(salt, Base64.encodeToString(hash, Base64.NO_WRAP))
    }

    fun verify(password: String, salt: String, expectedHash: String): Boolean {
        val combined = password.toByteArray(Charsets.UTF_8) + Base64.decode(salt, Base64.NO_WRAP)
        val hash = sha256(combined)
        val encoded = Base64.encodeToString(hash, Base64.NO_WRAP)
        return encoded == expectedHash
    }
}
