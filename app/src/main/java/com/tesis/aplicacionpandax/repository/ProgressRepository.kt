package com.tesis.aplicacionpandax.repository

import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.entity.GameSession
import kotlinx.coroutines.flow.Flow

class ProgressRepository(private val db: AppDatabase) {
    private val dao = db.gameSessionDao()

    suspend fun saveSession(session: GameSession) {
        dao.insert(session)
    }

    fun getSessionsForChild(childUserId: Long): Flow<List<GameSession>> {
        return dao.getSessionsByChild(childUserId)
    }

    fun getSessionsForChildByType(childUserId: Long, gameType: String): Flow<List<GameSession>> {
        return dao.getSessionsByChildAndType(childUserId, gameType)
    }
}