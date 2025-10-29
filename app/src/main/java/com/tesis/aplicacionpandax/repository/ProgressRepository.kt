package com.tesis.aplicacionpandax.repository

import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.entity.Game
import com.tesis.aplicacionpandax.data.entity.GameSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ProgressSummary(
    val sessionCount: Int,
    val averageStars: Float,
    val averageTimeTaken: Float,
    val averageAttempts: Float
)

class ProgressRepository(private val db: AppDatabase) {
    private val dao = db.gameSessionDao()
    private val specialtyGameDao = db.specialtyGameDao()

    suspend fun saveSession(session: GameSession) {
        dao.insert(session)
    }

    fun getSessionsForChild(childUserId: Long): Flow<List<GameSession>> {
        return dao.getSessionsByChild(childUserId)
    }

    fun getSessionsByChildAndType(childUserId: Long, gameType: String): Flow<List<GameSession>> {
        return dao.getSessionsByChildAndType(childUserId, gameType)
    }

    fun getSessionsByDateRange(childUserId: Long, startTime: Long, endTime: Long): Flow<List<GameSession>> {
        return dao.getSessionsByChildAndDateRange(childUserId, startTime, endTime)
    }

    fun getProgressSummary(childUserId: Long): Flow<ProgressSummary> {
        return getSessionsForChild(childUserId).map { sessions ->
            ProgressSummary(
                sessionCount = sessions.size,
                averageStars = if (sessions.isNotEmpty()) sessions.map { it.stars }.average().toFloat() else 0f,
                averageTimeTaken = if (sessions.isNotEmpty()) sessions.map { it.timeTaken / 1000f }.average().toFloat() else 0f,
                averageAttempts = if (sessions.isNotEmpty()) sessions.map { it.attempts.toFloat() }.average().toFloat() else 0f
            )
        }
    }

    fun getProgressSummaryByDateRange(childUserId: Long, startTime: Long, endTime: Long): Flow<ProgressSummary> {
        return getSessionsByDateRange(childUserId, startTime, endTime).map { sessions ->
            ProgressSummary(
                sessionCount = sessions.size,
                averageStars = if (sessions.isNotEmpty()) sessions.map { it.stars }.average().toFloat() else 0f,
                averageTimeTaken = if (sessions.isNotEmpty()) sessions.map { it.timeTaken / 1000f }.average().toFloat() else 0f,
                averageAttempts = if (sessions.isNotEmpty()) sessions.map { it.attempts.toFloat() }.average().toFloat() else 0f
            )
        }
    }

    fun getGamesForSpecialty(specialtyId: Long): Flow<List<Game>> {
        return specialtyGameDao.getGamesForSpecialty(specialtyId)
    }
}