package com.tesis.aplicacionpandax.repository

import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.PasswordUtils
import com.tesis.aplicacionpandax.data.entity.*

class AuthRepository(private val db: AppDatabase) {

    private val userDao = db.userDao()
    private val specialistDao = db.specialistDao()
    private val childDao = db.childDao()

    // 🔹 Login
    suspend fun login(username: String, password: String): Result<User> {
        val user = userDao.getByUsername(username)
            ?: return Result.failure(Exception("Usuario no encontrado"))
        val ok = PasswordUtils.verify(password, user.salt, user.passwordHash)
        return if (ok) Result.success(user)
        else Result.failure(Exception("Contraseña incorrecta"))
    }

    // 🔹 Registro de especialista (solo admin puede llamar)
    suspend fun registerSpecialist(
        username: String,
        password: String,
        firstName: String,
        lastName: String,
        phone: String,
        email: String,
        specialty: String
    ): Result<Long> {
        if (userDao.getByUsername(username) != null) {
            return Result.failure(Exception("Usuario ya existe"))
        }
        val (salt, hash) = PasswordUtils.hashPasswordWithSalt(password)
        val user = User(username = username, passwordHash = hash, salt = salt, role = "SPECIALIST")
        val id = userDao.insert(user)
        specialistDao.insert(
            Specialist(userId = id, firstName, lastName, phone, email, specialty)
        )
        return Result.success(id)
    }

    // 🔹 Registro de niño (admin o especialista)
    suspend fun registerChild(
        username: String,
        password: String,
        firstName: String,
        lastName: String,
        dni: String,
        condition: String,
        sex: String,
        birthDateMillis: Long,
        guardianName: String,
        guardianPhone: String,
        specialistId: Long? = null
    ): Result<Long> {
        if (userDao.getByUsername(username) != null) {
            return Result.failure(Exception("Usuario ya existe"))
        }
        if (specialistId != null && specialistDao.getByUserId(specialistId) == null) {
            return Result.failure(Exception("Especialista no existe"))
        }
        val (salt, hash) = PasswordUtils.hashPasswordWithSalt(password)
        val user = User(username = username, passwordHash = hash, salt = salt, role = "CHILD")
        val id = userDao.insert(user)
        childDao.insert(
            Child(
                userId = id,
                firstName = firstName,
                lastName = lastName,
                dni = dni,
                condition = condition,
                sex = sex,
                birthDateMillis = birthDateMillis,
                guardianName = guardianName,
                guardianPhone = guardianPhone,
                specialistId = specialistId
            )
        )
        return Result.success(id)
    }
}
