package com.tesis.aplicacionpandax.ui.screens.child

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // Importar iconos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController // <-- IMPORTACIÓN AÑADIDA
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.entity.Child
import com.tesis.aplicacionpandax.data.entity.Specialist
import com.tesis.aplicacionpandax.repository.ProgressRepository
import com.tesis.aplicacionpandax.ui.navigation.BottomNavItem
import kotlinx.coroutines.launch // Importar launch

@Composable
fun ChildHomeScreen(
    child: Child?,
    specialist: Specialist?,
    progressRepo: ProgressRepository,
    db: AppDatabase,
    navController: NavController, // <-- PARÁMETRO AÑADIDO (es el NavController principal de MainActivity)
    onLogout: () -> Unit
) {
    val bottomNavController = rememberNavController() // NavController INTERNO para las pestañas

    val items = listOf(
        BottomNavItem("home", "Inicio", Icons.Default.Home),
        BottomNavItem("games", "Juegos", Icons.Default.Games),
        BottomNavItem("progress", "Progreso", Icons.Default.Check),
        BottomNavItem("profile", "Perfil", Icons.Default.Person)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                items.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                bottomNavController.navigate(item.route){
                                    popUpTo(bottomNavController.graph.startDestinationId){ saveState = true }
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
        NavHost(
            navController = bottomNavController, // Usa el NavController interno
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                ChildHomeSection(
                    child = child,
                    specialist = specialist,
                    db = db,
                    navController = bottomNavController // Pasa el INTERNO para navegar a "games"
                )
            }
            composable("games") {
                GamesMenuScreen(
                    child = child,
                    specialtyId = specialist?.specialtyId,
                    navController = navController, // Pasa el NavController PRINCIPAL (de MainActivity)
                    progressRepo = progressRepo
                )
            }
            composable("progress") {
                ChildProgressScreen( // Esta pantalla no navega, así que no necesita NavController
                    child = child,
                    specialtyId = specialist?.specialtyId,
                    progressRepo = progressRepo
                )
            }
            composable("profile") {
                ChildProfileScreen(child = child, onLogout = onLogout)
            }
        } // Fin NavHost interno
    } // Fin Scaffold
}