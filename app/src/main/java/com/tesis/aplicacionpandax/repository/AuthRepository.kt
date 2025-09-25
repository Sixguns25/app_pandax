package com.tesis.aplicacionpandax.repository

import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.PasswordUtils
import com.tesis.aplicacionpandax.data.entity.*

class AuthRepository(private val db: AppDatabase) {

    private val userDao = db.userDao()
    private val specialistDao = db.specialistDao()
    private val childDao = db.childDao()
    private val specialtyDao = db.specialtyDao()

    // 🔹 Login
    suspend fun login(username: String, password: String): Result<User> {
        val user = userDao.getByUsername(username)
            ?: return Result.failure(Exception("Usuario no encontrado"))
        val ok = PasswordUtils.verify(password, user.salt, user.passwordHash)
        return if (ok) Result.success(user)
        else Result.failure(Exception("Contraseña incorrecta"))
    }

    // 🔹 Registro de especialista (Create)
    suspend fun registerSpecialist(
        username: String,
        password: String,
        firstName: String,
        lastName: String,
        phone: String,
        email: String,
        specialtyId: Long
    ): Result<Long> {
        if (userDao.getByUsername(username) != null) {
            return Result.failure(Exception("Usuario ya existe"))
        }
        if (specialtyDao.getById(specialtyId) == null) {
            return Result.failure(Exception("Especialidad no existe"))
        }
        val (salt, hash) = PasswordUtils.hashPasswordWithSalt(password)
        val user = User(username = username, passwordHash = hash, salt = salt, role = "SPECIALIST")
        val id = userDao.insert(user)
        specialistDao.insert(
            Specialist(userId = id, firstName, lastName, phone, email, specialtyId)
        )
        return Result.success(id)
    }

    // 🔹 Actualizar especialista (Update)
    suspend fun updateSpecialist(
        specialistId: Long,
        firstName: String,
        lastName: String,
        phone: String,
        email: String,
        specialtyId: Long,
        password: String? = null
    ): Result<Unit> {
        val specialist = specialistDao.getByUserId(specialistId)
            ?: return Result.failure(Exception("Especialista no encontrado"))
        if (specialtyDao.getById(specialtyId) == null) {
            return Result.failure(Exception("Especialidad no existe"))
        }
        val updatedSpecialist = specialist.copy(
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            phone = phone.trim(),
            email = email.trim(),
            specialtyId = specialtyId
        )
        specialistDao.update(updatedSpecialist)

        if (password != null && password.isNotBlank()) {
            val user = userDao.getById(specialistId)
                ?: return Result.failure(Exception("Usuario no encontrado"))
            val (salt, hash) = PasswordUtils.hashPasswordWithSalt(password)
            val updatedUser = user.copy(passwordHash = hash, salt = salt)
            userDao.update(updatedUser)
        }
        return Result.success(Unit)
    }

    // 🔹 Eliminar especialista (Delete)
    suspend fun deleteSpecialist(specialistId: Long): Result<Unit> {
        val assignedChildren = childDao.getBySpecialist(specialistId)
        if (assignedChildren.isNotEmpty()) {
            return Result.failure(Exception("No se puede eliminar, hay niños asignados"))
        }
        val specialist = specialistDao.getByUserId(specialistId)
            ?: return Result.failure(Exception("Especialista no encontrado"))
        val user = userDao.getById(specialistId)
            ?: return Result.failure(Exception("Usuario no encontrado"))
        specialistDao.delete(specialist)
        userDao.delete(user)
        return Result.success(Unit)
    }

    // 🔹 Registro de niño
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