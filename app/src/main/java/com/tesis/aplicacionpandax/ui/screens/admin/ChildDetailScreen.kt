package com.tesis.aplicacionpandax.ui.screens.admin // O com.tesis.aplicacionpandax.ui.screens.common si se usa en varios roles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState // Importar para scroll
import androidx.compose.foundation.verticalScroll // Importar para scroll
import androidx.compose.material.icons.Icons // Para iconos
import androidx.compose.material.icons.filled.* // Importar iconos específicos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector // Para icono en InfoItem
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.entity.Child
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class) // Para Scaffold, TopAppBar, Card
@Composable
fun ChildDetailScreen(
    navController: NavController,
    db: AppDatabase, // Necesario para cargar datos
    childId: Long
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState() // Añadir estado de scroll

    // Estados para los datos y carga
    var child by remember { mutableStateOf<Child?>(null) }
    var specialistName by remember { mutableStateOf<String?>(null) } // Puede ser null si no hay especialista
    var isLoading by remember { mutableStateOf(true) }

    // Cargar datos del niño y su especialista
    LaunchedEffect(childId) {
        if (childId == -1L) { // Verifica si el ID es válido
            isLoading = false
            // Podrías mostrar un Snackbar o mensaje aquí indicando ID inválido
            return@LaunchedEffect
        }
        isLoading = true
        scope.launch {
            child = db.childDao().getByUserId(childId)
            // Carga el nombre del especialista solo si el niño existe y tiene un ID de especialista
            child?.specialistId?.let { specId ->
                val specialist = db.specialistDao().getByUserId(specId)
                specialistName = specialist?.let { "${it.firstName} ${it.lastName}" }
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalles del Niño") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = { // Botón para volver
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box( // Box para centrar indicador de carga o mensaje de error
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp) // Padding interno
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                child == null -> {
                    Text(
                        "No se encontraron los detalles para este niño.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    // Contenido principal con scroll
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState), // Habilitar scroll
                        verticalArrangement = Arrangement.spacedBy(16.dp) // Espacio entre Cards
                    ) {
                        // --- Card: Datos Personales ---
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Datos Personales", style = MaterialTheme.typography.titleMedium)
                                Divider(modifier = Modifier.padding(vertical = 4.dp))

                                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                val birthDate = Date(child!!.birthDateMillis)
                                val sexText = when (child!!.sex) { "M" -> "Masculino"; "F" -> "Femenino"; else -> child!!.sex }

                                InfoItem(icon = Icons.Filled.Person, label = "Nombre:", value = "${child!!.firstName} ${child!!.lastName}")
                                InfoItem(icon = Icons.Filled.Badge, label = "DNI:", value = child!!.dni)
                                InfoItem(icon = Icons.Filled.AccessibilityNew, label = "Condición:", value = child!!.condition)
                                InfoItem(icon = if (child!!.sex == "M") Icons.Filled.Male else Icons.Filled.Female, label = "Sexo:", value = sexText)
                                InfoItem(icon = Icons.Filled.Cake, label = "Nacimiento:", value = sdf.format(birthDate))
                            }
                        }

                        // --- Card: Datos del Apoderado ---
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Datos del Apoderado", style = MaterialTheme.typography.titleMedium)
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                InfoItem(icon = Icons.Filled.SupervisorAccount, label = "Nombre:", value = child!!.guardianName)
                                InfoItem(icon = Icons.Filled.Phone, label = "Teléfono:", value = child!!.guardianPhone)
                            }
                        }

                        // --- Card: Especialista Asignado ---
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Especialista Asignado", style = MaterialTheme.typography.titleMedium)
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                InfoItem(icon = Icons.Filled.MedicalServices, label = "Nombre:", value = specialistName ?: "No asignado")
                                // Podrías añadir la especialidad si cargas el objeto Specialist completo
                            }
                        }
                    } // Fin Column principal
                } // Fin else (child != null)
            } // Fin when
        } // Fin Box
        // QUITA: El botón "Volver" inferior
    } // Fin Scaffold
}

// Composable auxiliar reutilizado (Asegúrate que exista, igual que en ChildProfileScreen)
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
            modifier = Modifier.width(100.dp) // Ajusta según necesidad
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}