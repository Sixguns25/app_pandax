package com.tesis.aplicacionpandax.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.entity.Specialty
import com.tesis.aplicacionpandax.repository.AuthRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterSpecialistScreen(
    repo: AuthRepository,
    db: AppDatabase,  // Agregado para acceder a specialtyDao
    onBack: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var selectedSpecialtyId by remember { mutableStateOf<Long?>(null) }  // Cambiado de specialty a selectedSpecialtyId
    var expanded by remember { mutableStateOf(false) }  // Estado para dropdown
    var message by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val specialties by db.specialtyDao().getAll().collectAsState(initial = emptyList())  // Obtener especialidades

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text("Registrar Especialista", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Usuario") })
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Contraseña") })
        OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("Nombres") })
        OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Apellidos") })
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono") })
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Correo") })

        // Dropdown para especialidades
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = specialties.find { it.id == selectedSpecialtyId }?.name ?: "Seleccionar especialidad",
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
                specialties.forEach { specialty ->
                    DropdownMenuItem(
                        text = { Text(specialty.name) },
                        onClick = {
                            selectedSpecialtyId = specialty.id
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if (selectedSpecialtyId == null) {
                message = "Error: Selecciona una especialidad"
                return@Button
            }
            scope.launch {
                val result = repo.registerSpecialist(
                    username, password, firstName, lastName, phone, email, selectedSpecialtyId!!  // Usa specialtyId
                )
                message = result.fold(
                    onSuccess = { "Especialista registrado correctamente ✅" },
                    onFailure = { "Error: ${it.message}" }
                )
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Registrar")
        }

        message?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Volver")
        }
    }
}