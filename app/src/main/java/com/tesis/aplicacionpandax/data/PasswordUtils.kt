package com.tesis.aplicacionpandax.data

import java.util.Base64 // <-- Usa la versión de Java
import java.security.MessageDigest
import java.security.SecureRandom

object PasswordUtils {
    private const val SALT_LEN = 16

    private fun generateSalt(): String {
        val sr = SecureRandom()
        val salt = ByteArray(SALT_LEN)
        sr.nextBytes(salt)

        return Base64.getEncoder().encodeToString(salt)
    }

    private fun sha256(input: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(input)
    }

    fun hashPasswordWithSalt(password: String): Pair<String, String> {
        val salt = generateSalt()

        val combined = password.toByteArray(Charsets.UTF_8) + Base64.getDecoder().decode(salt)
        val hash = sha256(combined)

        return Pair(salt, Base64.getEncoder().encodeToString(hash))
    }

    fun verify(password: String, salt: String, expectedHash: String): Boolean {

        val combined = password.toByteArray(Charsets.UTF_8) + Base64.getDecoder().decode(salt)
        val hash = sha256(combined)

        val encoded = Base64.getEncoder().encodeToString(hash)
        return encoded == expectedHash
    }
}