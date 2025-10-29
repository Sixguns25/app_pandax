package com.tesis.aplicacionpandax.ui.screens.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color // Importa Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.tesis.aplicacionpandax.R
import com.tesis.aplicacionpandax.data.entity.User
import com.tesis.aplicacionpandax.ui.viewmodel.AuthViewModel
import androidx.compose.animation.AnimatedVisibility // Importa si falta

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: (User) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    val loginState by viewModel.loginState.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.login_background), // Reemplaza con tu imagen
            contentDescription = "Fondo de inicio de sesión",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(20.dp)) // Esquinas un poco más redondeadas
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)) // Ligeramente menos transparente
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Image(
                painter = painterResource(id = R.drawable.logo_pandax), // Reemplaza con tu logo
                contentDescription = "Logo Pandax",
                modifier = Modifier
                    .size(200.dp)
                    .padding(bottom = 24.dp)
            )

            // --- Campos de Texto Personalizados ---
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Usuario") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp), // Esquinas más suaves
                colors = OutlinedTextFieldDefaults.colors( // Personaliza colores
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    // Puedes añadir focusedContainerColor, unfocusedContainerColor si quieres un fondo
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp), // Misma forma que el de usuario
                colors = OutlinedTextFieldDefaults.colors( // Mismos colores
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(24.dp))

            // --- Botón Personalizado ---
            Button(
                onClick = { viewModel.login(username.trim(), password) },
                modifier = Modifier.fillMaxWidth().height(50.dp), // Un poco más alto
                shape = RoundedCornerShape(12.dp), // Misma forma que los campos
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary, // Color principal de tu tema
                    contentColor = MaterialTheme.colorScheme.onPrimary // Color del texto sobre el primario
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp) // Sombra ligera
            ) {
                Text("Entrar", style = MaterialTheme.typography.labelLarge) // Texto un poco más grande
            }

            // --- Mensaje de Error (sin cambios visuales directos aquí) ---
            var errorMessage by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(loginState) {
                loginState?.onFailure { e ->
                    errorMessage = "Error: ${e.message}"
                }?.onSuccess { user ->
                    errorMessage = null
                    onLoginSuccess(user)
                    viewModel.clearState()
                }
            }
            AnimatedVisibility(visible = errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        } // Fin Column (contenido login)
    } // Fin Box (contenedor principal)
}