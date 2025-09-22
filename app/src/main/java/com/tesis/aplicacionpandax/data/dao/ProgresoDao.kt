package com.tesis.aplicacionpandax.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tesis.aplicacionpandax.data.entity.Progreso
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgresoDao {
    @Insert
    suspend fun insert(progreso: Progreso): Long

    @Query("SELECT * FROM progreso WHERE childId = :childId ORDER BY date ASC")
    fun getByChild(childId: Long): Flow<List<Progreso>>

    @Query("SELECT * FROM progreso WHERE childId = :childId AND game = :game ORDER BY date ASC")
    fun getByChildAndGame(childId: Long, game: String): Flow<List<Progreso>>
}
