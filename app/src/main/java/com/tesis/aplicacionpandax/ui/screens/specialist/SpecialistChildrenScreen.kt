package com.tesis.aplicacionpandax.ui.screens.specialist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tesis.aplicacionpandax.data.entity.Child
import kotlinx.coroutines.flow.Flow
import androidx.compose.runtime.collectAsState

@Composable
fun SpecialistChildrenScreen(
    childrenFlow: Flow<List<Child>>
) {
    val children by childrenFlow.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Niños asignados:", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (children.isEmpty()) {
            Text("No tienes niños asignados todavía.")
        } else {
            LazyColumn {
                items(children) { child ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("${child.firstName} ${child.lastName}", style = MaterialTheme.typography.titleMedium)
                            Text("DNI: ${child.dni}")
                            Text("Condición: ${child.condition}")
                            Text("Apoderado: ${child.guardianName} (${child.guardianPhone})")
                        }
                    }
                }
            }
        }
    }
}
