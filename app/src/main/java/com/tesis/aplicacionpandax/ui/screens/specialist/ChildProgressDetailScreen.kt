package com.tesis.aplicacionpandax.ui.screens.specialist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tesis.aplicacionpandax.data.entity.GameSession
import com.tesis.aplicacionpandax.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow

@Composable
fun ChildProgressDetailScreen(
    childUserId: Long,
    progressRepo: ProgressRepository
) {
    val sessionsFlow: Flow<List<GameSession>> = progressRepo.getSessionsForChild(childUserId)
    val sessions by sessionsFlow.collectAsState(initial = emptyList())

    Column(Modifier.padding(16.dp)) {
        Text("Progreso del Niño (ID: $childUserId)")
        if (sessions.isEmpty()) {
            Text("No hay sesiones registradas.")
        } else {
            LazyColumn {
                items(sessions) { session ->
                    Text("Juego: ${session.gameType} | Score: ${session.score} | Tiempo: ${session.timeTaken / 1000}s | Intentos: ${session.attempts} | Fecha: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date(session.timestamp))}")
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}