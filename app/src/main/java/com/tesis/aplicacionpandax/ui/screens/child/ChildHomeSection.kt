package com.tesis.aplicacionpandax.ui.screens.child

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tesis.aplicacionpandax.data.entity.Child
import com.tesis.aplicacionpandax.data.entity.Specialist
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChildHomeSection(
    child: Child?,
    specialist: Specialist?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (child == null) {
            Text("No se encontró información del niño.")
            return@Column
        }

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val birthDate = Date(child.birthDateMillis)

        Text("Bienvenido ${child.firstName} ${child.lastName}", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))

        Text("DNI: ${child.dni}")
        Text("Condición: ${child.condition}")
        Text("Sexo: ${child.sex}")
        Text("Fecha de Nacimiento: ${sdf.format(birthDate)}")
        Spacer(modifier = Modifier.height(8.dp))
        Text("Apoderado: ${child.guardianName} (${child.guardianPhone})")

        Spacer(modifier = Modifier.height(16.dp))
        Text("Especialista asignado:", style = MaterialTheme.typography.titleMedium)
        if (specialist == null) {
            Text("No asignado")
        } else {
            Text("${specialist.firstName} ${specialist.lastName} - ${specialist.specialty}")
        }
    }
}
