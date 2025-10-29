package com.tesis.aplicacionpandax.ui.screens.admin

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.tesis.aplicacionpandax.data.AppDatabase // <-- IMPORTACIÓN AÑADIDA
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
    db: AppDatabase, // <-- 👇 PARÁMETRO AÑADIDO
    specialistId: Long? = null,
    childId: Long? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    // --- Estados del formulario ---
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var dni by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") }
    var sexDisplay by remember { mutableStateOf("") }
    var birthDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var guardianName by remember { mutableStateOf("") }
    var guardianPhone by remember { mutableStateOf("") }
    var selectedSpecialistId by remember { mutableStateOf<Long?>(specialistId) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // --- Estados de UI ---
    var showDatePicker by remember { mutableStateOf(false) }
    var expandedSpecialist by remember { mutableStateOf(false) }
    var expandedSex by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isDataLoading by remember { mutableStateOf(childId != null && childId != -1L) }

    val sexOptions = listOf("Masculino" to "M", "Femenino" to "F")

    // --- Cargar datos si es modo edición ---
    LaunchedEffect(childId) {
        if (childId != null && childId != -1L) {
            isDataLoading = true
            // Usa la instancia 'db' pasada como parámetro
            val child = db.childDao().getByUserId(childId)
            val user = db.userDao().getById(childId)
            if (child != null && user != null) {
                username = user.username
                firstName = child.firstName
                lastName = child.lastName
                dni = child.dni
                condition = child.condition
                sex = child.sex
                sexDisplay = sexOptions.find { it.second == child.sex }?.first ?: ""
                birthDateMillis = child.birthDateMillis
                guardianName = child.guardianName
                guardianPhone = child.guardianPhone
                selectedSpecialistId = specialistId ?: child.specialistId
            } else {
                scope.launch { snackbarHostState.showSnackbar("Error: No se encontraron los datos del niño.") }
            }
            isDataLoading = false
        } else {
            isDataLoading = false
        }
    }

    // --- Obtener nombre del especialista fijo ---
    var fixedSpecialistName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(specialistId) {
        if (specialistId != null) {
            scope.launch {
                val specialist = db.specialistDao().getByUserId(specialistId)
                fixedSpecialistName = specialist?.let { "${it.firstName} ${it.lastName}" }
            }
        }
    }

    // --- Función de Validación ---
    fun validateInputs(): String? {
        if (username.isBlank() || firstName.isBlank() || lastName.isBlank() ||
            dni.isBlank() || condition.isBlank() || sex.isBlank() || guardianName.isBlank() || guardianPhone.isBlank()
        ) {
            return "Por favor, completa todos los campos obligatorios (*)"
        }
        if (!dni.matches(Regex("\\d{8}"))) {
            return "DNI debe tener exactamente 8 dígitos"
        }
        if (!guardianPhone.matches(Regex("\\d{9,15}"))) {
            return "Teléfono debe tener entre 9 y 15 dígitos"
        }
        val finalChildId = childId ?: -1L
        if (finalChildId == -1L && password.isBlank()) {
            return "La contraseña es obligatoria al registrar (*)"
        }
        if (password.isNotBlank() && password.length < 6) {
            return "La contraseña debe tener al menos 6 caracteres"
        }
        if (sex !in listOf("M", "F")) {
            return "Selecciona un sexo válido (*)"
        }
        val selectedCal = Calendar.getInstance().apply { timeInMillis = birthDateMillis }
        val todayCal = Calendar.getInstance()
        if (selectedCal.after(todayCal) &&
            !(selectedCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    selectedCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR))) {
            return "La fecha de nacimiento no puede ser futura"
        }
        return null
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (childId == null || childId == -1L) "Registrar Niño" else "Editar Niño") },
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
        if (isDataLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // --- Card: Cuenta del Niño ---
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Cuenta del Niño", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = username, onValueChange = { username = it }, label = { Text("Usuario *") },
                            modifier = Modifier.fillMaxWidth(), enabled = (childId == null || childId == -1L) && !isLoading,
                            singleLine = true, shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = password, onValueChange = { password = it },
                            label = { Text(if (childId == null || childId == -1L) "Contraseña *" else "Nueva Contraseña (opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon( if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null )
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true, enabled = !isLoading, shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // --- Card: Datos del Niño ---
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Datos del Niño", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField( value = firstName, onValueChange = { firstName = it }, label = { Text("Nombres *") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading, singleLine = true, shape = RoundedCornerShape(12.dp) )
                        OutlinedTextField( value = lastName, onValueChange = { lastName = it }, label = { Text("Apellidos *") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading, singleLine = true, shape = RoundedCornerShape(12.dp) )
                        OutlinedTextField( value = dni, onValueChange = { dni = it }, label = { Text("DNI (8 dígitos) *") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading, singleLine = true, shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number) )
                        OutlinedTextField( value = condition, onValueChange = { condition = it }, label = { Text("Condición *") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading, singleLine = true, shape = RoundedCornerShape(12.dp) )
                        ExposedDropdownMenuBox( expanded = expandedSex, onExpandedChange = { if (!isLoading) expandedSex = !expandedSex } ) {
                            OutlinedTextField(
                                value = sexDisplay, onValueChange = {}, readOnly = true, label = { Text("Sexo *") },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSex) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                enabled = !isLoading, shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(expanded = expandedSex, onDismissRequest = { expandedSex = false }) {
                                sexOptions.forEach { (display, value) ->
                                    DropdownMenuItem(text = { Text(display) }, onClick = { sexDisplay = display; sex = value; expandedSex = false })
                                }
                            }
                        }
                        OutlinedTextField(
                            value = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(birthDateMillis)),
                            onValueChange = {}, label = { Text("Fecha de Nacimiento *") },
                            modifier = Modifier.fillMaxWidth().clickable(enabled = !isLoading) { showDatePicker = true },
                            enabled = false, shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        )
                    }
                }

                // --- Card: Contacto del Niño ---
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Contacto del Niño", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField( value = guardianName, onValueChange = { guardianName = it }, label = { Text("Nombre Apoderado *") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading, singleLine = true, shape = RoundedCornerShape(12.dp) )
                        OutlinedTextField( value = guardianPhone, onValueChange = { guardianPhone = it }, label = { Text("Teléfono Apoderado (9-15 dígitos) *") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading, singleLine = true, shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone) )
                    }
                }

                // --- Especialista asignado o selección ---
                if (fixedSpecialistName != null) {
                    Text("Especialista Asignado:", style = MaterialTheme.typography.titleMedium)
                    Text(fixedSpecialistName ?: "Cargando...", style = MaterialTheme.typography.bodyLarge)
                } else {
                    Text("Seleccionar Especialista (Opcional):", style = MaterialTheme.typography.titleMedium)
                    val selectedSpecialistName = selectedSpecialistId?.let { id -> specialists.find { it.userId == id } }?.let { "${it.firstName} ${it.lastName}" } ?: "Ninguno"
                    ExposedDropdownMenuBox( expanded = expandedSpecialist, onExpandedChange = { if (!isLoading) expandedSpecialist = !expandedSpecialist } ) {
                        OutlinedTextField(
                            value = selectedSpecialistName, onValueChange = {}, readOnly = true,
                            label = { Text("Especialista") }, modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSpecialist) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            enabled = !isLoading, shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(expanded = expandedSpecialist, onDismissRequest = { expandedSpecialist = false }) {
                            DropdownMenuItem(text = { Text("Ninguno") }, onClick = { selectedSpecialistId = null; expandedSpecialist = false })
                            if (specialists.isEmpty()) { DropdownMenuItem(text = { Text("No hay especialistas") }, onClick = {}, enabled = false) }
                            else { specialists.forEach { sp -> DropdownMenuItem(text = { Text("${sp.firstName} ${sp.lastName}") }, onClick = { selectedSpecialistId = sp.userId; expandedSpecialist = false }) } }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Botones de Acción ---
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            val validationError = validateInputs()
                            if (validationError != null) { scope.launch { snackbarHostState.showSnackbar(validationError) }; return@Button }
                            scope.launch {
                                isLoading = true
                                val finalChildId = childId ?: -1L
                                val result = if (finalChildId == -1L) {
                                    repo.registerChild( username = username.trim(), password = password, firstName = firstName.trim(), lastName = lastName.trim(), dni = dni.trim(), condition = condition.trim(), sex = sex.trim(), birthDateMillis = birthDateMillis, guardianName = guardianName.trim(), guardianPhone = guardianPhone.trim(), specialistId = selectedSpecialistId )
                                } else {
                                    repo.updateChild( childId = finalChildId, firstName = firstName.trim(), lastName = lastName.trim(), dni = dni.trim(), condition = condition.trim(), sex = sex.trim(), birthDateMillis = birthDateMillis, guardianName = guardianName.trim(), guardianPhone = guardianPhone.trim(), specialistId = selectedSpecialistId, password = if (password.isNotBlank()) password else null )
                                }
                                isLoading = false
                                val messageToShow = result.fold( onSuccess = { if (finalChildId == -1L) "Niño registrado ✅" else "Niño actualizado ✅" }, onFailure = { "Error: ${it.message} ❌" } )
                                snackbarHostState.showSnackbar(messageToShow)
                                if (result.isSuccess) {
                                    if (finalChildId == -1L) {
                                        username = ""; password = ""; firstName = ""; lastName = ""; dni = ""; condition = ""; sex = ""; sexDisplay = ""; birthDateMillis = System.currentTimeMillis(); guardianName = ""; guardianPhone = ""; if (fixedSpecialistName == null) selectedSpecialistId = null
                                    } else { onBack() }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp), enabled = !isLoading, shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) { CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary) }
                        else { Text(if (childId == null || childId == -1L) "Registrar" else "Actualizar") }
                    }
                } // Fin Column Botones

                // --- DatePicker Dialog ---
                if (showDatePicker) {
                    val selectableDatesObject = remember {
                        object : SelectableDates {
                            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                                return utcTimeMillis <= System.currentTimeMillis()
                            }
                            override fun isSelectableYear(year: Int): Boolean {
                                return year >= 1980 && year <= Calendar.getInstance().get(Calendar.YEAR)
                            }
                        }
                    }
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = birthDateMillis,
                        yearRange = (1980..(Calendar.getInstance().get(Calendar.YEAR))),
                        selectableDates = selectableDatesObject
                    )
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            Button(onClick = {
                                datePickerState.selectedDateMillis?.let { selectedMillis ->
                                    if (selectedMillis <= System.currentTimeMillis()) { birthDateMillis = selectedMillis }
                                    else { scope.launch { snackbarHostState.showSnackbar("Fecha no puede ser futura.") } }
                                }
                                showDatePicker = false
                            }) { Text("Aceptar") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                } // Fin DatePicker Dialog
            } // Fin Column principal
        } // Fin else isDataLoading
    } // Fin Scaffold
}