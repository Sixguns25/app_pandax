package com.tesis.aplicacionpandax.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
// Importa los iconos específicos que usarás
import androidx.compose.material.icons.filled.Category // Para especialidades
import androidx.compose.material.icons.filled.EscalatorWarning // Ícono ejemplo para niño
import androidx.compose.material.icons.filled.Logout // Para cerrar sesión
import androidx.compose.material.icons.filled.People // Para gestionar niños
import androidx.compose.material.icons.filled.PersonAdd // Para registrar especialista
import androidx.compose.material.icons.filled.SupervisedUserCircle // Para gestionar especialistas
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Data class para mantener la información de cada acción del menú
data class AdminActionItem(
    val title: String,
    val icon: ImageVector,
    val action: () -> Unit // La función lambda a ejecutar al hacer clic
)

@OptIn(ExperimentalMaterial3Api::class) // Necesario para Card onClick y TopAppBarDefaults
@Composable
fun AdminHomeScreen(
    onRegisterSpecialist: () -> Unit,
    onRegisterChild: () -> Unit,
    onManageSpecialties: () -> Unit,
    onManageSpecialists: () -> Unit,
    onManageChildren: () -> Unit,
    onLogout: () -> Unit
) {
    // Define la lista de acciones con sus títulos, iconos y funciones asociadas
    val adminActions = listOf(
        AdminActionItem("Registrar\nEspecialista", Icons.Filled.PersonAdd, onRegisterSpecialist),
        AdminActionItem("Registrar\nNiño", Icons.Filled.EscalatorWarning, onRegisterChild), // Puedes buscar un icono más adecuado
        AdminActionItem("Gestionar\nEspecialidades", Icons.Filled.Category, onManageSpecialties),
        AdminActionItem("Gestionar\nEspecialistas", Icons.Filled.SupervisedUserCircle, onManageSpecialists),
        AdminActionItem("Gestionar\nNiños", Icons.Filled.People, onManageChildren),
        AdminActionItem("Cerrar\nSesión", Icons.Filled.Logout, onLogout)
    )

    Scaffold(
        topBar = {
            // Barra superior consistente con otras pantallas
            TopAppBar(
                title = { Text("Panel de Administrador") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues -> // El contenido principal usa el padding proporcionado por Scaffold
        // Cuadrícula vertical perezosa para mostrar las tarjetas
        LazyVerticalGrid(
            columns = GridCells.Fixed(2), // Define que habrá 2 columnas
            modifier = Modifier
                .padding(paddingValues) // Aplica el padding de Scaffold
                .padding(16.dp), // Padding adicional alrededor de toda la cuadrícula
            verticalArrangement = Arrangement.spacedBy(16.dp), // Espacio vertical entre tarjetas
            horizontalArrangement = Arrangement.spacedBy(16.dp) // Espacio horizontal entre tarjetas
        ) {
            // Itera sobre la lista de acciones y crea una tarjeta para cada una
            items(adminActions) { item ->
                AdminActionCard(item = item)
            }
        }
    }
}

// Composable reutilizable para cada tarjeta de acción en la cuadrícula
@OptIn(ExperimentalMaterial3Api::class) // Necesario para Card onClick
@Composable
fun AdminActionCard(item: AdminActionItem) {
    Card(
        onClick = item.action, // La tarjeta completa es clickeable y ejecuta la acción
        modifier = Modifier
            .aspectRatio(1f) // Hace que la tarjeta sea cuadrada (ancho = alto)
            .padding(4.dp), // Pequeño padding alrededor de la tarjeta misma
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), // Sombra ligera
        shape = MaterialTheme.shapes.medium // Usa la forma media definida en el tema (usualmente esquinas redondeadas)
    ) {
        // Columna para organizar el icono y el texto dentro de la tarjeta
        Column(
            modifier = Modifier
                .fillMaxSize() // Ocupa todo el espacio de la tarjeta
                .padding(16.dp), // Padding interno para el contenido
            horizontalAlignment = Alignment.CenterHorizontally, // Centra el icono y texto horizontalmente
            verticalArrangement = Arrangement.Center // Centra el icono y texto verticalmente
        ) {
            // Icono representativo de la acción
            Icon(
                imageVector = item.icon,
                contentDescription = item.title, // Descripción para accesibilidad
                modifier = Modifier.size(48.dp), // Tamaño del icono
                tint = MaterialTheme.colorScheme.primary // Color del icono
            )
            Spacer(modifier = Modifier.height(12.dp)) // Espacio entre icono y texto
            // Texto descriptivo de la acción
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), // Estilo del texto
                textAlign = TextAlign.Center // Centra el texto (útil si ocupa más de una línea)
            )
        }
    }
}