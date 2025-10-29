package com.tesis.aplicacionpandax.ui.screens.specialist

// QUITA: import androidx.compose.animation.* // Ya no se necesita
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape // Para FAB
import androidx.compose.material.icons.Icons // Para iconos
import androidx.compose.material.icons.filled.Add // Icono para FAB
// QUITA: import androidx.compose.material.icons.filled.ArrowBack // No se necesita aquí, es parte del BottomNav
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.entity.Child
import com.tesis.aplicacionpandax.repository.AuthRepository
import com.tesis.aplicacionpandax.ui.components.ChildCard // Reutilizamos ChildCard
import com.tesis.aplicacionpandax.ui.navigation.NavRoutes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class) // Mantenido por Card, TopAppBar, FAB
@Composable
fun SpecialistChildrenScreen(
    db: AppDatabase, // Puede ser útil para ChildCard o futuras expansiones
    repo: AuthRepository,
    navController: NavController,
    specialistId: Long, // ID del especialista actual
    childrenFlow: Flow<List<Child>> // Flujo de niños asignados a este especialista
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
    var showDeleteDialog by remember { mutableStateOf<Child?>(null) }
    var isDeleting by remember { mutableStateOf(false) } // Estado específico para borrado

    // Diálogo de confirmación para eliminación (Mejorado)
    if (showDeleteDialog != null) {
        val childToDelete = showDeleteDialog!! // Captura el valor no nulo
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
                                snackbarHostState.showSnackbar(e.message ?: "Error inesperado al eliminar ❌")
                            }
                            showDeleteDialog = null // Cierra el diálogo
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
                title = { Text("Mis Niños Asignados") }, // Título claro
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                // No ponemos NavigationIcon aquí porque esta pantalla es parte del Bottom Navigation
            )
        },
        // FAB para añadir nuevo niño (asociado a este especialista)
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(NavRoutes.SpecialistRegisterChild.route) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Registrar Nuevo Niño")
            }
        }
    ) { paddingValues ->
        Box( // Usamos Box para superponer el indicador de carga
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
                        "No tienes niños asignados. Presiona '+' para registrar uno.",
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                // Estado con datos
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp), // Padding lateral
                        verticalArrangement = Arrangement.spacedBy(12.dp), // Espacio entre cards
                        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp) // Espacio arriba/abajo (para FAB)
                    ) {
                        items(children, key = { it.userId }) { child ->
                            // Muestra ChildCard directamente
                            ChildCard(
                                child = child,
                                specialistName = "Asignado a mí", // Texto fijo ya que son *sus* niños
                                onEdit = {
                                    // Navega a la pantalla de edición (flujo especialista)
                                    navController.navigate("${NavRoutes.SpecialistRegisterChild.route}/${child.userId}")
                                },
                                onDetail = {
                                    // Navega a la pantalla de detalles común
                                    navController.navigate("child_detail/${child.userId}")
                                },
                                onProgress = {
                                    // Navega a la pantalla de progreso común
                                    navController.navigate("child_progress/${child.userId}")
                                },
                                onDelete = { showDeleteDialog = child }, // Abre el diálogo
                                enabled = !isDeleting // Deshabilita acciones si se está borrando
                            )
                        }
                    } // Fin LazyColumn
                } // Fin else (lista con datos)
            } // Fin when
        } // Fin Box
    } // Fin Scaffold
}

// Nota: El composable ChildCard se asume que está definido en ui/components/ChildCard.kt
// y que ya tiene un buen diseño visual.