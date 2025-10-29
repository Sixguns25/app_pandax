package com.tesis.aplicacionpandax.ui.components

// QUITA: import androidx.compose.foundation.background // Ya no se necesita
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape // Sigue siendo útil para IconButtons si se estilizan
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // Importa iconos necesarios (Edit, Info, Star, Delete)
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
// QUITA: import androidx.compose.ui.draw.clip // Card ya usa shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tesis.aplicacionpandax.data.entity.Child

@OptIn(ExperimentalMaterial3Api::class) // Necesario para Card
@Composable
fun ChildCard(
    child: Child,
    specialistName: String, // Nombre del especialista asignado
    onEdit: () -> Unit,
    onDetail: () -> Unit,
    onDelete: () -> Unit,
    onProgress: () -> Unit, // Asegúrate de que se pase correctamente desde donde se usa
    enabled: Boolean = true // Para habilitar/deshabilitar acciones
) {
    Card(
        modifier = Modifier.fillMaxWidth(), // Ocupa todo el ancho disponible
        shape = MaterialTheme.shapes.medium, // Esquinas redondeadas consistentes
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), // Sombra más sutil
        colors = CardDefaults.cardColors( // Colores por defecto o ajustados al tema
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            // Compose maneja los colores deshabilitados automáticamente
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp), // Padding ajustado
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Información del Niño (Izquierda) ---
            Column(
                modifier = Modifier.weight(1f), // Permite que ocupe el espacio disponible
                verticalArrangement = Arrangement.spacedBy(2.dp) // Menos espacio vertical
            ) {
                Text(
                    text = "${child.firstName} ${child.lastName}",
                    style = MaterialTheme.typography.titleMedium, // Título un poco más grande
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface // Color principal para el nombre
                )
                Text(
                    text = "DNI: ${child.dni}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant // Color secundario
                )
                Text(
                    text = "Condición: ${child.condition}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Mostrar nombre del especialista
                Text(
                    text = "Especialista: $specialistName",
                    style = MaterialTheme.typography.bodySmall, // Más pequeño
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            // --- Botones de Acción (Derecha) ---
            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp) // Menos espacio (o 4.dp) entre botones
            ) {
                // IconButton simple (sin fondo explícito)
                IconButton(onClick = onEdit, enabled = enabled) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar ${child.firstName} ${child.lastName}",
                        tint = MaterialTheme.colorScheme.primary // Tinte primario
                    )
                }
                IconButton(onClick = onDetail, enabled = enabled) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Ver detalles de ${child.firstName} ${child.lastName}",
                        tint = MaterialTheme.colorScheme.secondary // Tinte secundario
                    )
                }
                IconButton(onClick = onProgress, enabled = enabled) {
                    Icon(
                        imageVector = Icons.Default.BarChart, // Icono alternativo para progreso
                        // imageVector = Icons.Default.Star, // O mantener estrella
                        contentDescription = "Ver progreso de ${child.firstName} ${child.lastName}",
                        tint = MaterialTheme.colorScheme.tertiary // Tinte terciario
                    )
                }
                IconButton(onClick = onDelete, enabled = enabled) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar ${child.firstName} ${child.lastName}",
                        tint = MaterialTheme.colorScheme.error // Tinte de error
                    )
                }
            } // Fin Row Botones
        } // Fin Row Principal
    } // Fin Card
}