package com.tesis.aplicacionpandax

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.repository.AuthRepository
import com.tesis.aplicacionpandax.repository.ProgressRepository
import com.tesis.aplicacionpandax.ui.viewmodel.AuthViewModel
import com.tesis.aplicacionpandax.ui.navigation.NavRoutes
import com.tesis.aplicacionpandax.ui.screens.admin.AdminChildrenScreen
import com.tesis.aplicacionpandax.ui.screens.admin.AdminHomeScreen
import com.tesis.aplicacionpandax.ui.screens.admin.RegisterChildScreen
import com.tesis.aplicacionpandax.ui.screens.admin.RegisterSpecialistScreen
import com.tesis.aplicacionpandax.ui.screens.admin.SpecialtiesManagementScreen
import com.tesis.aplicacionpandax.ui.screens.admin.SpecialistsManagementScreen
import com.tesis.aplicacionpandax.ui.screens.admin.SpecialistDetailScreen
import com.tesis.aplicacionpandax.ui.screens.admin.ChildDetailScreen
import com.tesis.aplicacionpandax.ui.screens.child.ChildHomeScreen
import com.tesis.aplicacionpandax.ui.screens.common.LoginScreen
import com.tesis.aplicacionpandax.ui.screens.specialist.SpecialistHomeScreen
import com.tesis.aplicacionpandax.ui.screens.specialist.ChildProgressDetailScreen
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
            val viewModel: AuthViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
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
                        onManageSpecialties = { navController.navigate(NavRoutes.ManageSpecialties.route) },
                        onManageSpecialists = { navController.navigate(NavRoutes.ManageSpecialists.route) },
                        onManageChildren = { navController.navigate(NavRoutes.ManageChildren.route) },
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
                        db = db,
                        specialistId = -1L,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = "${NavRoutes.RegisterSpecialist.route}/{specialistId}",
                    arguments = listOf(navArgument("specialistId") { type = NavType.LongType; defaultValue = -1L })
                ) { backStackEntry ->
                    val specialistId = backStackEntry.arguments?.getLong("specialistId") ?: -1L
                    RegisterSpecialistScreen(
                        repo = authRepo,
                        db = db,
                        specialistId = specialistId,
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
                composable(
                    route = "${NavRoutes.RegisterChild.route}/{childId}",
                    arguments = listOf(navArgument("childId") { type = NavType.LongType; defaultValue = -1L })
                ) { backStackEntry ->
                    val childId = backStackEntry.arguments?.getLong("childId") ?: -1L
                    RegisterChildScreen(
                        repo = authRepo,
                        specialists = specialists,
                        onBack = { navController.popBackStack() },
                        childId = childId
                    )
                }
                composable(NavRoutes.ManageSpecialties.route) {
                    SpecialtiesManagementScreen(
                        db = db,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(NavRoutes.ManageSpecialists.route) {
                    SpecialistsManagementScreen(
                        db = db,
                        repo = authRepo,
                        navController = navController
                    )
                }
                composable(NavRoutes.ManageChildren.route) {
                    AdminChildrenScreen(
                        db = db,
                        repo = authRepo,
                        navController = navController,
                        childrenFlow = childrenFlow
                    )
                }
                composable(
                    route = "specialist_detail/{specialistId}",
                    arguments = listOf(navArgument("specialistId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val specialistId = backStackEntry.arguments?.getLong("specialistId") ?: -1L
                    SpecialistDetailScreen(
                        navController = navController,
                        db = db,
                        specialistId = specialistId
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
                composable(
                    route = "child_progress/{childId}",
                    arguments = listOf(navArgument("childId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val childId = backStackEntry.arguments?.getLong("childId") ?: -1L
                    ChildProgressDetailScreen(
                        childUserId = childId,
                        progressRepo = progressRepo
                    )
                }

                // Specialist
                composable(NavRoutes.SpecialistHome.route) {
                    SpecialistHomeScreen(
                        specialistId = loggedUserId ?: -1,
                        childrenFlow = db.childDao().getChildrenForSpecialist(loggedUserId ?: -1),
                        progressRepo = progressRepo,
                        db = db,
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
                        specialistId = loggedUserId
                    )
                }
                composable(
                    route = "${NavRoutes.SpecialistRegisterChild.route}/{childId}",
                    arguments = listOf(navArgument("childId") { type = NavType.LongType; defaultValue = -1L })
                ) { backStackEntry ->
                    val childId = backStackEntry.arguments?.getLong("childId") ?: -1L
                    RegisterChildScreen(
                        repo = authRepo,
                        specialists = specialists,
                        onBack = { navController.popBackStack() },
                        specialistId = loggedUserId,
                        childId = childId
                    )
                }

                // Child
                composable(NavRoutes.ChildHome.route) {
                    val childFlow = db.childDao().getChildByUserId(loggedUserId ?: -1)
                    val child by childFlow.collectAsState(initial = null)
                    var specialist by remember { mutableStateOf<com.tesis.aplicacionpandax.data.entity.Specialist?>(null) }

                    LaunchedEffect(child?.specialistId) {
                        child?.specialistId?.let { specialistId ->
                            specialist = db.specialistDao().getByUserId(specialistId)
                        }
                    }

                    ChildHomeScreen(
                        child = child,
                        specialist = specialist,
                        progressRepo = progressRepo,
                        db = db,
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