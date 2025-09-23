package com.tesis.aplicacionpandax.ui.screens.child

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tesis.aplicacionpandax.data.entity.Child

@Composable
fun ChildProgressScreen(
    child: Child?
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

        Text("Progreso de ${child.firstName} ${child.lastName}", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Aquí se mostrarán gráficos y avances personalizados (placeholder).")
    }
}
