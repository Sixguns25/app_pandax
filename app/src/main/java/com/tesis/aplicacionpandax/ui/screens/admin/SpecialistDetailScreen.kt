package com.tesis.aplicacionpandax.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.entity.Specialist
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialistDetailScreen(
    navController: NavController,
    db: AppDatabase,
    specialistId: Long
) {
    val coroutineScope = rememberCoroutineScope()
    var specialist by remember { mutableStateOf<Specialist?>(null) }
    var username by remember { mutableStateOf("") }
    var specialtyName by remember { mutableStateOf("") }

    // Cargar datos del especialista
    LaunchedEffect(specialistId) {
        coroutineScope.launch {
            specialist = db.specialistDao().getByUserId(specialistId)
            val user = db.userDao().getById(specialistId)
            username = user?.username ?: "Desconocido"
            val specialty = db.specialtyDao().getById(specialist?.specialtyId ?: -1)
            specialtyName = specialty?.name ?: "Desconocida"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Detalles del Especialista", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (specialist == null) {
            Text("Cargando...", style = MaterialTheme.typography.bodyLarge)
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Usuario: $username", style = MaterialTheme.typography.bodyLarge)
                    Text("Nombres: ${specialist!!.firstName}", style = MaterialTheme.typography.bodyLarge)
                    Text("Apellidos: ${specialist!!.lastName}", style = MaterialTheme.typography.bodyLarge)
                    Text("Teléfono: ${specialist!!.phone}", style = MaterialTheme.typography.bodyLarge)
                    Text("Correo: ${specialist!!.email}", style = MaterialTheme.typography.bodyLarge)
                    Text("Especialidad: $specialtyName", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Volver")
        }
    }
}