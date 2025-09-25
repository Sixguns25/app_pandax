package com.tesis.aplicacionpandax.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.entity.Specialty
import com.tesis.aplicacionpandax.repository.AuthRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterSpecialistScreen(
    repo: AuthRepository,
    db: AppDatabase,
    specialistId: Long = -1L, // Agregado para edición
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val specialties by db.specialtyDao().getAll().collectAsState(initial = emptyList())
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedSpecialtyId by remember { mutableStateOf<Long?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    // Cargar datos si es edición
    LaunchedEffect(specialistId) {
        if (specialistId != -1L) {
            coroutineScope.launch {
                val specialist = db.specialistDao().getByUserId(specialistId)
                val user = db.userDao().getById(specialistId)
                if (specialist != null && user != null) {
                    username = user.username
                    firstName = specialist.firstName
                    lastName = specialist.lastName
                    phone = specialist.phone
                    email = specialist.email
                    selectedSpecialtyId = specialist.specialtyId
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            if (specialistId == -1L) "Registrar Especialista" else "Editar Especialista",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Usuario") },
            modifier = Modifier.fillMaxWidth(),
            enabled = specialistId == -1L // Username no editable
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth()
        )
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
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Teléfono") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo") },
            modifier = Modifier.fillMaxWidth()
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = specialties.find { it.id == selectedSpecialtyId }?.name ?: "Seleccionar",
                onValueChange = {},
                readOnly = true,
                label = { Text("Especialidad") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                specialties.forEach { spec ->
                    DropdownMenuItem(
                        text = { Text(spec.name) },
                        onClick = {
                            selectedSpecialtyId = spec.id
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (username.isBlank() || (specialistId == -1L && password.isBlank()) ||
                    firstName.isBlank() || lastName.isBlank() || phone.isBlank() ||
                    email.isBlank() || selectedSpecialtyId == null
                ) {
                    message = "Por favor, completa todos los campos"
                    return@Button
                }
                coroutineScope.launch {
                    if (specialistId == -1L) {
                        // Registrar nuevo especialista
                        repo.registerSpecialist(
                            username = username,
                            password = password,
                            firstName = firstName,
                            lastName = lastName,
                            phone = phone,
                            email = email,
                            specialtyId = selectedSpecialtyId!!
                        ).fold(
                            onSuccess = {
                                message = "Especialista registrado correctamente ✅"
                                onBack()
                            },
                            onFailure = { message = "Error: ${it.message}" }
                        )
                    } else {
                        // Actualizar especialista existente
                        repo.updateSpecialist(
                            specialistId = specialistId,
                            firstName = firstName,
                            lastName = lastName,
                            phone = phone,
                            email = email,
                            specialtyId = selectedSpecialtyId!!,
                            password = if (password.isNotBlank()) password else null
                        ).fold(
                            onSuccess = {
                                message = "Especialista actualizado correctamente ✅"
                                onBack()
                            },
                            onFailure = { message = "Error: ${it.message}" }
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (specialistId == -1L) "Registrar" else "Actualizar")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Volver")
        }

        message?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.primary)
        }
    }
}