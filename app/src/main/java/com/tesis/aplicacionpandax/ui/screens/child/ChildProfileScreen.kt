package com.tesis.aplicacionpandax.ui.screens.child

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState // Importar para scroll si el contenido crece
import androidx.compose.foundation.verticalScroll // Importar para scroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tesis.aplicacionpandax.data.entity.Child
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildProfileScreen(
    child: Child?,
    onLogout: () -> Unit
) {
    val scrollState = rememberScrollState() // Añadir estado de scroll

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Columna principal que permite scroll
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Aplicar scroll vertical por si el contenido excede la pantalla
                    .verticalScroll(scrollState)
                    // Dejar espacio en la parte inferior para el botón de logout
                    .padding(bottom = 72.dp), // Ajusta este padding según sea necesario
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mensaje si no hay datos del niño
                if (child == null) {
                    Text(
                        "No se encontró tu información.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.CenterHorizontally) // Alinear al centro si es solo este mensaje
                    )
                } else {
                    // --- Card: Información del Perfil Agrupada ---
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // --- Sección: Datos Personales ---
                            Text("Datos Personales", style = MaterialTheme.typography.titleMedium)
                            Divider(modifier = Modifier.padding(vertical = 4.dp)) // Separador visual

                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            val birthDate = Date(child.birthDateMillis)
                            val sexText = when (child.sex) {
                                "M" -> "Masculino"
                                "F" -> "Femenino"
                                else -> child.sex
                            }

                            InfoItem(icon = Icons.Filled.Person, label = "Nombre:", value = "${child.firstName} ${child.lastName}")
                            InfoItem(icon = Icons.Filled.Badge, label = "DNI:", value = child.dni)
                            InfoItem(icon = Icons.Filled.AccessibilityNew, label = "Condición:", value = child.condition)
                            InfoItem(icon = if (child.sex == "M") Icons.Filled.Male else Icons.Filled.Female, label = "Sexo:", value = sexText)
                            InfoItem(icon = Icons.Filled.Cake, label = "Nacimiento:", value = sdf.format(birthDate))

                            Spacer(modifier = Modifier.height(16.dp)) // Espacio antes de la siguiente sección

                            // --- Sección: Datos del Apoderado ---
                            Text("Datos del Apoderado", style = MaterialTheme.typography.titleMedium)
                            Divider(modifier = Modifier.padding(vertical = 4.dp)) // Separador visual

                            InfoItem(icon = Icons.Filled.SupervisorAccount, label = "Nombre:", value = child.guardianName)
                            InfoItem(icon = Icons.Filled.Phone, label = "Teléfono:", value = child.guardianPhone)
                        }
                    } // Fin Card
                } // Fin else (child != null)
            } // Fin Column principal (con scroll)

            // --- Botón Cerrar Sesión (Alineado al fondo del Box) ---
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter), // Alinea al fondo
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text("Cerrar sesión")
            }
        } // Fin Box
    } // Fin Scaffold
}

// Composable auxiliar reutilizado (sin cambios)
@Composable
private fun InfoItem(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}