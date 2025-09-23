package com.tesis.aplicacionpandax.ui.navigation

sealed class NavRoutes(val route: String) {
    object Login : NavRoutes("login")

    // Admin
    object AdminHome : NavRoutes("admin_home")
    object RegisterSpecialist : NavRoutes("register_specialist")
    object RegisterChild : NavRoutes("register_child")

    // Specialist
    object SpecialistHome : NavRoutes("specialist_home")

    // Child
    object ChildHome : NavRoutes("child_home")

    object SpecialistRegisterChild : NavRoutes("specialist_register_child")
}
