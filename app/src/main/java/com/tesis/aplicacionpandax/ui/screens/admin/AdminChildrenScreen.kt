package com.tesis.aplicacionpandax.ui.screens.admin

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.dao.ChildWithSpecialist
import com.tesis.aplicacionpandax.repository.AuthRepository
import com.tesis.aplicacionpandax.ui.components.ChildCard
import com.tesis.aplicacionpandax.ui.navigation.NavRoutes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun AdminChildrenScreen(
    db: AppDatabase,
    repo: AuthRepository,
    navController: NavController,
    childrenFlow: Flow<List<ChildWithSpecialist>> // Cambiado a ChildWithSpecialist
) {
    val coroutineScope = rememberCoroutineScope()
    val children by childrenFlow.collectAsState(initial = emptyList())
    var message by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf<ChildWithSpecialist?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Mostrar mensaje en Snackbar
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            message = null
        }
    }

    // Diálogo de confirmación para eliminación
    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { if (!isLoading) showDeleteDialog = null },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Seguro que quieres eliminar a ${showDeleteDialog!!.child.firstName} ${showDeleteDialog!!.child.lastName}?") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            val result = repo.deleteChild(showDeleteDialog!!.child.userId)
                            isLoading = false
                            result.onSuccess {
                                message = "Niño eliminado correctamente ✅"
                            }.onFailure { e ->
                                message = e.message ?: "Error inesperado al eliminar"
                            }
                            showDeleteDialog = null
                        }
                    },
                    enabled = !isLoading
                ) { Text("Eliminar") }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteDialog = null },
                    enabled = !isLoading
                ) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Gestionar Niños",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón para registrar nuevo niño
                    Button(
                        onClick = { navController.navigate(NavRoutes.RegisterChild.route) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        Text("Registrar Nuevo Niño", style = MaterialTheme.typography.labelLarge)
                    }

                    // Subtítulo
                    Text(
                        "Todos los Niños",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Indicador de carga o mensaje si la lista está vacía
                    if (children.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No hay niños registrados",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(children, key = { it.child.userId }) { childWithSpecialist ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn() + slideInVertically(),
                                    exit = fadeOut() + slideOutVertically()
                                ) {
                                    ChildCard(
                                        child = childWithSpecialist.child,
                                        specialistName = childWithSpecialist.specialistName,
                                        onEdit = {
                                            if (childWithSpecialist.child.userId > 0) {
                                                navController.navigate("${NavRoutes.RegisterChild.route}/${childWithSpecialist.child.userId}")
                                            } else {
                                                message = "Error: ID de niño inválido"
                                            }
                                        },
                                        onDetail = {
                                            if (childWithSpecialist.child.userId > 0) {
                                                navController.navigate("child_detail/${childWithSpecialist.child.userId}")
                                            } else {
                                                message = "Error: ID de niño inválido"
                                            }
                                        },
                                        onProgress = {
                                            if (childWithSpecialist.child.userId > 0) {
                                                navController.navigate("child_progress/${childWithSpecialist.child.userId}")
                                            } else {
                                                message = "Error: ID de niño inválido"
                                            }
                                        },
                                        onDelete = { showDeleteDialog = childWithSpecialist },
                                        enabled = !isLoading
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}