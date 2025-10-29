package com.tesis.aplicacionpandax.ui.screens.admin

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add // Icono para añadir
import androidx.compose.material.icons.filled.ArrowBack // Icono para volver
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.entity.Specialist
import com.tesis.aplicacionpandax.repository.AuthRepository
import com.tesis.aplicacionpandax.ui.navigation.NavRoutes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun SpecialistsManagementScreen(
    db: AppDatabase,
    repo: AuthRepository,
    navController: NavController
) {
    val coroutineScope = rememberCoroutineScope()
    // Estado para saber si los datos iniciales están cargando
    var isInitialLoading by remember { mutableStateOf(true) }
    val specialists by db.specialistDao().getAll().collectAsState(initial = emptyList())
    val specialties by db.specialtyDao().getAll().collectAsState(initial = emptyList())

    // Actualiza isInitialLoading solo una vez que se recibe la primera lista (incluso si está vacía)
    LaunchedEffect(specialists) {
        if (isInitialLoading) {
            isInitialLoading = false
        }
    }

    var message by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf<Specialist?>(null) } // Para diálogo de confirmación
    var isDeleting by remember { mutableStateOf(false) } // Estado para operación de borrado

    // Mostrar mensaje en Snackbar
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            message = null // Limpia el mensaje después de mostrarlo
        }
    }

    // Diálogo de confirmación para eliminación
    if (showDeleteDialog != null) {
        val specialistToDelete = showDeleteDialog!! // Captura el valor no nulo
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = null }, // Permite cerrar si no está borrando
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Seguro que quieres eliminar a ${specialistToDelete.firstName} ${specialistToDelete.lastName}? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true // Inicia estado de borrado
                        coroutineScope.launch {
                            val result = repo.deleteSpecialist(specialistToDelete.userId)
                            isDeleting = false // Termina estado de borrado
                            result.onSuccess { message = "Especialista eliminado correctamente ✅" }
                                .onFailure { errMsg -> message = "Error: ${errMsg.message} ❌" }
                            showDeleteDialog = null // Cierra el diálogo
                        }
                    },
                    enabled = !isDeleting, // Deshabilita mientras borra
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onError)
                    } else {
                        Text("Eliminar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = null },
                    enabled = !isDeleting // Deshabilita mientras borra
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Gestionar Especialistas") }, // Título más conciso
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                // Botón para volver atrás
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        // FloatingActionButton para añadir nuevo especialista
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(NavRoutes.RegisterSpecialist.route) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Registrar Nuevo Especialista")
            }
        }
    ) { paddingValues -> // Renombrado a paddingValues para claridad
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Aplica padding de Scaffold
                .padding(horizontal = 16.dp), // Padding horizontal para el contenido
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp)) // Espacio inicial

            // Título de la sección (opcional, ya está en TopAppBar)
            // Text("Especialistas Existentes", ...)

            // Manejo de estado de carga inicial y lista vacía
            Box(modifier = Modifier.fillMaxSize()) { // Ocupa todo el espacio disponible
                when {
                    isInitialLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    specialists.isEmpty() -> {
                        Text(
                            "No hay especialistas registrados.",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    else -> {
                        // Lista de especialistas
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp), // Más espacio entre cards
                            contentPadding = PaddingValues(bottom = 80.dp) // Espacio abajo para no solapar con FAB
                        ) {
                            items(specialists, key = { it.userId }) { specialist ->
                                // 👇 AnimatedVisibility ELIMINADO de aquí
                                SpecialistCard( // Muestra la tarjeta directamente
                                    specialist = specialist,
                                    specialtyName = specialties.find { it.id == specialist.specialtyId }?.name ?: "Sin especialidad",
                                    onEdit = {
                                        navController.navigate("${NavRoutes.RegisterSpecialist.route}/${specialist.userId}")
                                    },
                                    onDetail = {
                                        navController.navigate("specialist_detail/${specialist.userId}")
                                    },
                                    // Ahora abre el diálogo de confirmación
                                    onDelete = { showDeleteDialog = specialist }
                                )
                                // AnimatedVisibility } // <-- Línea eliminada
                            }
                        } // Fin LazyColumn
                    } // Fin else (lista no vacía)
                } // Fin when
            } // Fin Box
        } // Fin Column principal
        // QUITA: El bottomBar con el botón "Volver"
    } // Fin Scaffold
}

// --- Composable SpecialistCard (Refinado) ---
@Composable
fun SpecialistCard(
    specialist: Specialist,
    specialtyName: String,
    onEdit: () -> Unit,
    onDetail: () -> Unit,
    onDelete: () -> Unit // Ahora abre el diálogo
) {
    Card(
        modifier = Modifier.fillMaxWidth(), // Ocupa todo el ancho
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp) // Consistencia
        // No necesita clip si se aplica shape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp), // Padding interno
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Información del especialista a la izquierda
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) { // Ocupa espacio flexible
                Text(
                    "${specialist.firstName} ${specialist.lastName}",
                    style = MaterialTheme.typography.titleMedium, // Un poco más grande
                    fontWeight = FontWeight.Bold
                )
                Text(
                    specialtyName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f) // Color secundario
                )
            }
            // Botones de acción a la derecha
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { // Menos espacio entre botones
                // IconButton para Editar (más compacto)
                IconButton(onClick = onEdit) {
                    Icon( Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary )
                }
                // IconButton para Detalles (más compacto)
                IconButton(onClick = onDetail) {
                    Icon( Icons.Default.Info, contentDescription = "Detalles", tint = MaterialTheme.colorScheme.secondary )
                }
                // IconButton para Eliminar (más compacto)
                IconButton(onClick = onDelete) { // Llama a la acción que abre el diálogo
                    Icon( Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error )
                }
            }
        }
    }
}