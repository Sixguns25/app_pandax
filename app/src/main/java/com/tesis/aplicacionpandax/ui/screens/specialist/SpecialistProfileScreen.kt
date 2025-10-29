package com.tesis.aplicacionpandax.ui.screens.specialist

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons // Importar Icons
import androidx.compose.material.icons.filled.* // Importar iconos específicos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector // Para iconos en ProfileItem
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.entity.Specialist
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class) // Para Scaffold, TopAppBar, Card
@Composable
fun SpecialistProfileScreen(
    specialistId: Long,
    db: AppDatabase // Asegúrate de que este parámetro se pasa desde SpecialistHomeScreen
    // Quitamos authRepo si no se usa directamente aquí
) {
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
        // No creamos instancia de DB aquí, usamos la que se pasa como parámetro 'db'
        scope.launch {
            specialist = db.specialistDao().getByUserId(specialistId)
            val user = db.userDao().getById(specialistId)
            username = user?.username ?: "No encontrado"
            val specialty = db.specialtyDao().getById(specialist?.specialtyId ?: -1)
            specialtyName = specialty?.name ?: "No asignada"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                // Sin navigationIcon porque es parte del BottomNav
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->

        Box( // Usamos Box para centrar el indicador o el mensaje de error
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Aplica padding de Scaffold
                .padding(16.dp) // Padding interno
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                specialist == null -> {
                    Text(
                        "No se pudo cargar la información del perfil.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    // Muestra la Card con la información una vez cargada
                    Card(
                        modifier = Modifier.fillMaxWidth(), // Ocupa el ancho
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Sombra ligera
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp) // Espacio entre ítems
                        ) {
                            // Usamos el ProfileItem mejorado con iconos
                            ProfileItem(Icons.Filled.AccountCircle, "Usuario:", username)
                            ProfileItem(Icons.Filled.Person, "Nombres:", specialist!!.firstName)
                            ProfileItem(Icons.Filled.PersonOutline, "Apellidos:", specialist!!.lastName)
                            ProfileItem(Icons.Filled.Phone, "Teléfono:", specialist!!.phone)
                            ProfileItem(Icons.Filled.Email, "Correo:", specialist!!.email)
                            ProfileItem(Icons.Filled.Star, "Especialidad:", specialtyName) // O Icons.Filled.Work
                        }
                    }
                } // Fin else (datos cargados)
            } // Fin when
        } // Fin Box
    } // Fin Scaffold
}

// Composable auxiliar ProfileItem mejorado con Icono
@Composable
private fun ProfileItem(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically // Alinea icono y texto verticalmente
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // Descripción opcional si es decorativo
            modifier = Modifier.size(24.dp), // Tamaño del icono
            tint = MaterialTheme.colorScheme.primary // Color del icono
        )
        Spacer(modifier = Modifier.width(16.dp)) // Espacio entre icono y texto
        Column { // Columna para etiqueta y valor si fueran multilínea
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium, // Etiqueta más pequeña
                color = MaterialTheme.colorScheme.onSurfaceVariant // Color secundario
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge // Valor principal
            )
        }
    }
}