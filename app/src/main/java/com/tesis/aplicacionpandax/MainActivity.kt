package com.tesis.aplicacionpandax

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.repository.AuthRepository
import com.tesis.aplicacionpandax.repository.ProgressRepository
import com.tesis.aplicacionpandax.ui.viewmodel.AuthViewModel
import com.tesis.aplicacionpandax.ui.navigation.NavRoutes
import com.tesis.aplicacionpandax.ui.screens.admin.AdminHomeScreen
import com.tesis.aplicacionpandax.ui.screens.admin.RegisterChildScreen
import com.tesis.aplicacionpandax.ui.screens.admin.RegisterSpecialistScreen
import com.tesis.aplicacionpandax.ui.screens.child.ChildHomeScreen
import com.tesis.aplicacionpandax.ui.screens.common.LoginScreen
import com.tesis.aplicacionpandax.ui.screens.specialist.SpecialistHomeScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class MainActivity : ComponentActivity() {

    private val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getInstance(applicationContext, applicationScope)
        val authRepo = AuthRepository(db)
        val progressRepo = ProgressRepository(db)

        setContent {
            val viewModel: AuthViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return AuthViewModel(authRepo) as T
                    }
                }
            )

            val navController = rememberNavController()
            var loggedUserId by remember { mutableStateOf<Long?>(null) }

            // Flujos de datos
            val specialistsFlow = db.specialistDao().getAll()
            val specialists by specialistsFlow.collectAsState(initial = emptyList())

            NavHost(
                navController = navController,
                startDestination = NavRoutes.Login.route
            ) {
                // Login
                composable(NavRoutes.Login.route) {
                    LoginScreen(viewModel) { user ->
                        loggedUserId = user.userId
                        when (user.role) {
                            "ADMIN" -> navController.navigate(NavRoutes.AdminHome.route)
                            "SPECIALIST" -> navController.navigate(NavRoutes.SpecialistHome.route)
                            "CHILD" -> navController.navigate(NavRoutes.ChildHome.route)
                        }
                    }
                }

                // Admin
                composable(NavRoutes.AdminHome.route) {
                    AdminHomeScreen(
                        onRegisterSpecialist = { navController.navigate(NavRoutes.RegisterSpecialist.route) },
                        onRegisterChild = { navController.navigate(NavRoutes.RegisterChild.route) },
                        onLogout = {
                            loggedUserId = null
                            navController.navigate(NavRoutes.Login.route) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            }
                        }
                    )
                }
                composable(NavRoutes.RegisterSpecialist.route) {
                    RegisterSpecialistScreen(
                        repo = authRepo,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(NavRoutes.RegisterChild.route) {
                    RegisterChildScreen(
                        repo = authRepo,
                        specialists = specialists,
                        onBack = { navController.popBackStack() }
                    )
                }

                // Specialist
                composable(NavRoutes.SpecialistHome.route) {
                    SpecialistHomeScreen(
                        specialistId = loggedUserId ?: -1,
                        childrenFlow = db.childDao().getChildrenForSpecialist(loggedUserId ?: -1),
                        progressRepo = progressRepo, // Agregar progressRepo
                        onLogout = {
                            loggedUserId = null
                            navController.navigate(NavRoutes.Login.route) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            }
                        }
                    )
                }

                composable(NavRoutes.SpecialistRegisterChild.route) {
                    RegisterChildScreen(
                        repo = authRepo,
                        specialists = specialists,
                        onBack = { navController.popBackStack() },
                        specialistId = loggedUserId // Pasamos el ID del especialista logueado
                    )
                }

                // Child
                composable(NavRoutes.ChildHome.route) {
                    val childFlow = db.childDao().getChildByUserId(loggedUserId ?: -1)
                    val child by childFlow.collectAsState(initial = null)

                    val specialistFlow = db.specialistDao().getById(child?.specialistId ?: -1)
                    val specialist by specialistFlow.collectAsState(initial = null)

                    ChildHomeScreen(
                        child = child,
                        specialist = specialist,
                        progressRepo = progressRepo,
                        onLogout = {
                            loggedUserId = null
                            navController.navigate(NavRoutes.Login.route) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            }
                        }
                    )
                }

            }
        }
    }
}