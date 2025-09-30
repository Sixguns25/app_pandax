package com.tesis.aplicacionpandax.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.entity.Specialist
import com.tesis.aplicacionpandax.repository.AuthRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterChildScreen(
    repo: AuthRepository,
    specialists: List<Specialist>,
    onBack: () -> Unit,
    specialistId: Long? = null,
    childId: Long? = null  // Agregado para modo edición
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    // Estados
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var dni by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") } // "M" o "F"
    var sexDisplay by remember { mutableStateOf("") } // "Masculino" o "Femenino"
    var birthDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var guardianName by remember { mutableStateOf("") }
    var guardianPhone by remember { mutableStateOf("") }
    var selectedSpecialistId by remember { mutableStateOf<Long?>(specialistId) }
    var message by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var expandedSpecialist by remember { mutableStateOf(false) }
    var expandedSex by remember { mutableStateOf(false) }

    // Opciones para el desplegable de sexo
    val sexOptions = listOf("Masculino" to "M", "Femenino" to "F")

    // Cargar datos si es modo edición
    LaunchedEffect(childId) {
        if (childId != null) {
            val db = AppDatabase.getInstance(context, scope)
            val child = db.childDao().getByUserId(childId)
            val user = db.userDao().getById(childId)
            if (child != null && user != null) {
                username = user.username
                firstName = child.firstName
                lastName = child.lastName
                dni = child.dni
                condition = child.condition
                sex = child.sex
                sexDisplay = if (child.sex == "M") "Masculino" else if (child.sex == "F") "Femenino" else ""
                birthDateMillis = child.birthDateMillis
                guardianName = child.guardianName
                guardianPhone = child.guardianPhone
                selectedSpecialistId = child.specialistId ?: specialistId
            }
        }
    }

    // Obtener nombre del especialista si specialistId no es null
    var specialistName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(specialistId) {
        if (specialistId != null) {
            val db = AppDatabase.getInstance(context, scope)
            db.specialistDao().getById(specialistId).collectLatest { specialist ->
                specialistName = specialist?.let { "${it.firstName} ${it.lastName}" }
            }
        }
    }

    // Validaciones
    fun validateInputs(): String? {
        if (username.isBlank() || firstName.isBlank() || lastName.isBlank() ||
            dni.isBlank() || condition.isBlank() || sex.isBlank() || guardianName.isBlank() || guardianPhone.isBlank()
        ) {
            return "Por favor, completa todos los campos"
        }
        if (!dni.matches(Regex("\\d{8}"))) {
            return "DNI debe tener exactamente 8 dígitos"
        }
        if (!guardianPhone.matches(Regex("\\d{9,15}"))) {
            return "Teléfono debe tener entre 9 y 15 dígitos"
        }
        if ((childId == null && password.isBlank()) || (password.isNotBlank() && password.length < 6)) {
            return "La contraseña debe tener al menos 6 caracteres"
        }
        if (sex !in listOf("M", "F")) {
            return "Sexo debe ser 'Masculino' o 'Femenino'"
        }
        if (birthDateMillis > System.currentTimeMillis()) {
            return "La fecha de nacimiento no puede ser futura"
        }
        return null
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.Top
        ) {
            Text(if (childId == null) "Registrar Niño" else "Editar Niño", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            // Sección: Cuenta del Niño
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Cuenta del Niño", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Usuario") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = childId == null  // No editable en modo edición
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(if (childId == null) "Contraseña" else "Nueva Contraseña (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sección: Datos del Niño
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Datos del Niño", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("Nombres") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Apellidos") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dni,
                        onValueChange = { dni = it },
                        label = { Text("DNI") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = condition,
                        onValueChange = { condition = it },
                        label = { Text("Condición") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Desplegable para Sexo
                    ExposedDropdownMenuBox(
                        expanded = expandedSex,
                        onExpandedChange = { expandedSex = !expandedSex }
                    ) {
                        OutlinedTextField(
                            value = sexDisplay,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Sexo") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expandedSex,
                            onDismissRequest = { expandedSex = false }
                        ) {
                            sexOptions.forEach { (display, value) ->
                                DropdownMenuItem(
                                    text = { Text(display) },
                                    onClick = {
                                        sexDisplay = display
                                        sex = value
                                        expandedSex = false
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(birthDateMillis)),
                        onValueChange = {},
                        label = { Text("Fecha de Nacimiento") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true },
                        enabled = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sección: Contacto del Niño
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Contacto del Niño", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = guardianName,
                        onValueChange = { guardianName = it },
                        label = { Text("Nombre Apoderado") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = guardianPhone,
                        onValueChange = { guardianPhone = it },
                        label = { Text("Teléfono Apoderado") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Especialista asignado o selección
            if (specialistId == null) {
                Text("Seleccionar Especialista:")
                val selectedSpecialistName = selectedSpecialistId
                    ?.let { id -> specialists.find { it.userId == id } }
                    ?.let { "${it.firstName} ${it.lastName}" }

                Box(modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { expandedSpecialist = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedSpecialistName ?: "Elegir especialista")
                    }
                    DropdownMenu(expanded = expandedSpecialist, onDismissRequest = { expandedSpecialist = false }) {
                        if (specialists.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No hay especialistas") },
                                onClick = { expandedSpecialist = false }
                            )
                        } else {
                            specialists.forEach { sp ->
                                DropdownMenuItem(
                                    text = { Text("${sp.firstName} ${sp.lastName}") },
                                    onClick = {
                                        selectedSpecialistId = sp.userId
                                        expandedSpecialist = false
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    "Especialista asignado: ${specialistName ?: "Cargando..."}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón Registrar/Actualizar
            var isLoading by remember { mutableStateOf(false) }
            Button(
                onClick = {
                    scope.launch {
                        // Validar inputs
                        message = validateInputs()
                        if (message != null) {
                            snackbarHostState.showSnackbar(message!!)
                            return@launch
                        }

                        isLoading = true
                        val result = if (childId == null) {
                            // Registro
                            repo.registerChild(
                                username = username.trim(),
                                password = password,
                                firstName = firstName.trim(),
                                lastName = lastName.trim(),
                                dni = dni.trim(),
                                condition = condition.trim(),
                                sex = sex.trim(),
                                birthDateMillis = birthDateMillis,
                                guardianName = guardianName.trim(),
                                guardianPhone = guardianPhone.trim(),
                                specialistId = selectedSpecialistId
                            )
                        } else {
                            // Actualización
                            repo.updateChild(
                                childId = childId,
                                firstName = firstName.trim(),
                                lastName = lastName.trim(),
                                dni = dni.trim(),
                                condition = condition.trim(),
                                sex = sex.trim(),
                                birthDateMillis = birthDateMillis,
                                guardianName = guardianName.trim(),
                                guardianPhone = guardianPhone.trim(),
                                specialistId = selectedSpecialistId,
                                password = if (password.isNotBlank()) password else null
                            )
                        }
                        isLoading = false
                        message = result.fold(
                            onSuccess = { if (childId == null) "Se registró al niño exitosamente ✅" else "Niño actualizado exitosamente ✅" },
                            onFailure = { "Error: ${it.message}" }
                        )
                        snackbarHostState.showSnackbar(message!!)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (childId == null) "Registrar" else "Actualizar")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Volver")
            }

            // DatePicker Dialog
            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = birthDateMillis)
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        Button(onClick = {
                            datePickerState.selectedDateMillis?.let { birthDateMillis = it }
                            showDatePicker = false
                        }) {
                            Text("Aceptar")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showDatePicker = false }) {
                            Text("Cancelar")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
        }
    }
}