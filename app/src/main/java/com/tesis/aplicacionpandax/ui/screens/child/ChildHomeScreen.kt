package com.tesis.aplicacionpandax.ui.screens.child

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tesis.aplicacionpandax.data.entity.Child
import com.tesis.aplicacionpandax.data.entity.GameSession
import com.tesis.aplicacionpandax.data.entity.Specialist
import com.tesis.aplicacionpandax.repository.ProgressRepository
import com.tesis.aplicacionpandax.ui.games.MemoryGame
import com.tesis.aplicacionpandax.ui.navigation.BottomNavItem
import kotlinx.coroutines.launch

@Composable
fun ChildHomeScreen(
    child: Child?,
    specialist: Specialist?,
    progressRepo: ProgressRepository,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val items = listOf(
        BottomNavItem("home", "Inicio", Icons.Default.Home),
        BottomNavItem("games", "Juegos", Icons.Default.Games),
        BottomNavItem("progress", "Progreso", Icons.Default.Check),
        BottomNavItem("profile", "Perfil", Icons.Default.Person)
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
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                ChildHomeSection(child, specialist)
            }
            composable("games") {
                if (child != null) {
                    MemoryGame(
                        childUserId = child.userId,
                        progressRepo = progressRepo, // Mantener para que MemoryGame guarde la sesión
                        onGameEnd = { score, timeTaken, attempts ->
                            // Eliminar el guardado duplicado aquí
                            // Solo loguear o notificar si es necesario
                            Log.d("ChildHomeScreen", "Juego terminado: score=$score, timeTaken=$timeTaken, attempts=$attempts")
                            // Puedes agregar feedback UI aquí si quieres, ej. mostrar un snackbar
                        }
                    )
                } else {
                    Text("No se encontró información del niño.")
                }
            }
            composable("progress") {
                ChildProgressScreen(child, progressRepo)
            }
            composable("profile") {
                ChildProfileScreen(child, onLogout)
            }
        }
    }
}