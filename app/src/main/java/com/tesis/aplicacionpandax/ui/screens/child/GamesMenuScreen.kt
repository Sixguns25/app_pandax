package com.tesis.aplicacionpandax.ui.screens.child

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tesis.aplicacionpandax.R
import com.tesis.aplicacionpandax.data.entity.Child
import com.tesis.aplicacionpandax.data.entity.Game
import com.tesis.aplicacionpandax.repository.ProgressRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesMenuScreen(
    child: Child?,
    specialtyId: Long?,
    navController: NavController,
    progressRepo: ProgressRepository,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }
    val gamesFlow = remember(specialtyId) {
        progressRepo.getGamesForSpecialty(specialtyId ?: -1L)
    }
    val games by gamesFlow.collectAsState(initial = emptyList())

    LaunchedEffect(games) { if (isLoading) { isLoading = false } }

    val specialtyName = when (specialtyId) {
        1L -> "Conducta"
        2L -> "Fonoaudiología"
        3L -> "Lenguaje"
        4L -> "Educación"
        5L -> "Terapia de Motricidad"
        else -> "Juegos"
    }

    var gameRequiringLevel by remember { mutableStateOf<Game?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Elige un Juego") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                child == null || specialtyId == null -> {
                    Text( "No se pudo cargar la lista de juegos.", textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center) )
                }
                games.isEmpty() -> {
                    Text( "No hay juegos asignados para $specialtyName.", textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center) )
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                    ) {
                        items(games, key = { it.id }) { game ->
                            GameGridItem(game = game) {
                                if (child != null) {
                                    // --- 👇 LÓGICA DE CLIC ACTUALIZADA 👇 ---
                                    // Comprueba si el juego requiere selección de nivel
                                    if (game.name == "MEMORY" || game.name == "COORDINATION" || game.name == "EMOTIONS" || game.name == "PRONUNCIATION") {
                                        gameRequiringLevel = game // Muestra el diálogo de nivel
                                    } else {
                                        // Para otros juegos (Pronunciation), navega directamente
                                        val route = game.route.replace("{childUserId}", child.userId.toString())
                                        navController.navigate(route)
                                    }
                                }
                            }
                        }
                    } // Fin LazyVerticalGrid
                } // Fin else
            } // Fin when

            // --- Diálogo Genérico (Sin cambios, ya maneja gameRequiringLevel) ---
            if (gameRequiringLevel != null && child != null) {
                LevelSelectionDialog(
                    gameName = gameRequiringLevel!!.displayName,
                    gameType = gameRequiringLevel!!.name, // Pasa el tipo
                    onDismiss = { gameRequiringLevel = null },
                    onLevelSelected = { level ->
                        // Construye la ruta base (ej. "memory_game", "emotions_game")
                        val routeBase = gameRequiringLevel!!.name.lowercase() + "_game"
                        val finalRoute = "$routeBase/${child.userId}/$level"

                        Log.d("GamesMenuScreen", "Navegando a: $finalRoute")
                        navController.navigate(finalRoute)
                        gameRequiringLevel = null // Cierra el diálogo
                    }
                )
            }
        } // Fin Box
    } // Fin Scaffold
}

// Composable GameGridItem (Sin Cambios)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameGridItem(game: Game, onClick: () -> Unit) {
    val imageRes = when (game.name) {
        "MEMORY" -> R.drawable.memory_game_preview
        "EMOTIONS" -> R.drawable.emotions_game_preview
        "COORDINATION" -> R.drawable.coordination_game_preview
        "PRONUNCIATION" -> R.drawable.pronunciation_game_preview
        else -> R.drawable.logo_pandax
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .aspectRatio(0.8f)
            .semantics { contentDescription = "Jugar a ${game.displayName}" },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = game.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            )
        }
    }
}

// --- 👇 DIÁLOGO DE NIVEL ACTUALIZADO 👇 ---
@Composable
private fun LevelSelectionDialog(
    gameName: String,
    gameType: String, // "MEMORY", "COORDINATION", o "EMOTIONS" y PRONUNCIATION
    onDismiss: () -> Unit,
    onLevelSelected: (Int) -> Unit
) {
    // Define los textos de los niveles basados en el tipo de juego
    val (level1Text, level2Text, level3Text) = when(gameType) {
        "MEMORY" -> Triple("Nivel 1 (Fácil: 2x2)", "Nivel 2 (Normal: 2x4)", "Nivel 3 (Difícil: 3x4)")
        "COORDINATION" -> Triple("Nivel 1 (Fácil)", "Nivel 2 (Normal)", "Nivel 3 (Difícil)")
        "EMOTIONS" -> Triple("Nivel 1 (Fácil: 2 Opciones)", "Nivel 2 (Normal: 4 Opciones)", "Nivel 3 (Difícil: 6 Emociones)")
        "PRONUNCIATION" -> Triple("Nivel 1 (Fácil: Monosílabas)", "Nivel 2 (Normal: Bisílabas)", "Nivel 3 (Difícil: Polisílabas)")
        else -> Triple("Nivel 1", "Nivel 2", "Nivel 3") // Default
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Elige un Nivel") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("¿Qué nivel de '$gameName' quieres jugar?", textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { onLevelSelected(1) }, modifier = Modifier.fillMaxWidth()) {
                    Text(level1Text)
                }
                Button(onClick = { onLevelSelected(2) }, modifier = Modifier.fillMaxWidth()) {
                    Text(level2Text)
                }
                Button(onClick = { onLevelSelected(3) }, modifier = Modifier.fillMaxWidth()) {
                    Text(level3Text)
                }
            }
        },
        confirmButton = { /* Vacío */ },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}