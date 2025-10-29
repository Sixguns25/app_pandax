package com.tesis.aplicacionpandax.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.entity.Specialty
import com.tesis.aplicacionpandax.ui.viewmodel.SpecialtiesUiState // Importa el estado
import com.tesis.aplicacionpandax.ui.viewmodel.SpecialtiesViewModel // Importa el ViewModel
import com.tesis.aplicacionpandax.ui.viewmodel.SpecialtiesViewModelFactory // Importa la Factory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialtiesManagementScreen(
    db: AppDatabase, // Mantenemos db para la Factory
    onBack: () -> Unit
) {
    // Obtén los DAOs necesarios para la Factory
    val specialtyDao = db.specialtyDao()
    val specialistDao = db.specialistDao()

    // Crea e instancia el ViewModel usando la Factory
    val viewModel: SpecialtiesViewModel = viewModel(
        factory = SpecialtiesViewModelFactory(specialtyDao, specialistDao)
    )

    // Observa el estado de la UI desde el ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // Para mostrar mensajes en Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope() // Para lanzar el Snackbar

    // Muestra el Snackbar cuando hay un mensaje en el estado
    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
                viewModel.clearMessage() // Notifica al ViewModel que el mensaje se mostró
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text("Gestionar Especialidades", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            // Formulario para agregar/editar especialidad
            OutlinedTextField(
                value = uiState.currentInputName, // Vinculado al estado del ViewModel
                onValueChange = { viewModel.updateInputName(it) }, // Llama a la acción del ViewModel
                label = { Text(if (uiState.editingSpecialty != null) "Editar Especialidad" else "Nueva Especialidad") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = uiState.isLoading // No editable mientras carga
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Botón Guardar / Agregar
            Button(
                onClick = { viewModel.saveSpecialty() }, // Llama a la acción del ViewModel
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading // Deshabilitado mientras carga
            ) {
                if (uiState.isLoading && uiState.editingSpecialty == null) { // Indicador solo si está agregando
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (uiState.editingSpecialty != null) "Guardar Cambios" else "Agregar Especialidad")
                }
            }

            // Botón Cancelar Edición (visible solo si estamos editando)
            if (uiState.editingSpecialty != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.cancelEditing() }, // Llama a la acción del ViewModel
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    enabled = !uiState.isLoading
                ) {
                    Text("Cancelar Edición")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de especialidades
            Text("Especialidades Existentes", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.specialties, key = { it.id }) { specialty ->
                    SpecialtyItem(
                        specialty = specialty,
                        onEditClick = { viewModel.startEditing(specialty) }, // Llama a la acción
                        onDeleteClick = { viewModel.deleteSpecialty(specialty) }, // Llama a la acción
                        isLoading = uiState.isLoading // Pasa el estado de carga
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f)) // Empuja el botón Volver hacia abajo

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                enabled = !uiState.isLoading
            ) {
                Text("Volver")
            }
        } // Fin Column principal
    } // Fin Scaffold
}

// Composable auxiliar para mostrar cada item de especialidad
@Composable
private fun SpecialtyItem(
    specialty: Specialty,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    isLoading: Boolean // Recibe el estado de carga general
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp), // Ajusta padding vertical
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(specialty.name, modifier = Modifier.weight(1f).padding(end = 8.dp)) // Permite que el texto ocupe espacio
            Row {
                Button(
                    onClick = onEditClick,
                    modifier = Modifier.padding(end = 8.dp),
                    enabled = !isLoading // Deshabilita si está cargando
                ) {
                    Text("Editar")
                }
                Button(
                    onClick = onDeleteClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !isLoading // Deshabilita si está cargando
                ) {
                    Text("Eliminar")
                }
            }
        }
    }
}