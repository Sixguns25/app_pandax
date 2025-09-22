package com.tesis.aplicacionpandax.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tesis.aplicacionpandax.data.entity.Child

@Composable
fun ChildProfileScreen(
    child: Child?,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (child != null) {
            Text("Perfil", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Nombre: ${child.firstName} ${child.lastName}")
            Text("DNI: ${child.dni}")
            Text("Condición: ${child.condition}")
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Text("No se encontró información del niño.")
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar sesión")
        }
    }
}
