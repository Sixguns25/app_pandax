package com.tesis.aplicacionpandax.repository

import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.entity.GameSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ProgressSummary(
    val sessionCount: Int,
    val averageScore: Float,
    val averageTimeTaken: Float,
    val averageAttempts: Float
)

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

    // Nuevo: Obtener sesiones por rango de fechas
    fun getSessionsByDateRange(childUserId: Long, startTime: Long, endTime: Long): Flow<List<GameSession>> {
        return dao.getSessionsByChildAndDateRange(childUserId, startTime, endTime)
    }

    // Nuevo: Obtener resumen de progreso
    fun getProgressSummary(childUserId: Long): Flow<ProgressSummary> {
        return getSessionsForChild(childUserId).map { sessions ->
            ProgressSummary(
                sessionCount = sessions.size,
                averageScore = if (sessions.isNotEmpty()) sessions.map { it.score }.average().toFloat() else 0f,
                averageTimeTaken = if (sessions.isNotEmpty()) sessions.map { it.timeTaken / 1000f }.average().toFloat() else 0f,
                averageAttempts = if (sessions.isNotEmpty()) sessions.map { it.attempts.toFloat() }.average().toFloat() else 0f
            )
        }
    }

    // Nuevo: Resumen por rango de fechas
    fun getProgressSummaryByDateRange(childUserId: Long, startTime: Long, endTime: Long): Flow<ProgressSummary> {
        return getSessionsByDateRange(childUserId, startTime, endTime).map { sessions ->
            ProgressSummary(
                sessionCount = sessions.size,
                averageScore = if (sessions.isNotEmpty()) sessions.map { it.score }.average().toFloat() else 0f,
                averageTimeTaken = if (sessions.isNotEmpty()) sessions.map { it.timeTaken / 1000f }.average().toFloat() else 0f,
                averageAttempts = if (sessions.isNotEmpty()) sessions.map { it.attempts.toFloat() }.average().toFloat() else 0f
            )
        }
    }
}