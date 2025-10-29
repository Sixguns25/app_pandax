package com.tesis.aplicacionpandax.ui.screens.child

import androidx.compose.foundation.Image // Importar Image
import androidx.compose.foundation.layout.*
// Importar LazyVerticalGrid y related items
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
import androidx.compose.ui.draw.clip // Para redondear imagen
import androidx.compose.ui.layout.ContentScale // Para escalar imagen
import androidx.compose.ui.res.painterResource // Para cargar drawables
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tesis.aplicacionpandax.R // Asegúrate que R se importe correctamente
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
        else -> "Juegos" // Título más corto
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Elige un Juego") }, // Título más genérico y amigable
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
                .padding(horizontal = 16.dp, vertical = 8.dp) // Ajustar padding
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
                    // --- Cuadrícula de Juegos ---
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2), // 2 columnas
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp), // Espacio entre filas
                        horizontalArrangement = Arrangement.spacedBy(16.dp), // Espacio entre columnas
                        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp) // Espacio arriba/abajo
                    ) {
                        items(games, key = { it.id }) { game ->
                            GameGridItem(game = game) { // Llama al nuevo Composable
                                // Acción al hacer clic
                                val route = game.route.replace("{childUserId}", child.userId.toString())
                                navController.navigate(route)
                            }
                        }
                    } // Fin LazyVerticalGrid
                } // Fin else (hay juegos)
            } // Fin when
        } // Fin Box
    } // Fin Scaffold
}

// Composable para cada elemento (tarjeta) en la cuadrícula de juegos
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameGridItem(game: Game, onClick: () -> Unit) {
    // Mapea el nombre interno del juego a un recurso drawable
    val imageRes = when (game.name) {
        "MEMORY" -> R.drawable.memory_game_preview // Reemplaza con tus nombres de archivo
        "EMOTIONS" -> R.drawable.emotions_game_preview
        "COORDINATION" -> R.drawable.coordination_game_preview
        "PRONUNCIATION" -> R.drawable.pronunciation_game_preview
        else -> R.drawable.logo_pandax // Imagen por defecto o un placeholder
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .aspectRatio(0.8f) // Hacer la tarjeta un poco más alta que ancha
            .semantics { contentDescription = "Jugar a ${game.displayName}" }, // Accesibilidad
        shape = RoundedCornerShape(16.dp), // Esquinas redondeadas
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(), // Ocupa toda la tarjeta
            horizontalAlignment = Alignment.CenterHorizontally, // Centra contenido horizontalmente
            verticalArrangement = Arrangement.Top // Alinea contenido arriba
        ) {
            // Imagen del Juego (ocupando la mayor parte)
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = null, // Descriptivo en la semántica de la tarjeta
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Ocupa el espacio vertical disponible
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)), // Redondea solo arriba si quieres
                contentScale = ContentScale.Crop // Recorta la imagen para llenar el espacio
            )

            // Nombre del Juego (debajo de la imagen)
            Text(
                text = game.displayName,
                style = MaterialTheme.typography.titleMedium, // Tamaño adecuado
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp) // Padding para el texto
            )
        }
    }
}