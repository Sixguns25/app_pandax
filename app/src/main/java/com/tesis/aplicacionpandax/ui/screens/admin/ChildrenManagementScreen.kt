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
import com.tesis.aplicacionpandax.data.entity.Child
import com.tesis.aplicacionpandax.repository.AuthRepository
import com.tesis.aplicacionpandax.ui.components.ChildCard
import com.tesis.aplicacionpandax.ui.navigation.NavRoutes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ChildrenManagementScreen(
    db: AppDatabase,
    repo: AuthRepository,
    navController: NavController
) {
    val coroutineScope = rememberCoroutineScope()
    val children by db.childDao().getAll().collectAsState(initial = emptyList())
    val specialists by db.specialistDao().getAll().collectAsState(initial = emptyList())
    var message by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf<Child?>(null) }

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
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Seguro que quieres eliminar a ${showDeleteDialog!!.firstName} ${showDeleteDialog!!.lastName}?") },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        repo.deleteChild(showDeleteDialog!!.userId).fold(
                            onSuccess = { message = "Niño eliminado correctamente ✅" },
                            onFailure = { message = "Error: ${it.message}" }
                        )
                    }
                    showDeleteDialog = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Cancelar") }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
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
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Registrar Nuevo Niño", style = MaterialTheme.typography.labelLarge)
                }

                // Subtítulo
                Text(
                    "Niños Existentes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Indicador de carga si la lista está vacía
                if (children.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(children, key = { it.userId }) { child ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + slideInVertically(),
                                exit = fadeOut() + slideOutVertically()
                            ) {
                                ChildCard(
                                    child = child,
                                    specialistName = specialists.find { it.userId == child.specialistId }?.let {
                                        "${it.firstName} ${it.lastName}"
                                    } ?: "No asignado",
                                    onEdit = {
                                        navController.navigate("${NavRoutes.RegisterChild.route}/${child.userId}")
                                    },
                                    onDetail = {
                                        navController.navigate("child_detail/${child.userId}")
                                    },
                                    onDelete = { showDeleteDialog = child },
                                    onProgress = {} // Vacío para admin
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Volver", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}