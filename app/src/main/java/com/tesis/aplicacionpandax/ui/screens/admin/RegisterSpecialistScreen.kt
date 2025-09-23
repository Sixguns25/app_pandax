package com.tesis.aplicacionpandax.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tesis.aplicacionpandax.repository.AuthRepository
import kotlinx.coroutines.launch

@Composable
fun RegisterSpecialistScreen(
    repo: AuthRepository,
    onBack: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var specialty by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

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
        OutlinedTextField(value = specialty, onValueChange = { specialty = it }, label = { Text("Especialidad") })

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            scope.launch {
                val result = repo.registerSpecialist(
                    username, password, firstName, lastName, phone, email, specialty
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
