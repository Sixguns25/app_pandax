package com.tesis.aplicacionpandax.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tesis.aplicacionpandax.data.entity.GameSession
import kotlinx.coroutines.flow.Flow

@Dao
interface GameSessionDao {
    @Insert
    suspend fun insert(session: GameSession)

    @Query("SELECT * FROM game_sessions WHERE childUserId = :childUserId ORDER BY timestamp DESC")
    fun getSessionsByChild(childUserId: Long): Flow<List<GameSession>>

    @Query("SELECT * FROM game_sessions WHERE childUserId = :childUserId AND gameType = :gameType ORDER BY timestamp DESC")
    fun getSessionsByChildAndType(childUserId: Long, gameType: String): Flow<List<GameSession>>
}