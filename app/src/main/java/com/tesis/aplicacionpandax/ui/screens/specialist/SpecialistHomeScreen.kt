package com.tesis.aplicacionpandax.ui.screens.specialist

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.entity.Child
import com.tesis.aplicacionpandax.repository.AuthRepository
import com.tesis.aplicacionpandax.repository.ProgressRepository
import com.tesis.aplicacionpandax.ui.navigation.BottomNavItem
import com.tesis.aplicacionpandax.ui.navigation.NavRoutes
import com.tesis.aplicacionpandax.ui.screens.admin.ChildDetailScreen
import com.tesis.aplicacionpandax.ui.screens.admin.RegisterChildScreen
import kotlinx.coroutines.flow.Flow

@Composable
fun SpecialistHomeScreen(
    specialistId: Long,
    childrenFlow: Flow<List<Child>>,
    progressRepo: ProgressRepository,
    db: AppDatabase,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val authRepository = AuthRepository(db) // Instancia única

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
                        onClick = { navController.navigate(item.route) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "children",
            modifier = Modifier.padding(padding)
        ) {
            composable("children") {
                SpecialistChildrenScreen(
                    db = db,
                    repo = authRepository,
                    navController = navController,
                    specialistId = specialistId,
                    childrenFlow = childrenFlow
                )
            }
            composable("profile") {
                SpecialistProfileScreen(
                    specialistId = specialistId,
                    db = db // <-- AÑADE ESTE PARÁMETRO
                )
            }
            composable("settings") {
                SpecialistSettingsScreen(
                    specialistId = specialistId, // Pasa el ID del especialista
                    authRepo = authRepository,   // Pasa la instancia del repositorio
                    onLogout = onLogout          // Pasa la función de logout
                )
            }
            composable("child_progress/{childId}") { backStackEntry ->
                val childId = backStackEntry.arguments?.getString("childId")?.toLong() ?: -1
                ChildProgressDetailScreen(
                    childUserId = childId,
                    progressRepo = progressRepo
                )
            }
            composable(NavRoutes.SpecialistRegisterChild.route) {
                RegisterChildScreen(
                    repo = authRepository,
                    specialists = emptyList(),
                    onBack = { navController.popBackStack() },
                    specialistId = specialistId
                )
            }
            composable(
                route = "${NavRoutes.SpecialistRegisterChild.route}/{childId}",
                arguments = listOf(navArgument("childId") { type = NavType.LongType })
            ) { backStackEntry ->
                val childId = backStackEntry.arguments?.getLong("childId") ?: -1L
                RegisterChildScreen(
                    repo = authRepository,
                    specialists = emptyList(),
                    onBack = { navController.popBackStack() },
                    specialistId = specialistId,
                    childId = childId
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
        }
    }
}