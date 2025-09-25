package com.tesis.aplicacionpandax.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.tesis.aplicacionpandax.data.entity.Specialist
import kotlinx.coroutines.flow.Flow

@Dao
interface SpecialistDao {
    @Insert
    suspend fun insert(specialist: Specialist)

    @Update
    suspend fun update(specialist: Specialist)  // Nuevo para Update

    @Delete
    suspend fun delete(specialist: Specialist)  // Nuevo para Delete

    @Query("SELECT * FROM specialists WHERE userId = :userId")
    suspend fun getByUserId(userId: Long): Specialist?

    @Query("SELECT * FROM specialists")
    fun getAll(): Flow<List<Specialist>>

    @Query("SELECT * FROM specialists WHERE userId = :id LIMIT 1")
    fun getById(id: Long): Flow<Specialist?>

    @Query("SELECT * FROM specialists WHERE specialtyId = :specialtyId")
    suspend fun getSpecialistsBySpecialtyId(specialtyId: Long): List<Specialist>
}
