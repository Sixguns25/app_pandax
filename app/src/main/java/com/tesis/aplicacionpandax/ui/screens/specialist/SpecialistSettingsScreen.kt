package com.tesis.aplicacionpandax.ui.screens.specialist


import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.repository.AuthRepository
import kotlinx.coroutines.launch

@Composable
fun SpecialistSettingsScreen(
    specialistId: Long,
    authRepo: AuthRepository,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    // QUITA: val activity
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- Estados para diálogos ---
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showEditContactDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // --- Estados para Editar Contacto ---
    var currentPhone by remember { mutableStateOf("") }
    var currentEmail by remember { mutableStateOf("") }
    var tempPhone by remember { mutableStateOf("") }
    var tempEmail by remember { mutableStateOf("") }

    // --- Estados para Cambiar Contraseña ---
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Cargar teléfono y email actuales del especialista
    LaunchedEffect(specialistId) {
        if (specialistId != -1L) {
            val db = AppDatabase.getInstance(context, scope)
            scope.launch {
                val specialist = db.specialistDao().getByUserId(specialistId)
                specialist?.let {
                    currentPhone = it.phone
                    currentEmail = it.email
                    if (tempPhone.isEmpty()) tempPhone = it.phone
                    if (tempEmail.isEmpty()) tempEmail = it.email
                }
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Ajustes", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))

            // --- Sección Cuenta ---
            Text("Cuenta", style = MaterialTheme.typography.titleMedium)
            Divider(modifier = Modifier.padding(vertical = 4.dp))

            SettingsItem(
                text = "Editar Información de Contacto",
                icon = Icons.Default.Edit,
                onClick = {
                    tempPhone = currentPhone
                    tempEmail = currentEmail
                    showEditContactDialog = true
                }
            )
            SettingsItem(
                text = "Cambiar Contraseña",
                icon = Icons.Default.LockReset,
                onClick = {
                    currentPassword = ""
                    newPassword = ""
                    confirmPassword = ""
                    showChangePasswordDialog = true
                }
            )
            SettingsItem(
                text = "Cerrar sesión",
                icon = Icons.Default.Logout,
                onClick = onLogout
            )

            Spacer(Modifier.height(16.dp))

            Spacer(Modifier.height(16.dp))

            // --- Sección Información ---
            Text("Información", style = MaterialTheme.typography.titleMedium)
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            SettingsItem(
                text = "Acerca de Pandax",
                icon = Icons.Default.Info,
                onClick = { showAboutDialog = true }
            )
        } // Fin Column principal
    } // Fin Scaffold

    // --- Diálogos ---

    // Diálogo para Editar Contacto (sin cambios)
    if (showEditContactDialog) { /* ... */ }

    // Diálogo para Cambiar Contraseña (sin cambios)
    if (showChangePasswordDialog) { /* ... */ }

    // Diálogo Acerca de (sin cambios)
    if (showAboutDialog) { /* ... */ }
}

// Composable auxiliar SettingsItem (sin cambios)
@Composable
fun SettingsItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge, color = LocalContentColor.current)
        }
    }
}