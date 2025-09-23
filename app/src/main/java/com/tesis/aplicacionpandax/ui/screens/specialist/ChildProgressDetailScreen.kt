package com.tesis.aplicacionpandax.ui.screens.specialist

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
    var selectedFilter by remember { mutableStateOf("Todas") }
    val filterOptions = listOf("Todas", "Últimos 7 días", "Últimos 30 días")
    var expanded by remember { mutableStateOf(false) }

    // Calcular rangos de fechas
    val calendar = Calendar.getInstance()
    val endTime = System.currentTimeMillis()
    val startTime = when (selectedFilter) {
        "Últimos 7 días" -> calendar.apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
        "Últimos 30 días" -> calendar.apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis
        else -> 0L // Todas
    }

    // Obtener datos
    var sessions by remember { mutableStateOf<List<GameSession>>(emptyList()) }
    var summary by remember { mutableStateOf<ProgressSummary?>(null) }
    LaunchedEffect(selectedFilter, childUserId) {
        if (selectedFilter == "Todas") {
            progressRepo.getSessionsForChild(childUserId).collectLatest { sessions = it }
            progressRepo.getProgressSummary(childUserId).collectLatest { summary = it }
        } else {
            progressRepo.getSessionsByDateRange(childUserId, startTime, endTime).collectLatest { sessions = it }
            progressRepo.getProgressSummaryByDateRange(childUserId, startTime, endTime).collectLatest { summary = it }
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
            Text("Progreso del Niño (ID: $childUserId)", style = MaterialTheme.typography.headlineMedium)

            // Filtro por fechas
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedFilter,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Filtrar por fecha") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .semantics { contentDescription = "Filtrar por fecha" },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    filterOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                selectedFilter = option
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Resumen de progreso
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Resumen", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    summary?.let {
                        Text("Sesiones jugadas: ${it.sessionCount}")
                        Text("Promedio de puntaje: %.2f".format(it.averageScore))
                        Text("Tiempo promedio por sesión: %.2f segundos".format(it.averageTimeTaken))
                        Text("Promedio de intentos: %.2f".format(it.averageAttempts))
                    } ?: Text("Cargando resumen...")
                }
            }

            // Gráfico de líneas con Canvas
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Progreso a lo largo del tiempo", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (sessions.isEmpty()) {
                        Text("No hay datos para mostrar en el gráfico.")
                    } else {
                        val maxScore = sessions.maxOfOrNull { it.score }?.toFloat() ?: 5f
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .semantics { contentDescription = "Gráfico de progreso" }
                        ) {
                            val width = size.width
                            val height = size.height
                            val padding = 32.dp.toPx()
                            val graphWidth = width - 2 * padding
                            val graphHeight = height - 2 * padding

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
                                val y = (height - padding) - (session.score / maxScore) * graphHeight
                                if (index == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }
                                // Dibujar punto
                                drawCircle(
                                    color = Color.Blue,
                                    radius = 4.dp.toPx(),
                                    center = Offset(x, y)
                                )
                            }
                            drawPath(
                                path = path,
                                color = Color.Blue,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }
            }

            // Lista de sesiones
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Sesiones Detalladas", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (sessions.isEmpty()) {
                        Text("No hay sesiones registradas.")
                    } else {
                        Column {
                            sessions.forEach { session ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                                .format(Date(session.timestamp))
                                        )
                                        Text("${session.score} ⭐ | ${session.timeTaken / 1000}s ⏱ | ${session.attempts} 🔄")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}