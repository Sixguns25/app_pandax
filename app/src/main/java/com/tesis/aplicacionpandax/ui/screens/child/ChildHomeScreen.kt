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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.entity.Child
import com.tesis.aplicacionpandax.data.entity.GameSession
import com.tesis.aplicacionpandax.data.entity.Specialist
import com.tesis.aplicacionpandax.repository.ProgressRepository
import com.tesis.aplicacionpandax.ui.games.CoordinationGame
import com.tesis.aplicacionpandax.ui.games.MemoryGame
import com.tesis.aplicacionpandax.ui.games.PronunciationGame
import com.tesis.aplicacionpandax.ui.navigation.BottomNavItem
import com.tesis.aplicacionpandax.ui.screens.game.EmotionsGame
import kotlinx.coroutines.launch

@Composable
fun ChildHomeScreen(
    child: Child?,
    specialist: Specialist?,
    progressRepo: ProgressRepository,
    db: AppDatabase,
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

    // Extrae specialtyId del especialista para pasarlo a GamesMenuScreen
    var specialtyId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(specialist) {
        specialtyId = specialist?.specialtyId
    }

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
                ChildHomeSection(child, specialist, db)
            }
            composable("games") {
                GamesMenuScreen(
                    child = child,
                    specialtyId = specialtyId,
                    navController = navController,
                    progressRepo = progressRepo
                )
            }
            composable("memory_game/{childUserId}") { backStackEntry ->
                val childUserId = backStackEntry.arguments?.getString("childUserId")?.toLong() ?: 0L
                MemoryGame(
                    childUserId = childUserId,
                    progressRepo = progressRepo,
                    onGameEnd = { score, timeTaken, attempts ->
                        Log.d("ChildHomeScreen", "Juego de Memoria terminado: score=$score, timeTaken=$timeTaken, attempts=$attempts")
                        scope.launch {
                            progressRepo.saveSession(
                                GameSession(
                                    childUserId = childUserId,
                                    gameType = "MEMORY",
                                    score = score,
                                    timeTaken = timeTaken,
                                    attempts = attempts,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                        navController.popBackStack()
                    }
                )
            }
            composable("emotions_game/{childUserId}") { backStackEntry ->
                val childUserId = backStackEntry.arguments?.getString("childUserId")?.toLong() ?: 0L
                EmotionsGame(
                    childUserId = childUserId,
                    onSessionComplete = { session ->
                        scope.launch { progressRepo.saveSession(session) }
                        navController.popBackStack()
                    }
                )
            }
            composable("coordination_game/{childUserId}") { backStackEntry ->
                val childUserId = backStackEntry.arguments?.getString("childUserId")?.toLong() ?: 0L
                CoordinationGame(
                    childUserId = childUserId,
                    progressRepo = progressRepo,
                    onGameEnd = { score, timeTaken, attempts ->
                        Log.d("ChildHomeScreen", "Juego de Coordinación terminado: score=$score, timeTaken=$timeTaken, attempts=$attempts")
                        scope.launch {
                            progressRepo.saveSession(
                                GameSession(
                                    childUserId = childUserId,
                                    gameType = "COORDINATION",
                                    score = score,
                                    timeTaken = timeTaken,
                                    attempts = attempts,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                        navController.popBackStack()
                    }
                )
            }
            composable("pronunciation_game/{childUserId}") { backStackEntry ->
                val childUserId = backStackEntry.arguments?.getString("childUserId")?.toLong() ?: 0L
                PronunciationGame(
                    childUserId = childUserId,
                    progressRepo = progressRepo,
                    onGameEnd = { score, timeTaken, attempts ->
                        Log.d("ChildHomeScreen", "Juego de Pronunciación terminado: score=$score, timeTaken=$timeTaken, attempts=$attempts")
                        scope.launch {
                            progressRepo.saveSession(
                                GameSession(
                                    childUserId = childUserId,
                                    gameType = "PRONUNCIATION",
                                    score = score,
                                    timeTaken = timeTaken,
                                    attempts = attempts,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                        navController.popBackStack()
                    }
                )
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