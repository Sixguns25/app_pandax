package com.tesis.aplicacionpandax

import android.os.Bundle
import android.util.Log // Importar Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize // Importar
import androidx.compose.foundation.layout.wrapContentHeight // Importar
import androidx.compose.material3.Text // Importar Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier // Importar Modifier
import androidx.compose.ui.text.style.TextAlign // Importar TextAlign
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController // Importar NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.entity.Specialist // Importar Specialist
import com.tesis.aplicacionpandax.data.entity.GameSession // Importar GameSession
import com.tesis.aplicacionpandax.repository.AuthRepository
import com.tesis.aplicacionpandax.repository.ProgressRepository
import com.tesis.aplicacionpandax.ui.navigation.NavRoutes
import com.tesis.aplicacionpandax.ui.screens.admin.*
import com.tesis.aplicacionpandax.ui.screens.child.ChildHomeScreen
import com.tesis.aplicacionpandax.ui.screens.common.LoginScreen
import com.tesis.aplicacionpandax.ui.screens.specialist.ChildProgressDetailScreen
import com.tesis.aplicacionpandax.ui.screens.specialist.SpecialistHomeScreen
import com.tesis.aplicacionpandax.ui.theme.AplicacionPandaxTheme
import com.tesis.aplicacionpandax.ui.viewmodel.AuthViewModel
// Importar los juegos
import com.tesis.aplicacionpandax.ui.games.CoordinationGame
import com.tesis.aplicacionpandax.ui.games.MemoryGame
import com.tesis.aplicacionpandax.ui.games.PronunciationGame
import com.tesis.aplicacionpandax.ui.screens.game.EmotionsGame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getInstance(applicationContext, applicationScope)
        val authRepo = AuthRepository(db)
        val progressRepo = ProgressRepository(db)

        setContent {
            AplicacionPandaxTheme(darkTheme = isSystemInDarkTheme(), dynamicColor = true) {
                val viewModel: AuthViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return AuthViewModel(authRepo) as T
                        }
                    }
                )

                val navController = rememberNavController()
                var loggedUserId by remember { mutableStateOf<Long?>(null) }
                val coroutineScope = rememberCoroutineScope()

                // Flujos de datos
                val specialistsFlow = db.specialistDao().getAll()
                val specialists by specialistsFlow.collectAsState(initial = emptyList())
                val childrenFlow = db.childDao().getAllWithSpecialist()

                NavHost(
                    navController = navController,
                    startDestination = NavRoutes.Login.route
                ) {
                    // --- Login ---
                    composable(NavRoutes.Login.route) {
                        LoginScreen(viewModel) { user ->
                            loggedUserId = user.userId
                            val destination = when (user.role) {
                                "ADMIN" -> NavRoutes.AdminHome.route
                                "SPECIALIST" -> NavRoutes.SpecialistHome.route
                                "CHILD" -> NavRoutes.ChildHome.route
                                else -> NavRoutes.Login.route
                            }
                            navController.navigate(destination) {
                                popUpTo(NavRoutes.Login.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }

                    // --- Admin ---
                    composable(NavRoutes.AdminHome.route) {
                        AdminHomeScreen(
                            onRegisterSpecialist = { navController.navigate(NavRoutes.RegisterSpecialist.route) },
                            onRegisterChild = { navController.navigate(NavRoutes.RegisterChild.route) },
                            onManageSpecialties = { navController.navigate(NavRoutes.ManageSpecialties.route) },
                            onManageSpecialists = { navController.navigate(NavRoutes.ManageSpecialists.route) },
                            onManageChildren = { navController.navigate(NavRoutes.ManageChildren.route) },
                            onLogout = {
                                loggedUserId = null
                                navController.navigate(NavRoutes.Login.route) {
                                    popUpTo(NavRoutes.AdminHome.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(NavRoutes.RegisterSpecialist.route) {
                        RegisterSpecialistScreen( repo = authRepo, db = db, specialistId = -1L, onBack = { navController.popBackStack() } )
                    }
                    composable(
                        route = "${NavRoutes.RegisterSpecialist.route}/{specialistId}",
                        arguments = listOf(navArgument("specialistId") { type = NavType.LongType; defaultValue = -1L })
                    ) { backStackEntry ->
                        val specialistIdArg = backStackEntry.arguments?.getLong("specialistId") ?: -1L
                        RegisterSpecialistScreen( repo = authRepo, db = db, specialistId = specialistIdArg, onBack = { navController.popBackStack() } )
                    }
                    // Registrar niño (Admin) - CORREGIDO
                    composable(NavRoutes.RegisterChild.route) {
                        RegisterChildScreen(
                            repo = authRepo,
                            specialists = specialists,
                            onBack = { navController.popBackStack() },
                            db = db // <-- PARÁMETRO db AÑADIDO
                        )
                    }
                    // Editar niño (Admin) - CORREGIDO
                    composable(
                        route = "${NavRoutes.RegisterChild.route}/{childId}",
                        arguments = listOf(navArgument("childId") { type = NavType.LongType; defaultValue = -1L })
                    ) { backStackEntry ->
                        val childIdArg = backStackEntry.arguments?.getLong("childId") ?: -1L
                        RegisterChildScreen(
                            repo = authRepo,
                            specialists = specialists,
                            onBack = { navController.popBackStack() },
                            childId = childIdArg,
                            db = db // <-- PARÁMETRO db AÑADIDO
                        )
                    }
                    composable(NavRoutes.ManageSpecialties.route) {
                        SpecialtiesManagementScreen( db = db, onBack = { navController.popBackStack() } )
                    }
                    composable(NavRoutes.ManageSpecialists.route) {
                        SpecialistsManagementScreen( db = db, repo = authRepo, navController = navController )
                    }
                    composable(NavRoutes.ManageChildren.route) {
                        AdminChildrenScreen( db = db, repo = authRepo, navController = navController, childrenFlow = childrenFlow )
                    }
                    composable(
                        route = "specialist_detail/{specialistId}",
                        arguments = listOf(navArgument("specialistId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val specialistIdArg = backStackEntry.arguments?.getLong("specialistId") ?: -1L
                        SpecialistDetailScreen( navController = navController, db = db, specialistId = specialistIdArg )
                    }


                    // --- Specialist ---
                    // CORREGIDO
                    composable(NavRoutes.SpecialistHome.route) {
                        SpecialistHomeScreen(
                            specialistId = loggedUserId ?: -1,
                            childrenFlow = db.childDao().getChildrenForSpecialist(loggedUserId ?: -1),
                            progressRepo = progressRepo,
                            db = db,
                            authRepo = authRepo, // <-- PARÁMETRO authRepo AÑADIDO
                            onLogout = {
                                loggedUserId = null
                                navController.navigate(NavRoutes.Login.route) {
                                    popUpTo(NavRoutes.SpecialistHome.route) { inclusive = true }
                                }
                            }
                        )
                    }
                    // Registrar niño (Specialist) - CORREGIDO
                    composable(NavRoutes.SpecialistRegisterChild.route) {
                        RegisterChildScreen(
                            repo = authRepo,
                            specialists = specialists,
                            onBack = { navController.popBackStack() },
                            specialistId = loggedUserId,
                            db = db // <-- PARÁMETRO db AÑADIDO
                        )
                    }
                    // Editar niño (Specialist) - CORREGIDO
                    composable(
                        route = "${NavRoutes.SpecialistRegisterChild.route}/{childId}",
                        arguments = listOf(navArgument("childId") { type = NavType.LongType; defaultValue = -1L })
                    ) { backStackEntry ->
                        val childIdArg = backStackEntry.arguments?.getLong("childId") ?: -1L
                        RegisterChildScreen(
                            repo = authRepo,
                            specialists = specialists,
                            onBack = { navController.popBackStack() },
                            specialistId = loggedUserId,
                            childId = childIdArg,
                            db = db // <-- PARÁMETRO db AÑADIDO
                        )
                    }


                    // --- Child ---
                    // CORREGIDO
                    composable(NavRoutes.ChildHome.route) {
                        val childId = loggedUserId ?: -1L
                        val childFlow = db.childDao().getChildByUserId(childId)
                        val child by childFlow.collectAsState(initial = null)
                        var specialist by remember { mutableStateOf<Specialist?>(null) }

                        LaunchedEffect(child?.specialistId) {
                            child?.specialistId?.let { specId ->
                                specialist = db.specialistDao().getByUserId(specId)
                            } ?: run { specialist = null }
                        }

                        ChildHomeScreen(
                            child = child,
                            specialist = specialist,
                            progressRepo = progressRepo,
                            db = db,
                            navController = navController, // <-- PARÁMETRO navController AÑADIDO
                            onLogout = {
                                loggedUserId = null
                                navController.navigate(NavRoutes.Login.route) {
                                    popUpTo(NavRoutes.ChildHome.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    // --- Rutas Comunes ---
                    composable(
                        route = "child_detail/{childId}",
                        arguments = listOf(navArgument("childId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val childIdArg = backStackEntry.arguments?.getLong("childId") ?: -1L
                        ChildDetailScreen( navController = navController, db = db, childId = childIdArg )
                    }
                    // Progreso niño (CORREGIDO)
                    composable(
                        route = "child_progress/{childId}",
                        arguments = listOf(navArgument("childId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val childIdArg = backStackEntry.arguments?.getLong("childId") ?: -1L
                        var childSpecialtyId by remember { mutableStateOf<Long?>(null) }
                        LaunchedEffect(childIdArg) {
                            val child = db.childDao().getByUserId(childIdArg)
                            child?.specialistId?.let { specId ->
                                val spec = db.specialistDao().getByUserId(specId)
                                childSpecialtyId = spec?.specialtyId
                            }
                        }

                        ChildProgressDetailScreen(
                            childUserId = childIdArg,
                            progressRepo = progressRepo,
                            db = db, // <-- PARÁMETRO db AÑADIDO
                            navController = navController, // <-- PARÁMETRO navController AÑADIDO
                            specialtyId = childSpecialtyId // <-- PARÁMETRO specialtyId AÑADIDO
                        )
                    }


                    // --- 👇👇👇 RUTAS DE JUEGOS AÑADIDAS/CORREGIDAS 👇👇👇 ---

                    composable(
                        // Ruta actualizada para incluir el nivel
                        route = "memory_game/{childUserId}/{level}",
                        arguments = listOf(
                            navArgument("childUserId") { type = NavType.LongType },
                            navArgument("level") { type = NavType.IntType } // Argumento de nivel
                        )
                    ) { backStackEntry ->
                        val childId = backStackEntry.arguments?.getLong("childUserId") ?: -1L
                        val level = backStackEntry.arguments?.getInt("level") ?: 1 // Extraer nivel

                        if (childId == -1L) {
                            Text("Error: ID de niño inválido.", textAlign = TextAlign.Center, modifier = Modifier.fillMaxSize().wrapContentHeight())
                        } else {
                            MemoryGame(
                                childUserId = childId,
                                progressRepo = progressRepo,
                                level = level, // Pasar el nivel
                                // onGameEnd ahora acepta 4 parámetros
                                onGameEnd = { stars, timeTaken, attempts, gameLevel ->
                                    coroutineScope.launch {
                                        progressRepo.saveSession(GameSession(
                                            childUserId = childId,
                                            gameType = "MEMORY",
                                            stars = stars,
                                            timeTaken = timeTaken,
                                            attempts = attempts,
                                            level = gameLevel // Guardar el nivel
                                        ))
                                    }
                                    navController.popBackStack()
                                }
                            )
                        }
                    }

                    composable(
                        route = "emotions_game/{childUserId}",
                        arguments = listOf(navArgument("childUserId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val childId = backStackEntry.arguments?.getLong("childUserId") ?: -1L
                        if (childId == -1L) {
                            Text("Error: ID de niño inválido.", textAlign = TextAlign.Center, modifier = Modifier.fillMaxSize().wrapContentHeight())
                        } else {
                            EmotionsGame(
                                childUserId = childId,
                                onSessionComplete = { session ->
                                    // EmotionsGame NO tiene niveles (aún), así que guardamos la sesión tal cual
                                    coroutineScope.launch {
                                        progressRepo.saveSession(session)
                                    }
                                    navController.popBackStack()
                                }
                            )
                        }
                    }

                    composable(
                        route = "coordination_game/{childUserId}/{level}", // 1. Ruta actualizada
                        arguments = listOf(
                            navArgument("childUserId") { type = NavType.LongType },
                            navArgument("level") { type = NavType.IntType } // 2. Argumento de nivel añadido
                        )
                    ) { backStackEntry ->
                        val childId = backStackEntry.arguments?.getLong("childUserId") ?: -1L
                        val level = backStackEntry.arguments?.getInt("level") ?: 1 // 3. Extraer nivel

                        if (childId == -1L) {
                            Text("Error: ID de niño inválido.", textAlign = TextAlign.Center, modifier = Modifier.fillMaxSize().wrapContentHeight())
                        } else {
                            CoordinationGame(
                                childUserId = childId,
                                progressRepo = progressRepo,
                                level = level, // 4. Pasar el nivel
                                // 5. Actualizar lambda onGameEnd (CoordinationGame ya guarda internamente)
                                onGameEnd = { stars, timeTaken, attempts, gameLevel ->
                                    // El juego ya guarda la sesión internamente (incluyendo el nivel).
                                    // Solo necesitamos volver atrás.
                                    navController.popBackStack()
                                }
                            )
                        }
                    }

                    composable(
                        route = "pronunciation_game/{childUserId}",
                        arguments = listOf(navArgument("childUserId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val childId = backStackEntry.arguments?.getLong("childUserId") ?: -1L
                        if (childId == -1L) {
                            Text("Error: ID de niño inválido.", textAlign = TextAlign.Center, modifier = Modifier.fillMaxSize().wrapContentHeight())
                        } else {
                            PronunciationGame(
                                childUserId = childId,
                                progressRepo = progressRepo,
                                onGameEnd = { stars, timeTaken, attempts ->
                                    // PronunciationGame ya guarda internamente (con level=1 por defecto)
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                    // --- 👆👆👆 FIN DE RUTAS DE JUEGOS 👆👆👆 ---

                } // Fin NavHost
            } // Fin AplicacionPandaxTheme
        } // Fin setContent
    } // Fin onCreate
}