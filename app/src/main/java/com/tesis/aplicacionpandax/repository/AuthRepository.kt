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

    // 🔹 Registro de niño (Create)
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

    // 🔹 Actualizar niño (Update)
    suspend fun updateChild(
        childId: Long,
        firstName: String,
        lastName: String,
        dni: String,
        condition: String,
        sex: String,
        birthDateMillis: Long,
        guardianName: String,
        guardianPhone: String,
        specialistId: Long?,
        password: String? = null
    ): Result<Unit> {
        val child = childDao.getByUserId(childId)
            ?: return Result.failure(Exception("Niño no encontrado"))
        if (specialistId != null && specialistDao.getByUserId(specialistId) == null) {
            return Result.failure(Exception("Especialista no existe"))
        }
        val updatedChild = child.copy(
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            dni = dni.trim(),
            condition = condition.trim(),
            sex = sex.trim(),
            birthDateMillis = birthDateMillis,
            guardianName = guardianName.trim(),
            guardianPhone = guardianPhone.trim(),
            specialistId = specialistId
        )
        childDao.update(updatedChild)

        if (password != null && password.isNotBlank()) {
            val user = userDao.getById(childId)
                ?: return Result.failure(Exception("Usuario no encontrado"))
            val (salt, hash) = PasswordUtils.hashPasswordWithSalt(password)
            val updatedUser = user.copy(passwordHash = hash, salt = salt)
            userDao.update(updatedUser)
        }
        return Result.success(Unit)
    }

    // 🔹 Eliminar niño (Delete)
    suspend fun deleteChild(childId: Long): Result<Unit> {
        val sessions = db.gameSessionDao().getSessionsByChildSuspend(childId)
        if (sessions.isNotEmpty()) {
            return Result.failure(Exception("No se puede eliminar, hay sesiones de juego asociadas"))
        }
        val child = childDao.getByUserId(childId)
            ?: return Result.failure(Exception("Niño no encontrado"))
        val user = userDao.getById(childId)
            ?: return Result.failure(Exception("Usuario no encontrado"))
        childDao.delete(child)
        userDao.delete(user)
        return Result.success(Unit)
    }

    // Nueva función para actualizar solo contacto
    suspend fun updateSpecialistContact(userId: Long, phone: String, email: String): Result<Unit> {
        val specialist = specialistDao.getByUserId(userId)
            ?: return Result.failure(Exception("Especialista no encontrado"))

        // Añade validaciones básicas si quieres (ej. formato de email)
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return Result.failure(Exception("Formato de correo inválido"))
        }
        if (!phone.matches(Regex("\\d{9,15}"))) { // Misma validación que en RegisterChild
            return Result.failure(Exception("Teléfono debe tener entre 9 y 15 dígitos"))
        }

        val updatedSpecialist = specialist.copy(
            phone = phone.trim(),
            email = email.trim()
        )
        return try {
            specialistDao.update(updatedSpecialist)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Error al actualizar: ${e.message}"))
        }
    }

    // Nueva función para cambiar contraseña
    suspend fun changePassword(userId: Long, currentPassword: String, newPassword: String): Result<Unit> {
        val user = userDao.getById(userId)
            ?: return Result.failure(Exception("Usuario no encontrado"))

        // Verificar contraseña actual
        val isCurrentPasswordValid = PasswordUtils.verify(currentPassword, user.salt, user.passwordHash)
        if (!isCurrentPasswordValid) {
            return Result.failure(Exception("La contraseña actual es incorrecta"))
        }

        // Validar nueva contraseña (ej. longitud mínima)
        if (newPassword.length < 6) {
            return Result.failure(Exception("La nueva contraseña debe tener al menos 6 caracteres"))
        }

        // Hashear y actualizar
        val (newSalt, newHash) = PasswordUtils.hashPasswordWithSalt(newPassword)
        val updatedUser = user.copy(passwordHash = newHash, salt = newSalt)

        return try {
            userDao.update(updatedUser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Error al actualizar la contraseña: ${e.message}"))
        }
    }
}