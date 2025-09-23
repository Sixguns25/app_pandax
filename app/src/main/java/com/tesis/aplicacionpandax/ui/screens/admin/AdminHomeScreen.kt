package com.tesis.aplicacionpandax.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AdminHomeScreen(
    onRegisterSpecialist: () -> Unit,
    onRegisterChild: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Panel de Administrador", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRegisterSpecialist,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrar Especialista")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRegisterChild,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrar Niño")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onLogout) {
            Text("Cerrar sesión")
        }
    }
}
