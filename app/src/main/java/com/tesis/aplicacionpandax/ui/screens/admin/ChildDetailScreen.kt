package com.tesis.aplicacionpandax.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.entity.Child
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildDetailScreen(
    navController: NavController,
    db: AppDatabase,
    childId: Long
) {
    val coroutineScope = rememberCoroutineScope()
    var child by remember { mutableStateOf<Child?>(null) }
    var specialistName by remember { mutableStateOf("No asignado") }

    // Cargar datos del niño
    LaunchedEffect(childId) {
        coroutineScope.launch {
            child = db.childDao().getByUserId(childId)
            child?.specialistId?.let { specialistId ->
                val specialist = db.specialistDao().getByUserId(specialistId)
                specialistName = specialist?.let { "${it.firstName} ${it.lastName}" } ?: "No asignado"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Detalles del Niño", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (child == null) {
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
                    Text("Nombres: ${child!!.firstName} ${child!!.lastName}", style = MaterialTheme.typography.bodyLarge)
                    Text("DNI: ${child!!.dni}", style = MaterialTheme.typography.bodyLarge)
                    Text("Condición: ${child!!.condition}", style = MaterialTheme.typography.bodyLarge)
                    Text("Sexo: ${child!!.sex}", style = MaterialTheme.typography.bodyLarge)
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    Text("Fecha de Nacimiento: ${sdf.format(Date(child!!.birthDateMillis))}", style = MaterialTheme.typography.bodyLarge)
                    Text("Apoderado: ${child!!.guardianName} (${child!!.guardianPhone})", style = MaterialTheme.typography.bodyLarge)
                    Text("Especialista: $specialistName", style = MaterialTheme.typography.bodyLarge)
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