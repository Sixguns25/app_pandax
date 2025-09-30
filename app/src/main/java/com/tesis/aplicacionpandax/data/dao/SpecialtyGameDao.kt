package com.tesis.aplicacionpandax.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tesis.aplicacionpandax.data.entity.Game
import com.tesis.aplicacionpandax.data.entity.SpecialtyGame
import kotlinx.coroutines.flow.Flow

@Dao
interface SpecialtyGameDao {
    @Insert
    suspend fun insert(specialtyGame: SpecialtyGame)

    @Query("""
        SELECT g.* FROM games g 
        INNER JOIN specialty_games sg ON g.id = sg.gameId 
        WHERE sg.specialtyId = :specialtyId
    """)
    fun getGamesForSpecialty(specialtyId: Long): Flow<List<Game>>
}