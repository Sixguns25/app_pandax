package com.tesis.aplicacionpandax.ui.screens.specialist

import androidx.compose.foundation.layout.Column // Importaciones necesarias
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController // <-- IMPORTACIÓN AÑADIDA
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.entity.Child
import com.tesis.aplicacionpandax.data.entity.Specialist // <-- IMPORTACIÓN AÑADIDA
import com.tesis.aplicacionpandax.repository.AuthRepository
import com.tesis.aplicacionpandax.repository.ProgressRepository
import com.tesis.aplicacionpandax.ui.navigation.BottomNavItem
import com.tesis.aplicacionpandax.ui.navigation.NavRoutes
import com.tesis.aplicacionpandax.ui.screens.admin.ChildDetailScreen
import com.tesis.aplicacionpandax.ui.screens.admin.RegisterChildScreen
import kotlinx.coroutines.flow.Flow // <-- IMPORTACIÓN AÑADIDA
import kotlinx.coroutines.launch // <-- IMPORTACIÓN AÑADIDA

@Composable
fun SpecialistHomeScreen(
    specialistId: Long,
    childrenFlow: Flow<List<Child>>,
    progressRepo: ProgressRepository,
    db: AppDatabase,
    authRepo: AuthRepository, // <-- PARÁMETRO AÑADIDO A LA DEFINICIÓN
    onLogout: () -> Unit
) {
    val navController = rememberNavController() // NavController interno para las pestañas

    val items = listOf(
        BottomNavItem("children", "Hijos", Icons.Default.List),
        BottomNavItem("profile", "Perfil", Icons.Default.Person),
        BottomNavItem("settings", "Ajustes", Icons.Default.Settings)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                items.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        // NavHost interno para manejar las pestañas Hijos, Perfil, Ajustes
        NavHost(
            navController = navController,
            startDestination = "children",
            modifier = Modifier.padding(padding)
        ) {
            composable("children") {
                SpecialistChildrenScreen(
                    db = db,
                    repo = authRepo, // Pasa el repo
                    navController = navController,
                    specialistId = specialistId,
                    childrenFlow = childrenFlow
                )
            }
            // CORREGIDO
            composable("profile") {
                SpecialistProfileScreen(
                    specialistId = specialistId,
                    db = db // <-- db añadido
                )
            }
            composable("settings") {
                SpecialistSettingsScreen(
                    specialistId = specialistId,
                    authRepo = authRepo, // Correcto
                    onLogout = onLogout // Correcto
                )
            }

            // --- Definiciones de rutas alcanzables DESDE las pantallas de este NavHost ---
            // CORREGIDO
            composable(
                route = "child_progress/{childId}",
                arguments = listOf(navArgument("childId") { type = NavType.LongType })
            ){ backStackEntry ->
                val childId = backStackEntry.arguments?.getLong("childId") ?: -1
                var childSpecialtyId by remember { mutableStateOf<Long?>(null) }
                LaunchedEffect(childId) {
                    val child = db.childDao().getByUserId(childId)
                    child?.specialistId?.let { specId ->
                        val spec = db.specialistDao().getByUserId(specId)
                        childSpecialtyId = spec?.specialtyId
                    }
                }
                ChildProgressDetailScreen(
                    childUserId = childId,
                    progressRepo = progressRepo,
                    db = db, // <-- db añadido
                    navController = navController, // <-- navController añadido
                    specialtyId = childSpecialtyId // <-- specialtyId añadido
                )
            }
            // CORREGIDO
            composable(NavRoutes.SpecialistRegisterChild.route) {
                val specialists by db.specialistDao().getAll().collectAsState(initial = emptyList())
                RegisterChildScreen(
                    repo = authRepo,
                    specialists = specialists, // <-- specialists añadido
                    onBack = { navController.popBackStack() },
                    specialistId = specialistId,
                    db = db // <-- db añadido
                )
            }
            // CORREGIDO
            composable(
                route = "${NavRoutes.SpecialistRegisterChild.route}/{childId}",
                arguments = listOf(navArgument("childId") { type = NavType.LongType })
            ) { backStackEntry ->
                val childId = backStackEntry.arguments?.getLong("childId") ?: -1L
                val specialists by db.specialistDao().getAll().collectAsState(initial = emptyList())
                RegisterChildScreen(
                    repo = authRepo,
                    specialists = specialists, // <-- specialists añadido
                    onBack = { navController.popBackStack() },
                    specialistId = specialistId,
                    childId = childId,
                    db = db // <-- db añadido
                )
            }
            composable(
                route = "child_detail/{childId}",
                arguments = listOf(navArgument("childId") { type = NavType.LongType })
            ) { backStackEntry ->
                val childId = backStackEntry.arguments?.getLong("childId") ?: -1L
                ChildDetailScreen(
                    navController = navController,
                    db = db,
                    childId = childId
                )
            }
        } // Fin NavHost interno
    } // Fin Scaffold
}