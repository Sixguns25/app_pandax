package com.tesis.aplicacionpandax.ui.screens.specialist

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.entity.GameSession
import com.tesis.aplicacionpandax.repository.ProgressRepository
import com.tesis.aplicacionpandax.repository.ProgressSummary
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildProgressDetailScreen(
    childUserId: Long,
    progressRepo: ProgressRepository
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var childName by remember { mutableStateOf("Niño") }
    var isLoading by remember { mutableStateOf(true) }
    var selectedDateFilter by remember { mutableStateOf("Todas") }
    val dateFilterOptions = listOf("Hoy", "Todas", "Últimos 7 días", "Últimos 30 días")
    var expandedDate by remember { mutableStateOf(false) }
    var selectedGameType by remember { mutableStateOf("Todos") }
    val gameTypes = listOf("Todos", "MEMORY", "EMOTIONS")
    var expandedGameType by remember { mutableStateOf(false) }

    // Calcular rangos de fechas
    val calendar = Calendar.getInstance()
    val endTime = System.currentTimeMillis()
    val startTime = when (selectedDateFilter) {
        "Hoy" -> calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        "Últimos 7 días" -> calendar.apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
        "Últimos 30 días" -> calendar.apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis
        else -> 0L // Todas
    }

    // Obtener datos
    var sessions by remember { mutableStateOf<List<GameSession>>(emptyList()) }
    var summary by remember { mutableStateOf<ProgressSummary?>(null) }

    LaunchedEffect(childUserId) {
        val db = AppDatabase.getInstance(context, scope)
        db.childDao().getChildByUserId(childUserId).collectLatest { child ->
            childName = child?.firstName ?: "Niño"
        }
    }

    LaunchedEffect(selectedDateFilter, selectedGameType, childUserId) {
        isLoading = true
        val filteredSessions = if (selectedDateFilter != "Todas") {
            progressRepo.getSessionsByDateRange(childUserId, startTime, endTime)
        } else {
            progressRepo.getSessionsForChild(childUserId)
        }
        filteredSessions.collectLatest { allSessions ->
            sessions = if (selectedGameType != "Todos") {
                allSessions.filter { it.gameType == selectedGameType }
            } else {
                allSessions
            }
            summary = ProgressSummary(
                sessionCount = sessions.size,
                averageStars = if (sessions.isNotEmpty()) sessions.map { it.stars }.average().toFloat() else 0f,
                averageTimeTaken = if (sessions.isNotEmpty()) sessions.map { it.timeTaken / 1000f }.average().toFloat() else 0f,
                averageAttempts = if (sessions.isNotEmpty()) sessions.map { it.attempts.toFloat() }.average().toFloat() else 0f
            )
            isLoading = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Progreso de $childName",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { contentDescription = "Título: Progreso de $childName" }
            )

            // Filtros
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Filtro por fechas
                ExposedDropdownMenuBox(
                    expanded = expandedDate,
                    onExpandedChange = { expandedDate = !expandedDate },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedDateFilter,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Filtrar por fecha") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .semantics { contentDescription = "Filtro por fecha" },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDate,
                        onDismissRequest = { expandedDate = false }
                    ) {
                        dateFilterOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedDateFilter = option
                                    expandedDate = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Filtro por tipo de juego
                ExposedDropdownMenuBox(
                    expanded = expandedGameType,
                    onExpandedChange = { expandedGameType = !expandedGameType },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedGameType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Filtrar por juego") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .semantics { contentDescription = "Filtro por juego" },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedGameType,
                        onDismissRequest = { expandedGameType = false }
                    ) {
                        gameTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedGameType = type
                                    expandedGameType = false
                                }
                            )
                        }
                    }
                }
            }

            // Resumen de progreso
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Resumen de progreso" },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📊 Resumen de Progreso",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                .semantics { contentDescription = "Cargando resumen" }
                        )
                    } else {
                        summary?.let {
                            Text(
                                text = "Sesiones jugadas: ${it.sessionCount}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Promedio de estrellas: %.2f ⭐".format(it.averageStars),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Tiempo promedio: %.2f segundos".format(it.averageTimeTaken),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Promedio de intentos: %.2f".format(it.averageAttempts),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = when {
                                    it.averageStars >= 2.5 -> "¡Progreso excepcional! El niño muestra gran desempeño."
                                    it.averageStars >= 1.5 -> "Buen progreso, con oportunidad de mejora en consistencia."
                                    it.sessionCount > 0 -> "Progreso inicial, se recomienda más práctica."
                                    else -> "Aún no hay sesiones registradas."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } ?: Text(
                            text = "No hay datos disponibles para el filtro seleccionado.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Gráfico de líneas mejorado
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Gráfico de progreso a lo largo del tiempo" },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📈 Progreso a lo largo del tiempo",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (sessions.isEmpty()) {
                        Text(
                            text = "No hay datos para mostrar en el gráfico.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        val maxScore = 3f // Máximo de estrellas es 3 para MEMORY y EMOTIONS
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .background(Color.White)
                        ) {
                            val width = size.width
                            val height = size.height
                            val padding = 40.dp.toPx()
                            val graphWidth = width - 2 * padding
                            val graphHeight = height - 2 * padding

                            // Dibujar cuadrícula de fondo
                            for (i in 0..3) {
                                val y = (height - padding) - (i / 3f) * graphHeight
                                drawLine(
                                    color = Color.LightGray.copy(alpha = 0.3f),
                                    start = Offset(padding, y),
                                    end = Offset(width - padding, y),
                                    strokeWidth = 1f
                                )
                            }

                            // Dibujar ejes
                            drawLine(
                                color = Color.Gray,
                                start = Offset(padding, height - padding),
                                end = Offset(padding, padding),
                                strokeWidth = 2f
                            ) // Eje Y
                            drawLine(
                                color = Color.Gray,
                                start = Offset(padding, height - padding),
                                end = Offset(width - padding, height - padding),
                                strokeWidth = 2f
                            ) // Eje X

                            // Dibujar puntos y líneas
                            val path = Path()
                            sessions.forEachIndexed { index, session ->
                                val x = padding + index * (graphWidth / (sessions.size - 1).coerceAtLeast(1))
                                val y = (height - padding) - (session.stars / maxScore) * graphHeight
                                if (index == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }
                                // Dibujar punto
                                drawCircle(
                                    color = if (session.gameType == "MEMORY") Color.Blue else Color.Green,
                                    radius = 6.dp.toPx(),
                                    center = Offset(x, y)
                                )
                            }
                            drawPath(
                                path = path,
                                color = Color.Blue,
                                style = Stroke(width = 3.dp.toPx())
                            )

                            // Dibujar etiquetas
                            drawIntoCanvas { canvas ->
                                val paint = android.graphics.Paint().apply {
                                    color = Color.Black.hashCode()
                                    textSize = with(density) { 12.sp.toPx() }
                                    textAlign = android.graphics.Paint.Align.CENTER
                                }
                                // Etiquetas eje Y (estrellas)
                                for (i in 0..3) {
                                    val y = (height - padding) - (i / 3f) * graphHeight
                                    canvas.nativeCanvas.drawText("$i ⭐", padding - 20, y + 5, paint)
                                }
                                // Etiquetas eje X (fechas)
                                sessions.forEachIndexed { index, session ->
                                    val x = padding + index * (graphWidth / (sessions.size - 1).coerceAtLeast(1))
                                    val date = SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(session.timestamp))
                                    canvas.nativeCanvas.drawText(date, x, height - padding + 20, paint)
                                }
                            }
                        }
                    }
                }
            }

            // Lista de sesiones detalladas
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Lista de sesiones detalladas" },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📋 Sesiones Detalladas",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (sessions.isEmpty()) {
                        Text(
                            text = "No hay sesiones registradas para el filtro seleccionado.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        Column {
                            sessions.forEach { session ->
                                SessionItem(session)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SessionItem(session: GameSession) {
    val gameName = when (session.gameType) {
        "MEMORY" -> "Juego de Memoria"
        "EMOTIONS" -> "Juego de Emociones"
        else -> session.gameType
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Sesión de $gameName" },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(session.timestamp))
            Text(
                text = "🎮 Juego: $gameName",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "📅 Fecha: $date",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "⭐ Estrellas: ${session.stars} ${"⭐".repeat(session.stars)}",
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