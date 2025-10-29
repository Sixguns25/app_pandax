package com.tesis.aplicacionpandax.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape // Para formas
import androidx.compose.material.icons.Icons // Para iconos
import androidx.compose.material.icons.filled.ArrowBack // Icono de volver
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
    db: AppDatabase, // Necesario para la Factory del ViewModel
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
    val scope = rememberCoroutineScope() // Para lanzar el Snackbar

    // Muestra el Snackbar cuando hay un mensaje en el estado
    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
                viewModel.clearMessage() // Notifica al ViewModel que el mensaje se mostró
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Gestionar Especialidades") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !uiState.isLoading) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues) // Aplica padding de Scaffold
                .padding(16.dp), // Padding interno adicional
            verticalArrangement = Arrangement.spacedBy(16.dp) // Espacio entre elementos principales
        ) {

            // --- Card: Formulario para Agregar/Editar ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (uiState.editingSpecialty != null) "Editar Especialidad" else "Agregar Nueva Especialidad",
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        value = uiState.currentInputName,
                        onValueChange = { viewModel.updateInputName(it) },
                        label = { Text("Nombre de la Especialidad *") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = uiState.isLoading, // No editable mientras carga
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp) // Estilo consistente
                    )

                    // Botón Guardar / Agregar
                    Button(
                        onClick = { viewModel.saveSpecialty() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(12.dp) // Estilo consistente
                    ) {
                        // Muestra indicador solo si está cargando Y guardando (no al cargar lista)
                        val isSaving = uiState.isLoading && (uiState.editingSpecialty != null || uiState.currentInputName.isNotBlank())
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text(if (uiState.editingSpecialty != null) "Guardar Cambios" else "Agregar Especialidad")
                        }
                    }

                    // Botón Cancelar Edición (visible solo si estamos editando)
                    if (uiState.editingSpecialty != null) {
                        OutlinedButton( // Usar OutlinedButton para diferenciar de Guardar
                            onClick = { viewModel.cancelEditing() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isLoading,
                            shape = RoundedCornerShape(12.dp) // Estilo consistente
                        ) {
                            Text("Cancelar Edición")
                        }
                    }
                } // Fin Column interna de Card
            } // Fin Card Formulario

            // --- Lista de Especialidades Existentes ---
            Text("Especialidades Existentes", style = MaterialTheme.typography.titleMedium)

            // Usamos un Box para manejar el caso de lista vacía o carga
            Box(modifier = Modifier.weight(1f)) { // Ocupa el espacio restante
                if (uiState.specialties.isEmpty() && !uiState.isLoading) {
                    Text(
                        "No hay especialidades registradas.",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize() // Ocupa el espacio del Box
                    ) {
                        items(uiState.specialties, key = { it.id }) { specialty ->
                            SpecialtyItem(
                                specialty = specialty,
                                onEditClick = { viewModel.startEditing(specialty) },
                                onDeleteClick = { viewModel.deleteSpecialty(specialty) },
                                isLoading = uiState.isLoading
                            )
                        }
                        // Añadir espacio al final si es necesario
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
                // Indicador de carga general (opcional, si la carga inicial es lenta)
                /* if (uiState.isLoading && uiState.specialties.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } */
            }

            // El botón "Volver" ya está en la TopAppBar, por lo que se elimina el de abajo
            // Spacer(modifier = Modifier.weight(1f)) // Ya no es necesario
            // Button(onClick = onBack, ...) // Eliminado

        } // Fin Column principal
    } // Fin Scaffold
}

// Composable auxiliar para mostrar cada item de especialidad (Estilo Refinado)
@Composable
private fun SpecialtyItem(
    specialty: Specialty,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp) // Consistencia
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp), // Padding ajustado
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                specialty.name,
                modifier = Modifier.weight(1f).padding(end = 8.dp), // Ocupa espacio disponible
                style = MaterialTheme.typography.bodyLarge
            )
            // Botones de acción
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { // Menos espacio entre botones
                // TextButton para Editar (menos prominente)
                TextButton(
                    onClick = onEditClick,
                    enabled = !isLoading,
                    contentPadding = PaddingValues(horizontal = 8.dp) // Menos padding
                ) {
                    Text("Editar")
                }
                // Button rojo para Eliminar (más prominente)
                Button(
                    onClick = onDeleteClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !isLoading,
                    contentPadding = PaddingValues(horizontal = 8.dp) // Menos padding
                ) {
                    Text("Eliminar")
                }
            }
        }
    }
}