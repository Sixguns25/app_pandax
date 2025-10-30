package com.tesis.aplicacionpandax

import com.tesis.aplicacionpandax.data.PasswordUtils
import org.junit.Assert.assertFalse // Importar Assert
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test // Importar Test

/**
 * Pruebas unitarias para PasswordUtils.
 * Estas pruebas se ejecutan en la JVM local.
 */
class PasswordUtilsTest {

    @Test
    fun `hashPasswordWithSalt genera un hash y salt diferentes cada vez`() {
        val password = "admin123"

        val (salt1, hash1) = PasswordUtils.hashPasswordWithSalt(password)
        val (salt2, hash2) = PasswordUtils.hashPasswordWithSalt(password)

        // Comprueba que los salts sean diferentes
        assertNotEquals("Los salts no deberían ser iguales", salt1, salt2)
        // Comprueba que los hashes (basados en diferentes salts) sean diferentes
        assertNotEquals("Los hashes no deberían ser iguales", hash1, hash2)
    }

    @Test
    fun `verify funciona con una contraseña correcta`() {
        val password = "mi_password_segura"
        val (salt, hash) = PasswordUtils.hashPasswordWithSalt(password)

        // Comprueba que la verificación sea exitosa (true)
        val esValido = PasswordUtils.verify(password, salt, hash)
        assertTrue("La verificación debería ser exitosa con la contraseña correcta", esValido)
    }

    @Test
    fun `verify falla con una contraseña incorrecta`() {
        val password = "mi_password_segura"
        val passwordIncorrecta = "password_incorrecto"
        val (salt, hash) = PasswordUtils.hashPasswordWithSalt(password)

        // Comprueba que la verificación falle (false)
        val esValido = PasswordUtils.verify(passwordIncorrecta, salt, hash)
        assertFalse("La verificación debería fallar con la contraseña incorrecta", esValido)
    }

    @Test
    fun `verify falla con un salt incorrecto`() {
        val password = "password123"
        val (saltCorrecto, hashCorrecto) = PasswordUtils.hashPasswordWithSalt(password)

        // Genera un salt diferente
        val (saltIncorrecto, _) = PasswordUtils.hashPasswordWithSalt("otro_password")

        // Comprueba que la verificación falle (false)
        val esValido = PasswordUtils.verify(password, saltIncorrecto, hashCorrecto)
        assertFalse("La verificación debería fallar con un salt incorrecto", esValido)
    }
}