package com.tesis.aplicacionpandax.ui.screens.child

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tesis.aplicacionpandax.data.entity.Child
import com.tesis.aplicacionpandax.data.entity.GameSession
import com.tesis.aplicacionpandax.repository.ProgressRepository
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildProgressScreen(
    child: Child?,
    progressRepo: ProgressRepository
) {
    var sessions by remember { mutableStateOf<List<GameSession>>(emptyList()) }
    var averageStars by remember { mutableStateOf(0.0f) }
    var selectedGameType by remember { mutableStateOf("Todos") }
    val gameTypes = listOf("Todos", "MEMORY", "EMOTIONS") // Incluye ambos juegos

    // Cargar sesiones en tiempo real
    LaunchedEffect(child?.userId, selectedGameType) {
        child?.userId?.let { childId ->
            val flow = if (selectedGameType == "Todos") {
                progressRepo.getSessionsForChild(childId)
            } else {
                progressRepo.getSessionsForChildByType(childId, selectedGameType)
            }
            flow.collectLatest { sessionList ->
                sessions = sessionList
                averageStars = if (sessionList.isNotEmpty()) {
                    sessionList.map { it.score }.average().toFloat()
                } else {
                    0.0f
                }
                Log.d("ChildProgressScreen", "Sesiones cargadas: ${sessions.size}, promedio=$averageStars")
            }
        } ?: Log.e("ChildProgressScreen", "child o userId es nulo")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (child == null) {
            Text(
                text = "😕 No se encontró información del niño.",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            return@Column
        }

        Text(
            text = "🌟 ¡Tu Progreso, ${child.firstName}! 🌟",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Filtro por tipo de juego
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🎮 Filtrar por juego:",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = selectedGameType,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor()
                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    gameTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                selectedGameType = type
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Resumen
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🏆 Tu Resumen",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Estrellas promedio: ${"%.1f".format(averageStars)} ⭐",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Juegos jugados: ${sessions.size} 🎲",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when {
                        averageStars >= 2.5 -> "¡Eres una superestrella! 🌟 ¡Sigue brillando!"
                        averageStars >= 1.5 -> "¡Buen trabajo! 😄 ¡Puedes ganar más estrellas!"
                        sessions.isEmpty() -> "¡Juega para ganar tus primeras estrellas! 🚀"
                        else -> "¡Sigue practicando, estás mejorando! 💪"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lista de sesiones
        if (sessions.isEmpty()) {
            Text(
                text = "😊 ¡No hay juegos jugados aún! Juega para ver tu progreso aquí.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        } else {
            LazyColumn {
                items(sessions) { session ->
                    SessionItem(session)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun SessionItem(session: GameSession) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(session.timestamp))
            Text(
                text = "🎮 Juego: ${session.gameType}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "📅 Fecha: $date",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "⭐ Estrellas: ${session.score} ${"⭐".repeat(session.score)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "⏱️ Tiempo: ${session.timeTaken / 1000} segundos",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "🔄 Intentos: ${session.attempts}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}