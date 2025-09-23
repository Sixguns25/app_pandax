package com.tesis.aplicacionpandax.ui.screens.child

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.tesis.aplicacionpandax.ui.navigation.BottomNavItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import com.tesis.aplicacionpandax.data.entity.Child
import com.tesis.aplicacionpandax.data.entity.Specialist

@Composable
fun ChildHomeScreen(
    child: Child?,
    specialist: Specialist?,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()

    val items = listOf(
        BottomNavItem("home", "Inicio", Icons.Default.Home),
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
            composable("progress") {
                ChildProgressScreen(child)
            }
            composable("profile") {
                ChildProfileScreen(child, onLogout)
            }
        }
    }
}
