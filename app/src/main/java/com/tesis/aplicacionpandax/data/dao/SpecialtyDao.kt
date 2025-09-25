package com.tesis.aplicacionpandax.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.tesis.aplicacionpandax.data.entity.Specialty
import kotlinx.coroutines.flow.Flow

@Dao
interface SpecialtyDao {
    @Insert
    suspend fun insert(specialty: Specialty)

    @Update
    suspend fun update(specialty: Specialty)

    @Delete
    suspend fun delete(specialty: Specialty)

    @Query("SELECT * FROM specialties")
    fun getAll(): Flow<List<Specialty>>

    @Query("SELECT * FROM specialties WHERE id = :id")
    suspend fun getById(id: Long): Specialty?
}