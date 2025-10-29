package com.tesis.aplicacionpandax.ui.screens.specialist // O common si la usa Admin también

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape // Para formas
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons // Para iconos
import androidx.compose.material.icons.filled.* // Importar iconos necesarios
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector // Para iconos
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign // Para centrar texto
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController // Necesario para botón de volver
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.entity.Game // Importar Game
import com.tesis.aplicacionpandax.data.entity.GameSession
import com.tesis.aplicacionpandax.repository.ProgressRepository
import com.tesis.aplicacionpandax.repository.ProgressSummary
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map // Para filtrar sesiones
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildProgressDetailScreen(
    childUserId: Long,
    progressRepo: ProgressRepository,
    db: AppDatabase, // Necesario para obtener nombre del niño y juegos
    navController: NavController, // Necesario para botón de volver
    specialtyId: Long? // Necesario para obtener juegos disponibles
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // --- Estados ---
    var childName by remember { mutableStateOf("Niño") }
    var isLoadingChildName by remember { mutableStateOf(true) }
    var isLoadingGames by remember { mutableStateOf(true) }
    var isLoadingSessions by remember { mutableStateOf(true) }

    var selectedDateFilter by remember { mutableStateOf("Todas") }
    val dateFilterOptions = listOf("Hoy", "Últimos 7 días", "Últimos 30 días", "Todas")
    var expandedDate by remember { mutableStateOf(false) }

    var availableGames by remember { mutableStateOf<List<Game>>(emptyList()) }
    var selectedGameType by remember { mutableStateOf("Todos") } // Nombre interno del juego o "Todos"
    var expandedGameType by remember { mutableStateOf(false) }

    var sessions by remember { mutableStateOf<List<GameSession>>(emptyList()) }
    var summary by remember { mutableStateOf<ProgressSummary?>(null) }

    // --- Carga de Datos ---
    // Cargar nombre del niño
    LaunchedEffect(childUserId) {
        isLoadingChildName = true
        db.childDao().getChildByUserId(childUserId).collectLatest { child ->
            childName = child?.firstName ?: "Niño Desconocido"
            isLoadingChildName = false
        }
    }

    // Cargar juegos disponibles
    LaunchedEffect(specialtyId) {
        isLoadingGames = true
        specialtyId?.let { id ->
            progressRepo.getGamesForSpecialty(id).collectLatest { games ->
                availableGames = games
                val gameNames = listOf("Todos") + games.map { it.name }
                if (!gameNames.contains(selectedGameType)) {
                    selectedGameType = "Todos" // Resetea si el filtro actual no es válido
                }
                isLoadingGames = false
            }
        } ?: run { isLoadingGames = false } // No hay especialidad, no hay juegos que cargar
    }

    // Calcular rangos de fechas (dentro de LaunchedEffect o remember con selectedDateFilter)
    val (startTime, endTime) = remember(selectedDateFilter) {
        val cal = Calendar.getInstance()
        val currentEndTime = System.currentTimeMillis() // Siempre hasta ahora
        val calculatedStartTime = when (selectedDateFilter) {
            "Hoy" -> cal.apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            "Últimos 7 días" -> cal.apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
            "Últimos 30 días" -> cal.apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis
            else -> 0L // Todas
        }
        Pair(calculatedStartTime, currentEndTime)
    }

    // Cargar sesiones filtradas
    LaunchedEffect(selectedDateFilter, selectedGameType, childUserId, availableGames) {
        if (childUserId == -1L) { // No cargar si el ID no es válido
            isLoadingSessions = false
            return@LaunchedEffect
        }
        isLoadingSessions = true
        val availableGameNames = availableGames.map { it.name }

        // Flujo base según fecha
        val baseFlow = if (selectedDateFilter != "Todas") {
            progressRepo.getSessionsByDateRange(childUserId, startTime, endTime)
        } else {
            progressRepo.getSessionsForChild(childUserId)
        }

        // Mapear y filtrar por tipo de juego y juegos disponibles
        baseFlow.map { sessionList ->
            sessionList.filter { s ->
                availableGameNames.contains(s.gameType) && // Solo juegos de la especialidad
                        (selectedGameType == "Todos" || s.gameType == selectedGameType) // Filtro por tipo
            }
        }.collectLatest { filteredSessions ->
            sessions = filteredSessions.sortedByDescending { it.timestamp } // Ordenar por más reciente
            summary = ProgressSummary(
                sessionCount = sessions.size,
                averageStars = if (sessions.isNotEmpty()) sessions.map { it.stars }.average().toFloat() else 0f,
                averageTimeTaken = if (sessions.isNotEmpty()) sessions.map { it.timeTaken / 1000f }.average().toFloat() else 0f,
                averageAttempts = if (sessions.isNotEmpty()) sessions.map { it.attempts.toFloat() }.average().toFloat() else 0f
            )
            isLoadingSessions = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progreso de $childName") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        // Indicador de carga general inicial
        if (isLoadingChildName || isLoadingGames) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Contenido principal con scroll
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- Filtros ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), // Espacio entre filtros
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Filtro por Fechas
                    ExposedDropdownMenuBox( expanded = expandedDate, onExpandedChange = { expandedDate = !expandedDate }, modifier = Modifier.weight(1f) ) {
                        OutlinedTextField(
                            value = selectedDateFilter, onValueChange = {}, readOnly = true, label = { Text("Periodo") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDate) },
                            shape = RoundedCornerShape(12.dp), colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            enabled = !isLoadingSessions // Deshabilita mientras cargan sesiones
                        )
                        ExposedDropdownMenu( expanded = expandedDate, onDismissRequest = { expandedDate = false } ) {
                            dateFilterOptions.forEach { option -> DropdownMenuItem( text = { Text(option) }, onClick = { selectedDateFilter = option; expandedDate = false } ) }
                        }
                    }
                    // Filtro por Juego
                    ExposedDropdownMenuBox( expanded = expandedGameType, onExpandedChange = { expandedGameType = !expandedGameType }, modifier = Modifier.weight(1f) ) {
                        OutlinedTextField(
                            value = availableGames.find { it.name == selectedGameType }?.displayName ?: "Todos", // Muestra displayName
                            onValueChange = {}, readOnly = true, label = { Text("Juego") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGameType) },
                            shape = RoundedCornerShape(12.dp), colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            enabled = !isLoadingSessions // Deshabilita mientras cargan sesiones
                        )
                        ExposedDropdownMenu( expanded = expandedGameType, onDismissRequest = { expandedGameType = false } ) {
                            DropdownMenuItem( text = { Text("Todos") }, onClick = { selectedGameType = "Todos"; expandedGameType = false } )
                            availableGames.forEach { game -> DropdownMenuItem( text = { Text(game.displayName) }, onClick = { selectedGameType = game.name; expandedGameType = false } ) }
                        }
                    }
                } // Fin Row Filtros

                // --- Resumen de progreso ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally, // Centra contenido
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text( "📊 Resumen", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer )
                        Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.3f))

                        if (isLoadingSessions) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(vertical = 16.dp))
                        } else if (summary != null && summary!!.sessionCount > 0) {
                            // Usamos InfoItemSimple del ChildProgressScreen (podría moverse a components)
                            InfoItemSimple(icon = Icons.Filled.VideogameAsset, text = "Sesiones jugadas: ${summary!!.sessionCount}")
                            InfoItemSimple(icon = Icons.Filled.Stars, text = "Estrellas promedio: ${"%.1f".format(summary!!.averageStars)} ⭐")
                            InfoItemSimple(icon = Icons.Filled.Timer, text = "Tiempo promedio: ${"%.1f".format(summary!!.averageTimeTaken)} seg")
                            InfoItemSimple(icon = Icons.Filled.Repeat, text = "Intentos promedio: ${"%.1f".format(summary!!.averageAttempts)}")
                            Spacer(modifier = Modifier.height(4.dp))
                            Text( // Mensaje basado en promedio
                                text = when {
                                    summary!!.averageStars >= 2.5 -> "¡Progreso excepcional! Desempeño destacado."
                                    summary!!.averageStars >= 1.5 -> "Buen progreso, sigue practicando."
                                    else -> "Progreso inicial, ¡ánimo!"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text( "No hay datos para este resumen.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer )
                        }
                    }
                } // Fin Card Resumen

                // --- Gráfico de líneas ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text( "📈 Progreso (Estrellas)", style = MaterialTheme.typography.titleLarge )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isLoadingSessions) {
                            Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center){ CircularProgressIndicator() }
                        } else if (sessions.isEmpty()) {
                            Text( "No hay datos para mostrar en el gráfico.", style = MaterialTheme.typography.bodyMedium )
                        } else {
                            // --- Canvas del Gráfico (sin cambios en la lógica interna) ---
                            val maxScore = 3f // Máximo de estrellas
                            Canvas( modifier = Modifier.fillMaxWidth().height(250.dp).background(Color.White) ) {
                                // ... (Toda la lógica de dibujo del Canvas como estaba antes) ...
                                val width = size.width; val height = size.height; val padding = 40.dp.toPx()
                                val graphWidth = width - 2 * padding; val graphHeight = height - 2 * padding
                                // Cuadrícula
                                for (i in 0..3) { val y = (height - padding) - (i / 3f) * graphHeight; drawLine( Color.LightGray.copy(alpha = 0.3f), Offset(padding, y), Offset(width - padding, y), 1f ) }
                                // Ejes
                                drawLine( Color.Gray, Offset(padding, height - padding), Offset(padding, padding), 2f )
                                drawLine( Color.Gray, Offset(padding, height - padding), Offset(width - padding, height - padding), 2f )
                                // Líneas y Puntos
                                val path = Path()
                                sessions.forEachIndexed { index, session ->
                                    val x = padding + if (sessions.size > 1) index * (graphWidth / (sessions.size - 1)) else graphWidth / 2 // Centrar si hay 1 punto
                                    val y = (height - padding) - (session.stars / maxScore).coerceIn(0f, 1f) * graphHeight // Asegura valor entre 0 y 1
                                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                    drawCircle( Color(0xFF4CAF50), radius = 6.dp.toPx(), center = Offset(x, y) ) // Color verde para puntos
                                }
                                drawPath( path = path, color = Color(0xFF2196F3), style = Stroke(width = 3.dp.toPx()) ) // Color azul para línea
                                // Etiquetas
                                drawIntoCanvas { canvas ->
                                    val paint = android.graphics.Paint().apply { color = Color.Black.hashCode(); textSize = 12.sp.toPx(); textAlign = android.graphics.Paint.Align.CENTER }
                                    val paintAxis = android.graphics.Paint().apply { color = Color.Black.hashCode(); textSize = 10.sp.toPx(); textAlign = android.graphics.Paint.Align.RIGHT } // Para eje Y
                                    for (i in 0..3) { // Eje Y
                                        val y = (height - padding) - (i / 3f) * graphHeight
                                        canvas.nativeCanvas.drawText("$i ⭐", padding - 8.dp.toPx(), y + 5.dp.toPx(), paintAxis)
                                    }
                                    val numLabelsX = minOf(sessions.size, 5) // Máximo 5 etiquetas X
                                    for (i in 0 until numLabelsX) { // Eje X
                                        val index = (sessions.size -1) * i / (numLabelsX -1).coerceAtLeast(1)
                                        val session = sessions[index]
                                        val x = padding + if (sessions.size > 1) index * (graphWidth / (sessions.size - 1)) else graphWidth / 2
                                        val date = SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(session.timestamp))
                                        canvas.nativeCanvas.drawText(date, x, height - padding + 15.dp.toPx(), paint)
                                    }
                                }
                            } // Fin Canvas
                        } // Fin else (hay sesiones para gráfico)
                    } // Fin Column Gráfico
                } // Fin Card Gráfico

                // --- Lista de sesiones detalladas ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text( "📋 Sesiones Detalladas", style = MaterialTheme.typography.titleLarge )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isLoadingSessions) {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center){ CircularProgressIndicator() }
                        } else if (sessions.isEmpty()) {
                            Text( "No hay sesiones registradas para este filtro.", style = MaterialTheme.typography.bodyMedium )
                        } else {
                            // Usamos Column aquí porque LazyColumn dentro de otra Column con scroll vertical es problemático.
                            // Si esperas MUCHAS sesiones, habría que repensar la estructura general.
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                sessions.forEach { session ->
                                    SessionItem(session, availableGames) // Usa el SessionItem mejorado
                                }
                            }
                        }
                    }
                } // Fin Card Sesiones Detalladas
            } // Fin Column principal
        } // Fin else (carga inicial completa)
    } // Fin Scaffold
}


// --- Composable SessionItem Mejorado (ya definido en ChildProgressScreen, asegúrate que esté aquí o importado) ---
@Composable
fun SessionItem(session: GameSession, availableGames: List<Game>) {
    val gameDisplayName = availableGames.find { it.name == session.gameType }?.displayName ?: session.gameType
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(session.timestamp))
            Text( text = "🎮 $gameDisplayName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary )
            Divider(modifier = Modifier.padding(bottom = 4.dp))
            InfoItemSimple(icon = Icons.Filled.Event, text = "Fecha: $date")
            InfoItemSimple(icon = Icons.Filled.Star, text = "Estrellas: ${session.stars} ${"⭐".repeat(session.stars)}")
            InfoItemSimple(icon = Icons.Filled.Timer, text = "Tiempo: ${session.timeTaken / 1000} seg")
            InfoItemSimple(icon = Icons.Filled.Repeat, text = "Intentos: ${session.attempts}")
        }
    }
}

// --- Composable auxiliar simple para info con icono (ya definido en ChildProgressScreen, asegúrate que esté aquí o importado) ---
@Composable
private fun InfoItemSimple(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon( imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = LocalContentColor.current.copy(alpha = 0.8f) )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}