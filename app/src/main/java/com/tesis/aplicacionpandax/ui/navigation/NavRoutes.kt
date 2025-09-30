package com.tesis.aplicacionpandax.ui.navigation

sealed class NavRoutes(val route: String) {
    object Login : NavRoutes("login")

    // Admin
    object AdminHome : NavRoutes("admin_home")
    object RegisterSpecialist : NavRoutes("register_specialist")
    object RegisterChild : NavRoutes("register_child")
    object ManageSpecialties : NavRoutes("manage_specialties") // Añadido
    object ManageSpecialists : NavRoutes("manage_specialists") // Añadido
    object ManageChildren : NavRoutes("manage_children") // Nueva ruta

    // Specialist
    object SpecialistHome : NavRoutes("specialist_home")
    object SpecialistRegisterChild : NavRoutes("specialist_register_child")

    // Child
    object ChildHome : NavRoutes("child_home")
    object Games : NavRoutes("games")

    // Rutas comunes
    object ChildDetail : NavRoutes("child_detail/{childId}")
    object ChildProgress : NavRoutes("child_progress/{childId}")
}