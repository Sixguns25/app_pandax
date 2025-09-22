package com.tesis.aplicacionpandax.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tesis.aplicacionpandax.data.entity.Specialist
import kotlinx.coroutines.flow.Flow

@Dao
interface SpecialistDao {
    @Insert
    suspend fun insert(specialist: Specialist)

    @Query("SELECT * FROM specialists WHERE userId = :userId")
    suspend fun getByUserId(userId: Long): Specialist?

    @Query("SELECT * FROM specialists")
    fun getAll(): Flow<List<Specialist>>

    @Query("SELECT * FROM specialists WHERE userId = :id LIMIT 1")
    fun getById(id: Long): Flow<Specialist?>
}
