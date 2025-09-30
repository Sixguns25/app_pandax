package com.tesis.aplicacionpandax.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tesis.aplicacionpandax.data.entity.Game
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Insert
    suspend fun insert(game: Game): Long

    @Query("SELECT * FROM games")
    fun getAll(): Flow<List<Game>>
}