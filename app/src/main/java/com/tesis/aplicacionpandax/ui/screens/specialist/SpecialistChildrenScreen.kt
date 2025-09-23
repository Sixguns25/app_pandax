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
import androidx.navigation.NavController

@Composable
fun SpecialistChildrenScreen(
    childrenFlow: Flow<List<Child>>,
    navController: NavController
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
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = {
                                navController.navigate("child_progress/${child.userId}")  // Nueva route
                            }) {
                                Text("Ver Progreso")
                            }
                        }
                    }
                }
            }
        }
    }
}
