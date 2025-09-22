package com.tesis.aplicacionpandax.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SpecialistProfileScreen(specialistId: Long) {
    Column(Modifier.padding(16.dp)) {
        Text("Perfil del especialista (ID: $specialistId)", style = MaterialTheme.typography.headlineSmall)
    }
}
