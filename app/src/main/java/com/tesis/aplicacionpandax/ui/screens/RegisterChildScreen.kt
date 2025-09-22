package com.tesis.aplicacionpandax.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tesis.aplicacionpandax.data.entity.Specialist
import com.tesis.aplicacionpandax.repository.AuthRepository
import kotlinx.coroutines.launch

@Composable
fun RegisterChildScreen(
    repo: AuthRepository,
    specialists: List<Specialist>,
    onBack: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var dni by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") }
    var birthDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var guardianName by remember { mutableStateOf("") }
    var guardianPhone by remember { mutableStateOf("") }
    var selectedSpecialistId by remember { mutableStateOf<Long?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text("Registrar Niño", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Usuario") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Contraseña") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("Nombres") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Apellidos") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = dni, onValueChange = { dni = it }, label = { Text("DNI") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = condition, onValueChange = { condition = it }, label = { Text("Condición") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = sex, onValueChange = { sex = it }, label = { Text("Sexo") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = guardianName, onValueChange = { guardianName = it }, label = { Text("Nombre Apoderado") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = guardianPhone, onValueChange = { guardianPhone = it }, label = { Text("Teléfono Apoderado") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(16.dp))
        Text("Seleccionar Especialista:")

        // Texto para mostrar el especialista seleccionado
        val selectedSpecialistName = selectedSpecialistId
            ?.let { id -> specialists.find { it.userId == id } }
            ?.let { "${it.firstName} ${it.lastName}" }

        // Dropdown de especialistas (botón que abre menú)
        Box(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selectedSpecialistName ?: "Elegir especialista")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (specialists.isEmpty()) {
                    DropdownMenuItem(text = { Text("No hay especialistas") }, onClick = { expanded = false })
                } else {
                    specialists.forEach { sp ->
                        DropdownMenuItem(
                            text = { Text("${sp.firstName} ${sp.lastName}") },
                            onClick = {
                                selectedSpecialistId = sp.userId
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            scope.launch {
                val result = repo.registerChild(
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
                message = result.fold(
                    onSuccess = { "Niño registrado correctamente ✅" },
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
