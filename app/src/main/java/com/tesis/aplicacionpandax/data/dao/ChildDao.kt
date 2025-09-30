package com.tesis.aplicacionpandax.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.tesis.aplicacionpandax.data.entity.Child
import kotlinx.coroutines.flow.Flow

@Dao
interface ChildDao {
    @Insert
    suspend fun insert(child: Child)

    @Update
    suspend fun update(child: Child)

    @Delete
    suspend fun delete(child: Child)

    @Query("SELECT * FROM children WHERE userId = :userId")
    suspend fun getByUserId(userId: Long): Child?

    @Query("SELECT * FROM children WHERE specialistId = :specialistId")
    suspend fun getBySpecialist(specialistId: Long): List<Child> // Cambiado de Flow a List

    @Query("SELECT * FROM children")
    fun getAll(): Flow<List<Child>>

    @Query("SELECT * FROM children WHERE specialistId = :specialistId")
    fun getChildrenForSpecialist(specialistId: Long): Flow<List<Child>>

    @Query("SELECT * FROM children WHERE userId = :userId LIMIT 1")
    fun getChildByUserId(userId: Long): Flow<Child?>

    // Nueva consulta para obtener niños con nombres de especialistas
    @Query("SELECT c.*, s.firstName AS specialistFirstName, s.lastName AS specialistLastName FROM children c LEFT JOIN specialists s ON c.specialistId = s.userId")
    fun getAllWithSpecialist(): Flow<List<ChildWithSpecialist>>
}

// Data class para representar un niño con los datos del especialista
data class ChildWithSpecialist(
    @Embedded val child: Child,
    val specialistFirstName: String?,
    val specialistLastName: String?
) {
    val specialistName: String
        get() = if (specialistFirstName != null && specialistLastName != null) {
            "$specialistFirstName $specialistLastName"
        } else {
            "Sin especialista"
        }
}