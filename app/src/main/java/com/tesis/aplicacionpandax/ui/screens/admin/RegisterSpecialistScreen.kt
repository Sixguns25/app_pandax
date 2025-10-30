package com.tesis.aplicacionpandax.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel // <-- Importar viewModel
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.entity.Specialty
import com.tesis.aplicacionpandax.repository.AuthRepository
// Importar el ViewModel, Estado y Factory
import com.tesis.aplicacionpandax.ui.viewmodel.NavigationEvent
import com.tesis.aplicacionpandax.ui.viewmodel.RegisterSpecialistViewModel
import com.tesis.aplicacionpandax.ui.viewmodel.RegisterSpecialistViewModelFactory
import com.tesis.aplicacionpandax.ui.viewmodel.RegisterSpecialistUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterSpecialistScreen(
    repo: AuthRepository, // Necesario para la Factory
    db: AppDatabase, // Necesario para la Factory
    specialistId: Long = -1L, // Se pasa a la Factory
    onBack: () -> Unit
) {
    // --- Configuración del ViewModel ---
    // Crea e instancia el ViewModel usando la Factory
    val viewModel: RegisterSpecialistViewModel = viewModel(
        factory = RegisterSpecialistViewModelFactory(repo, db, specialistId)
    )
    // Observa el estado de la UI desde el ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // --- Hooks de Composable ---
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    // --- Estado de UI (solo para elementos visuales como dropdown o visibilidad) ---
    var isPasswordVisible by remember { mutableStateOf(false) }
    var expandedSpecialty by remember { mutableStateOf(false) }

    // --- Efectos para manejar eventos del ViewModel ---

    // Efecto para mostrar SnackBar
    LaunchedEffect(key1 = uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown() // Notifica al ViewModel que el mensaje se mostró
        }
    }

    // Efecto para manejar la navegación
    LaunchedEffect(key1 = uiState.navigationEvent) {
        uiState.navigationEvent?.let { event ->
            when (event) {
                is NavigationEvent.NavigateBack -> onBack()
            }
            viewModel.onNavigationDone() // Notifica al ViewModel que la navegación se manejó
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                // El título ahora lee desde el estado del ViewModel
                title = { Text(if (uiState.isEditing) "Editar Especialista" else "Registrar Especialista") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    // Deshabilita el botón de volver mientras carga
                    IconButton(onClick = onBack, enabled = !uiState.isLoading) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        // Indicador de carga inicial (leído desde el ViewModel)
        if (uiState.isLoadingData) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Contenido del formulario
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // --- Card: Cuenta de Usuario ---
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Cuenta de Usuario", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = uiState.username, // <-- Lee del ViewModel
                            onValueChange = viewModel::onUsernameChange, // <-- Llama al ViewModel
                            label = { Text("Usuario *") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isEditing && !uiState.isLoading, // Estado del ViewModel
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = uiState.password, // <-- Lee del ViewModel
                            onValueChange = viewModel::onPasswordChange, // <-- Llama al ViewModel
                            label = { Text(if (uiState.isEditing) "Nueva Contraseña (opcional)" else "Contraseña *") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon( if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null )
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            enabled = !uiState.isLoading,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // --- Card: Datos Personales ---
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Datos Personales", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = uiState.firstName, onValueChange = viewModel::onFirstNameChange, label = { Text("Nombres *") },
                            modifier = Modifier.fillMaxWidth(), enabled = !uiState.isLoading, singleLine = true, shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = uiState.lastName, onValueChange = viewModel::onLastNameChange, label = { Text("Apellidos *") },
                            modifier = Modifier.fillMaxWidth(), enabled = !uiState.isLoading, singleLine = true, shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = uiState.phone, onValueChange = viewModel::onPhoneChange, label = { Text("Teléfono *") },
                            modifier = Modifier.fillMaxWidth(), enabled = !uiState.isLoading, singleLine = true, shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                        OutlinedTextField(
                            value = uiState.email, onValueChange = viewModel::onEmailChange, label = { Text("Correo *") },
                            modifier = Modifier.fillMaxWidth(), enabled = !uiState.isLoading, singleLine = true, shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                        // Dropdown para Especialidad
                        ExposedDropdownMenuBox(
                            expanded = expandedSpecialty,
                            onExpandedChange = { if (!uiState.isLoading) expandedSpecialty = !expandedSpecialty }
                        ) {
                            OutlinedTextField(
                                value = uiState.specialties.find { it.id == uiState.selectedSpecialtyId }?.name ?: "Seleccionar *",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Especialidad *") },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSpecialty) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                enabled = !uiState.isLoading,
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedSpecialty,
                                onDismissRequest = { expandedSpecialty = false }
                            ) {
                                if (uiState.specialties.isEmpty()){
                                    DropdownMenuItem(text = { Text("No hay especialidades") }, onClick = { expandedSpecialty = false }, enabled=false)
                                } else {
                                    uiState.specialties.forEach { spec ->
                                        DropdownMenuItem(
                                            text = { Text(spec.name) },
                                            onClick = {
                                                viewModel.onSpecialtySelected(spec.id) // Llama a ViewModel
                                                expandedSpecialty = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- Botón de Acción ---
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.saveSpecialist() // <-- Llama al ViewModel
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !uiState.isLoading, // Leído del ViewModel
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isLoading) { // Leído del ViewModel
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(if (uiState.isEditing) "Actualizar" else "Registrar") // Leído del ViewModel
                    }
                }

                Spacer(modifier = Modifier.height(8.dp)) // Espacio al final

            } // Fin Column principal
        } // Fin else
    } // Fin Scaffold
}