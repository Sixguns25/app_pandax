package com.tesis.aplicacionpandax.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape // Importa para forma
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack // Para el botón de volver
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment // Para Box contentAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // Necesario para AppDatabase
import androidx.compose.ui.platform.LocalFocusManager // Para ocultar teclado
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.entity.Specialty // Asegúrate de tener este import
import com.tesis.aplicacionpandax.repository.AuthRepository
import kotlinx.coroutines.flow.collectLatest // Opcional, pero bueno para el flow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterSpecialistScreen(
    repo: AuthRepository,
    db: AppDatabase, // Necesario para cargar datos y obtener especialidades
    specialistId: Long = -1L, // -1L indica modo registro, otro valor indica edición
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    // Obtener especialidades
    val specialties by db.specialtyDao().getAll().collectAsState(initial = emptyList())

    // --- Estados del formulario ---
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedSpecialtyId by remember { mutableStateOf<Long?>(null) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // --- Estados de UI ---
    var expandedSpecialty by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) } // Para operación de guardado
    var isDataLoading by remember { mutableStateOf(specialistId != -1L) } // Para carga inicial en edición

    // --- Cargar datos si es modo edición ---
    LaunchedEffect(specialistId) {
        if (specialistId != -1L) {
            isDataLoading = true
            scope.launch {
                val specialist = db.specialistDao().getByUserId(specialistId)
                val user = db.userDao().getById(specialistId)
                if (specialist != null && user != null) {
                    username = user.username
                    firstName = specialist.firstName
                    lastName = specialist.lastName
                    phone = specialist.phone
                    email = specialist.email
                    selectedSpecialtyId = specialist.specialtyId
                } else {
                    scope.launch { snackbarHostState.showSnackbar("Error al cargar datos del especialista.") }
                }
                isDataLoading = false
            }
        } else {
            isDataLoading = false // No es edición
        }
    }

    // --- Función de Validación ---
    fun validateInputs(): String? {
        if (username.isBlank() || firstName.isBlank() || lastName.isBlank() || phone.isBlank() || email.isBlank() || selectedSpecialtyId == null) {
            return "Por favor, completa todos los campos obligatorios (*)"
        }
        if (specialistId == -1L && password.isBlank()) {
            return "La contraseña es obligatoria al registrar (*)"
        }
        if (password.isNotBlank() && password.length < 6) {
            return "La contraseña debe tener al menos 6 caracteres"
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "Formato de correo inválido"
        }
        if (!phone.matches(Regex("\\d{9,15}"))) {
            return "Teléfono debe tener entre 9 y 15 dígitos"
        }
        return null
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (specialistId == -1L) "Registrar Especialista" else "Editar Especialista") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isLoading) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        // Indicador de carga inicial
        if (isDataLoading) {
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
                verticalArrangement = Arrangement.spacedBy(16.dp) // Espacio entre elementos/cards
            ) {

                // --- Card: Cuenta de Usuario ---
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Cuenta de Usuario", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Usuario *") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = specialistId == -1L && !isLoading,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(if (specialistId == -1L) "Contraseña *" else "Nueva Contraseña (opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = if (isPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // --- Card: Datos Personales ---
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Datos Personales", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = firstName, onValueChange = { firstName = it }, label = { Text("Nombres *") },
                            modifier = Modifier.fillMaxWidth(), enabled = !isLoading, singleLine = true, shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = lastName, onValueChange = { lastName = it }, label = { Text("Apellidos *") },
                            modifier = Modifier.fillMaxWidth(), enabled = !isLoading, singleLine = true, shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = phone, onValueChange = { phone = it }, label = { Text("Teléfono *") },
                            modifier = Modifier.fillMaxWidth(), enabled = !isLoading, singleLine = true, shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                        OutlinedTextField(
                            value = email, onValueChange = { email = it }, label = { Text("Correo *") },
                            modifier = Modifier.fillMaxWidth(), enabled = !isLoading, singleLine = true, shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                        // Dropdown para Especialidad
                        ExposedDropdownMenuBox(
                            expanded = expandedSpecialty,
                            onExpandedChange = { if (!isLoading) expandedSpecialty = !expandedSpecialty }
                        ) {
                            OutlinedTextField(
                                value = specialties.find { it.id == selectedSpecialtyId }?.name ?: "Seleccionar *",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Especialidad *") },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSpecialty) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                enabled = !isLoading,
                                shape = RoundedCornerShape(12.dp) // Redondeado
                            )
                            ExposedDropdownMenu(
                                expanded = expandedSpecialty,
                                onDismissRequest = { expandedSpecialty = false }
                            ) {
                                if (specialties.isEmpty()){
                                    DropdownMenuItem(text = { Text("No hay especialidades") }, onClick = { expandedSpecialty = false }, enabled=false)
                                } else {
                                    specialties.forEach { spec ->
                                        DropdownMenuItem(
                                            text = { Text(spec.name) },
                                            onClick = {
                                                selectedSpecialtyId = spec.id
                                                expandedSpecialty = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- Botones de Acción ---
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { // Agrupa botones
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            val validationError = validateInputs()
                            if (validationError != null) {
                                scope.launch { snackbarHostState.showSnackbar(validationError) }
                                return@Button
                            }
                            scope.launch {
                                isLoading = true
                                val result = if (specialistId == -1L) {
                                    // Registrar nuevo especialista
                                    repo.registerSpecialist(
                                        username = username.trim(),
                                        password = password, // No necesita trim
                                        firstName = firstName.trim(),
                                        lastName = lastName.trim(),
                                        phone = phone.trim(),
                                        email = email.trim(),
                                        specialtyId = selectedSpecialtyId!! // Usa !! porque validateInputs ya asegura que no es null
                                    )
                                } else {
                                    // Actualizar especialista existente
                                    repo.updateSpecialist(
                                        specialistId = specialistId, // El ID del especialista que se está editando
                                        firstName = firstName.trim(),
                                        lastName = lastName.trim(),
                                        phone = phone.trim(),
                                        email = email.trim(),
                                        specialtyId = selectedSpecialtyId!!, // Usa !! por la validación
                                        password = if (password.isNotBlank()) password else null // Pasa null si el campo está vacío
                                    )
                                }
                                isLoading = false

                                val messageToShow = result.fold(
                                    onSuccess = { if (specialistId == -1L) "Especialista registrado ✅" else "Especialista actualizado ✅" },
                                    onFailure = { "Error: ${it.message} ❌" }
                                )
                                snackbarHostState.showSnackbar(messageToShow)
                                if (result.isSuccess) {
                                    onBack() // Vuelve atrás si la operación fue exitosa
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text(if (specialistId == -1L) "Registrar" else "Actualizar")
                        }
                    }

                    // Botón Volver (opcional si ya está en TopAppBar, puedes quitarlo si prefieres)
                    /*
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Volver")
                    }
                    */
                } // Fin Column Botones
            } // Fin Column principal del formulario
        } // Fin else isDataLoading
    } // Fin Scaffold
}