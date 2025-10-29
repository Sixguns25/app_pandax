package com.tesis.aplicacionpandax.ui.screens.child

import androidx.compose.foundation.Image // Para el logo/imagen (opcional)
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape // Para el botón
import androidx.compose.material.icons.Icons // Para iconos
import androidx.compose.material.icons.filled.* // Importar iconos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource // Para cargar imagen del logo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign // Para centrar texto
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // Para ajustar tamaños de fuente
import androidx.navigation.NavController // Necesario para navegar a juegos
import com.tesis.aplicacionpandax.R // Asegúrate de tener tu logo aquí
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.entity.Child
import com.tesis.aplicacionpandax.data.entity.Specialist
import kotlinx.coroutines.flow.map // Para calcular estrellas totales
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildHomeSection(
    child: Child?,
    specialist: Specialist?,
    db: AppDatabase, // Necesario para obtener especialidad y progreso
    navController: NavController // Necesario para navegar a la pantalla de juegos
) {
    val scope = rememberCoroutineScope()
    var specialtyName by remember { mutableStateOf<String?>(null) }
    var isLoadingSpecialty by remember { mutableStateOf(specialist != null) }

    // Estado para estrellas totales
    val totalStarsFlow = remember(child?.userId) {
        child?.userId?.let { id ->
            // Creamos un Flow que calcula la suma de estrellas
            db.gameSessionDao().getSessionsByChild(id).map { sessions ->
                sessions.sumOf { it.stars } // Suma las estrellas de todas las sesiones
            }
        } ?: kotlinx.coroutines.flow.flowOf(0) // Flujo con 0 si no hay ID de niño
    }
    val totalStars by totalStarsFlow.collectAsState(initial = 0) // Recolecta el total de estrellas

    // Cargar nombre de la especialidad
    LaunchedEffect(specialist?.specialtyId) {
        if (specialist != null) {
            isLoadingSpecialty = true
            scope.launch {
                val specialty = db.specialtyDao().getById(specialist.specialtyId)
                specialtyName = specialty?.name ?: "Desconocida"
                isLoadingSpecialty = false
            }
        } else {
            specialtyName = null
            isLoadingSpecialty = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("¡Bienvenido/a, ${child?.firstName ?: "Campeón/a"}!") }, // Saludo más cálido
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
            horizontalAlignment = Alignment.CenterHorizontally, // Centra los elementos
            verticalArrangement = Arrangement.SpaceAround // Distribuye espacio verticalmente
        ) {
            // Mensaje si no hay datos del niño
            if (child == null) {
                Text(
                    "No se encontró tu información.",
                    style = MaterialTheme.typography.bodyLarge
                )
                // Early return
                return@Scaffold
            }

            // --- Logo / Imagen de Bienvenida (Opcional) ---
            Image(
                painter = painterResource(id = R.drawable.home_child_pandax), // Reemplaza con tu logo
                contentDescription = "Logo Pandax",
                modifier = Modifier.size(300.dp) // Tamaño ajustable
            )

            // --- Resumen de Progreso Simple ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Tu Progreso", style = MaterialTheme.typography.titleMedium)
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Total de Estrellas Ganadas:", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$totalStars ⭐",
                            style = MaterialTheme.typography.headlineSmall, // Más grande
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = if (totalStars > 10) "¡Eres increíble! ✨" else if (totalStars > 0) "¡Sigue así! 👍" else "¡Juega para ganar estrellas! 🚀",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }


            // --- Botón Principal para Jugar ---
            Button(
                onClick = { navController.navigate("games") }, // Navega a la pestaña/ruta de juegos
                modifier = Modifier
                    .fillMaxWidth(0.8f) // Ocupa el 80% del ancho
                    .height(60.dp), // Botón grande
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Filled.Games, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text("¡Vamos a Jugar!", fontSize = 18.sp)
            }


            // --- Información del Especialista (más discreta) ---
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Tu Especialista:", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                if (specialist == null) {
                    Text("No asignado", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(
                        "${specialist.firstName} ${specialist.lastName}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (isLoadingSpecialty) {
                        Text("Cargando especialidad...", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text(
                            "(${specialtyName ?: "Desconocida"})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

        } // Fin Column principal
    } // Fin Scaffold
}

// Composable auxiliar InfoItem (Puede quitarse de este archivo si está en 'components')
@Composable
private fun InfoItem(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(90.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}