package com.tesis.aplicacionpandax.ui.screens.specialist

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.entity.Specialist
import kotlinx.coroutines.launch

@Composable
fun SpecialistProfileScreen(specialistId: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var specialist by remember { mutableStateOf<Specialist?>(null) }
    var username by remember { mutableStateOf("...") }
    var specialtyName by remember { mutableStateOf("...") }
    var isLoading by remember { mutableStateOf(true) }

    // Cargar datos del especialista
    LaunchedEffect(specialistId) {
        if (specialistId == -1L) {
            isLoading = false // No hay ID válido
            return@LaunchedEffect
        }
        isLoading = true
        val db = AppDatabase.getInstance(context, scope)
        scope.launch {
            specialist = db.specialistDao().getByUserId(specialistId)
            val user = db.userDao().getById(specialistId)
            username = user?.username ?: "No encontrado"
            val specialty = db.specialtyDao().getById(specialist?.specialtyId ?: -1)
            specialtyName = specialty?.name ?: "No asignada"
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Mi Perfil", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else if (specialist == null) {
            Text("No se pudo cargar la información del perfil.")
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileItem("Usuario:", username)
                    ProfileItem("Nombres:", specialist!!.firstName)
                    ProfileItem("Apellidos:", specialist!!.lastName)
                    ProfileItem("Teléfono:", specialist!!.phone)
                    ProfileItem("Correo:", specialist!!.email)
                    ProfileItem("Especialidad:", specialtyName)
                }
            }
        }
    }
}

@Composable
private fun ProfileItem(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.width(120.dp) // Ancho fijo para alinear
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}