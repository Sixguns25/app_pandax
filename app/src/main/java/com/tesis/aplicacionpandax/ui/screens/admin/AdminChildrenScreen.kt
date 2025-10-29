package com.tesis.aplicacionpandax.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape // Para FAB y botones
import androidx.compose.material.icons.Icons // Para iconos
import androidx.compose.material.icons.filled.Add // Icono para FAB
import androidx.compose.material.icons.filled.ArrowBack // Icono para volver
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.dao.ChildWithSpecialist // Mantenemos este
import com.tesis.aplicacionpandax.repository.AuthRepository
import com.tesis.aplicacionpandax.ui.components.ChildCard // Usamos el componente existente
import com.tesis.aplicacionpandax.ui.navigation.NavRoutes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class) // Mantenido por Card y TopAppBar
@Composable
fun AdminChildrenScreen(
    db: AppDatabase, // Necesario si ChildCard necesita más datos o para futuras expansiones
    repo: AuthRepository,
    navController: NavController,
    childrenFlow: Flow<List<ChildWithSpecialist>> // Flujo que ya une niño y especialista
) {
    val coroutineScope = rememberCoroutineScope()
    // Estado para carga inicial
    var isInitialLoading by remember { mutableStateOf(true) }
    val children by childrenFlow.collectAsState(initial = emptyList())

    // Actualiza estado de carga inicial
    LaunchedEffect(children) {
        if (isInitialLoading) {
            isInitialLoading = false
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf<ChildWithSpecialist?>(null) }
    var isDeleting by remember { mutableStateOf(false) } // Estado específico para borrado

    // Diálogo de confirmación para eliminación (Mejorado)
    if (showDeleteDialog != null) {
        val childToDelete = showDeleteDialog!!.child // Extrae el Child
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteDialog = null },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Seguro que quieres eliminar a ${childToDelete.firstName} ${childToDelete.lastName}? Se eliminarán el usuario y el perfil del niño. Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        coroutineScope.launch {
                            val result = repo.deleteChild(childToDelete.userId)
                            isDeleting = false
                            result.onSuccess {
                                snackbarHostState.showSnackbar("Niño eliminado correctamente ✅")
                            }.onFailure { e ->
                                // Mostrar mensaje de error más específico si viene del repo
                                snackbarHostState.showSnackbar(e.message ?: "Error inesperado al eliminar ❌")
                            }
                            showDeleteDialog = null // Cierra el diálogo independientemente del resultado
                        }
                    },
                    enabled = !isDeleting,
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
                    enabled = !isDeleting
                ) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Gestionar Niños") }, // Título más conciso
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { // Botón para volver
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        // FAB para añadir nuevo niño
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(NavRoutes.RegisterChild.route) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Registrar Nuevo Niño")
            }
        }
    ) { paddingValues ->
        Box( // Usamos Box para superponer el indicador de carga si es necesario
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Aplica padding de Scaffold
        ) {
            when {
                // Estado de carga inicial
                isInitialLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                // Estado de lista vacía
                children.isEmpty() -> {
                    Text(
                        "No hay niños registrados. Presiona '+' para añadir uno.",
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp), // Padding para centrar texto
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                // Estado con datos
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp), // Padding lateral para la lista
                        verticalArrangement = Arrangement.spacedBy(12.dp), // Espacio entre ChildCards
                        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp) // Espacio arriba/abajo (para FAB)
                    ) {
                        items(children, key = { it.child.userId }) { childWithSpecialist ->
                            // Muestra ChildCard directamente, sin AnimatedVisibility
                            ChildCard(
                                child = childWithSpecialist.child,
                                specialistName = childWithSpecialist.specialistName, // Nombre ya viene formateado
                                onEdit = {
                                    // Navega a la pantalla de edición de niño (Admin)
                                    navController.navigate("${NavRoutes.RegisterChild.route}/${childWithSpecialist.child.userId}")
                                },
                                onDetail = {
                                    // Navega a la pantalla de detalles
                                    navController.navigate("child_detail/${childWithSpecialist.child.userId}")
                                },
                                onProgress = {
                                    // Navega a la pantalla de progreso
                                    navController.navigate("child_progress/${childWithSpecialist.child.userId}")
                                },
                                onDelete = { showDeleteDialog = childWithSpecialist }, // Abre el diálogo
                                enabled = !isDeleting // Deshabilita acciones si se está borrando algo
                            )
                        }
                    } // Fin LazyColumn
                } // Fin else (lista con datos)
            } // Fin when
        } // Fin Box
    } // Fin Scaffold
}

// Nota: El composable ChildCard se asume que está definido en ui/components/ChildCard.kt