package com.tesis.aplicacionpandax.ui.screens.specialist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit // Icono para editar
import androidx.compose.material.icons.filled.Info // Icono para acerca de
import androidx.compose.material.icons.filled.LockReset // Icono para cambiar contraseña
import androidx.compose.material.icons.filled.Logout // Icono para cerrar sesión
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType // Para tipos de teclado
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.repository.AuthRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class) // Necesario para Scaffold, TopAppBar, etc.
@Composable
fun SpecialistSettingsScreen(
    specialistId: Long, // ID del especialista logueado
    authRepo: AuthRepository, // Repositorio para acciones de cuenta
    onLogout: () -> Unit // Función para cerrar sesión
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- Estados para los diálogos ---
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showEditContactDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // --- Estados para Editar Contacto ---
    var currentPhone by remember { mutableStateOf("") }
    var currentEmail by remember { mutableStateOf("") }
    var tempPhone by remember { mutableStateOf("") } // Estado temporal para el diálogo
    var tempEmail by remember { mutableStateOf("") } // Estado temporal para el diálogo

    // --- Estados para Cambiar Contraseña ---
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisibleCurrent by remember { mutableStateOf(false) } // Visibilidad contraseñas
    var isPasswordVisibleNew by remember { mutableStateOf(false) }
    var isPasswordVisibleConfirm by remember { mutableStateOf(false) }


    // --- Estado de Carga (para operaciones en diálogos) ---
    var isSaving by remember { mutableStateOf(false) } // Para indicar progreso en diálogos

    // Cargar teléfono y email actuales del especialista al iniciar
    LaunchedEffect(specialistId) {
        if (specialistId != -1L) {
            // Usa la instancia de DB directamente si es necesario, o mejor aún,
            // considera añadir una función al repo para obtener los detalles del especialista
            val db = AppDatabase.getInstance(context, scope) // Obtiene instancia localmente si es necesario
            scope.launch {
                val specialist = db.specialistDao().getByUserId(specialistId)
                specialist?.let {
                    currentPhone = it.phone
                    currentEmail = it.email
                    // Inicializa los temporales la primera vez
                    if (tempPhone.isEmpty()) tempPhone = it.phone
                    if (tempEmail.isEmpty()) tempEmail = it.email
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                // Sin navigationIcon ya que es parte del BottomNav
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues) // Aplica padding del Scaffold
                .padding(16.dp), // Padding interno adicional
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // --- Sección Cuenta ---
            Text("Cuenta", style = MaterialTheme.typography.titleMedium)
            Divider(modifier = Modifier.padding(vertical = 4.dp))

            SettingsItem(
                text = "Editar Información de Contacto",
                icon = Icons.Default.Edit,
                onClick = {
                    // Asegura que los temporales reflejen los datos actuales antes de abrir
                    tempPhone = currentPhone
                    tempEmail = currentEmail
                    showEditContactDialog = true
                }
            )
            SettingsItem(
                text = "Cambiar Contraseña",
                icon = Icons.Default.LockReset,
                onClick = {
                    // Resetea los campos de contraseña al abrir el diálogo
                    currentPassword = ""
                    newPassword = ""
                    confirmPassword = ""
                    isPasswordVisibleCurrent = false
                    isPasswordVisibleNew = false
                    isPasswordVisibleConfirm = false
                    showChangePasswordDialog = true
                }
            )
            SettingsItem(
                text = "Cerrar sesión",
                icon = Icons.Default.Logout,
                onClick = onLogout // Llama a la función recibida
            )

            Spacer(Modifier.height(16.dp)) // Espacio entre secciones

            // --- Sección Información ---
            Text("Información", style = MaterialTheme.typography.titleMedium)
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            SettingsItem(
                text = "Acerca de Pandax",
                icon = Icons.Default.Info,
                onClick = { showAboutDialog = true }
            )

            // Puedes añadir más secciones o items aquí...

        } // Fin Column principal
    } // Fin Scaffold

    // --- Diálogos ---

    // Diálogo para Editar Contacto
    if (showEditContactDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) showEditContactDialog = false }, // No cerrar si está guardando
            title = { Text("Editar Contacto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tempPhone,
                        onValueChange = { tempPhone = it },
                        label = { Text("Nuevo Teléfono") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    OutlinedTextField(
                        value = tempEmail,
                        onValueChange = { tempEmail = it },
                        label = { Text("Nuevo Correo") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSaving = true // Inicia estado de guardado
                        scope.launch {
                            val result = authRepo.updateSpecialistContact(specialistId, tempPhone, tempEmail)
                            isSaving = false // Termina estado de guardado
                            result.onSuccess {
                                currentPhone = tempPhone // Actualiza estado local si tiene éxito
                                currentEmail = tempEmail
                                snackbarHostState.showSnackbar("Contacto actualizado correctamente ✅")
                                showEditContactDialog = false
                            }.onFailure {
                                snackbarHostState.showSnackbar("Error: ${it.message} ❌")
                            }
                        }
                    },
                    enabled = !isSaving // Deshabilita mientras guarda
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Guardar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditContactDialog = false },
                    enabled = !isSaving // Deshabilita mientras guarda
                ) { Text("Cancelar") }
            }
        )
    }

    // Diálogo para Cambiar Contraseña
    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) showChangePasswordDialog = false },
            title = { Text("Cambiar Contraseña") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Contraseña Actual
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Contraseña Actual") },
                        visualTransformation = if (isPasswordVisibleCurrent) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = { IconButton(onClick = { isPasswordVisibleCurrent = !isPasswordVisibleCurrent }) { Icon( if (isPasswordVisibleCurrent) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null ) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true
                    )
                    // Nueva Contraseña
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Nueva Contraseña") },
                        visualTransformation = if (isPasswordVisibleNew) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = { IconButton(onClick = { isPasswordVisibleNew = !isPasswordVisibleNew }) { Icon( if (isPasswordVisibleNew) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null ) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true
                    )
                    // Confirmar Nueva Contraseña
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirmar Nueva Contraseña") },
                        visualTransformation = if (isPasswordVisibleConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = { IconButton(onClick = { isPasswordVisibleConfirm = !isPasswordVisibleConfirm }) { Icon( if (isPasswordVisibleConfirm) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null ) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword != confirmPassword) {
                            scope.launch { snackbarHostState.showSnackbar("Las nuevas contraseñas no coinciden ❌") }
                            return@Button
                        }
                        if (newPassword.length < 6) { // Validación añadida aquí
                            scope.launch { snackbarHostState.showSnackbar("La nueva contraseña debe tener al menos 6 caracteres ❌") }
                            return@Button
                        }
                        isSaving = true // Inicia estado de guardado
                        scope.launch {
                            val result = authRepo.changePassword(specialistId, currentPassword, newPassword)
                            isSaving = false // Termina estado de guardado
                            result.onSuccess {
                                snackbarHostState.showSnackbar("Contraseña actualizada correctamente ✅")
                                showChangePasswordDialog = false
                            }.onFailure {
                                snackbarHostState.showSnackbar("Error: ${it.message} ❌")
                            }
                        }
                    },
                    enabled = !isSaving // Deshabilita mientras guarda
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Guardar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showChangePasswordDialog = false },
                    enabled = !isSaving // Deshabilita mientras guarda
                ) { Text("Cancelar") }
            }
        )
    }

    // Diálogo Acerca de
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Acerca de Pandax") },
            text = { Text("Versión 1.0\nDesarrollado por:\nCortez Díaz Julio César\nEspino Roncal Junior Alexis\nPeña Chávez Kevin Jhoel\n\nApp de soporte para tesis 'Implementación de un sistema interactivo multimedia para el soporte del aprendizaje en niños con trastornos del neurodesarrollo'.") },
            confirmButton = { Button(onClick = { showAboutDialog = false }) { Text("Cerrar") } }
        )
    }
}

// Composable auxiliar reutilizable para los items de ajuste
@Composable
fun SettingsItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp) // Padding interno
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge, color = LocalContentColor.current) // Usa color de contenido local
        }
    }
}