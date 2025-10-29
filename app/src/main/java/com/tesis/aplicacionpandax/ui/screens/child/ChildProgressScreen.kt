package com.tesis.aplicacionpandax.ui.screens.child

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Leaderboard // <-- IMPORTACIÓN DE ICONO AÑADIDA
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tesis.aplicacionpandax.data.entity.Child
import com.tesis.aplicacionpandax.data.entity.Game
import com.tesis.aplicacionpandax.data.entity.GameSession
import com.tesis.aplicacionpandax.repository.ProgressRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildProgressScreen(
    child: Child?,
    specialtyId: Long?, // Necesario para obtener juegos disponibles
    progressRepo: ProgressRepository
) {
    // Estados para datos y UI
    var sessions by remember { mutableStateOf<List<GameSession>>(emptyList()) }
    var availableGames by remember { mutableStateOf<List<Game>>(emptyList()) }
    var averageStars by remember { mutableStateOf(0.0f) }
    var selectedGameType by remember { mutableStateOf("Todos") } // Filtro por defecto
    var isLoadingGames by remember { mutableStateOf(true) } // Carga de juegos
    var isLoadingSessions by remember { mutableStateOf(true) } // Carga de sesiones

    // Obtener juegos disponibles para la especialidad
    LaunchedEffect(specialtyId) {
        isLoadingGames = true
        specialtyId?.let { id ->
            progressRepo.getGamesForSpecialty(id).collectLatest { games ->
                availableGames = games
                // Ajusta el filtro si el juego seleccionado ya no está disponible
                val gameTypes = listOf("Todos") + games.map { it.name }
                if (!gameTypes.contains(selectedGameType)) {
                    selectedGameType = "Todos"
                }
                isLoadingGames = false // Termina carga de juegos
            }
        } ?: run { isLoadingGames = false } // No hay specialtyId, termina carga
    }

    // Cargar sesiones filtradas por juegos disponibles y tipo seleccionado
    LaunchedEffect(child?.userId, selectedGameType, availableGames) {
        isLoadingSessions = true
        child?.userId?.let { childId ->
            val availableGameTypes = availableGames.map { it.name }

            // Define el Flow base según el filtro de juego
            val baseFlow = if (selectedGameType == "Todos") {
                progressRepo.getSessionsForChild(childId)
            } else if (availableGameTypes.contains(selectedGameType)) {
                progressRepo.getSessionsByChildAndType(childId, selectedGameType)
            } else {
                kotlinx.coroutines.flow.flowOf(emptyList()) // Si el tipo no es válido, flujo vacío
            }

            // Filtra adicionalmente por juegos disponibles (importante si "Todos" incluye juegos antiguos)
            baseFlow.map { sessionList ->
                sessionList.filter { availableGameTypes.contains(it.gameType) }
            }.collectLatest { filteredSessions ->
                sessions = filteredSessions // Ya están ordenadas por el DAO (DESC)
                averageStars = if (filteredSessions.isNotEmpty()) {
                    filteredSessions.map { it.stars }.average().toFloat()
                } else {
                    0.0f
                }
                Log.d("ChildProgressScreen", "Sesiones cargadas: ${sessions.size}, promedio=$averageStars")
                isLoadingSessions = false // Termina carga de sesiones
            }
        } ?: run {
            Log.e("ChildProgressScreen", "child o userId es nulo")
            isLoadingSessions = false // No hay ID, termina carga
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Progreso") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mensaje de error si faltan datos
            if (child == null || specialtyId == null) {
                Text( "😕 No se pudo cargar el progreso.", textAlign = TextAlign.Center )
                return@Scaffold
            }

            // --- Filtro por tipo de juego (Estilizado) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filtrar por juego:",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(end = 8.dp)
                )
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = availableGames.find { it.name == selectedGameType }?.displayName ?: "Todos",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        enabled = !isLoadingGames && !isLoadingSessions
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todos") },
                            onClick = { selectedGameType = "Todos"; expanded = false }
                        )
                        availableGames.forEach { game ->
                            DropdownMenuItem(
                                text = { Text(game.displayName) },
                                onClick = {
                                    selectedGameType = game.name
                                    expanded = false
                                }
                            )
                        }
                    }
                } // Fin ExposedDropdownMenuBox
            } // Fin Row Filtro

            // --- Resumen Mejorado ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text( "🏆 Tu Resumen", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer )
                    Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.3f))

                    if (isLoadingSessions) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimaryContainer)
                    } else {
                        InfoItemSimple(icon = Icons.Filled.Stars, text = "Estrellas promedio: ${"%.1f".format(averageStars)} ⭐")
                        InfoItemSimple(icon = Icons.Filled.VideogameAsset, text = "Juegos jugados: ${sessions.size} 🎲")
                        Text(
                            text = when {
                                averageStars >= 2.5 -> "¡Eres una superestrella! 🌟 ¡Sigue brillando!"
                                averageStars >= 1.5 -> "¡Buen trabajo! 😄 ¡Puedes ganar más estrellas!"
                                sessions.isEmpty() && !isLoadingGames -> "¡Juega para ganar tus primeras estrellas! 🚀"
                                else -> "¡Sigue practicando, estás mejorando! 💪"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // --- Lista de sesiones ---
            Text("Historial de Juegos", style = MaterialTheme.typography.titleMedium)

            Box(modifier = Modifier.weight(1f)) {
                when {
                    isLoadingSessions || isLoadingGames -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    sessions.isEmpty() -> {
                        Text(
                            "😊 ¡Aún no has jugado a ${if (selectedGameType=="Todos") "ningún juego" else availableGames.find{it.name==selectedGameType}?.displayName ?: "este juego"}! Ve a la pestaña 'Juegos' para empezar.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center).padding(horizontal=16.dp)
                        )
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            items(sessions, key = { it.sessionId }) { session ->
                                SessionItem(session, availableGames)
                            }
                        }
                    }
                }
            } // Fin Box lista
        } // Fin Column principal
    } // Fin Scaffold
}

// --- Composable SessionItem Mejorado ---
@Composable
fun SessionItem(session: GameSession, availableGames: List<Game>) {
    val gameDisplayName = availableGames.find { it.name == session.gameType }?.displayName ?: session.gameType

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(session.timestamp))
            Text(
                text = "🎮 $gameDisplayName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Divider(modifier = Modifier.padding(bottom = 4.dp))
            InfoItemSimple(icon = Icons.Filled.Event, text = "Fecha: $date")

            // --- 👇 AÑADIDO AQUÍ ---
            // Muestra el nivel solo si el juego es "MEMORY" (o añade más juegos si tienen niveles)
            if (session.gameType == "MEMORY" || session.gameType == "COORDINATION" || session.gameType == "EMOTIONS" || session.gameType == "PRONUNCIATION") {
                InfoItemSimple(icon = Icons.Filled.Leaderboard, text = "Nivel Jugado: ${session.level}")
            }
            // --- FIN DE LA ADICIÓN ---

            InfoItemSimple(icon = Icons.Filled.Star, text = "Estrellas: ${session.stars} ${"⭐".repeat(session.stars)}")
            InfoItemSimple(icon = Icons.Filled.Timer, text = "Tiempo: ${session.timeTaken / 1000} seg")
            InfoItemSimple(icon = Icons.Filled.Repeat, text = "Intentos: ${session.attempts}")
        }
    }
}

// Composable auxiliar simple para info con icono (sin etiqueta fija)
@Composable
private fun InfoItemSimple(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = LocalContentColor.current.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}