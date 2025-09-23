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
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tesis.aplicacionpandax.data.entity.Child
import com.tesis.aplicacionpandax.ui.navigation.BottomNavItem
import kotlinx.coroutines.flow.Flow

@Composable
fun SpecialistHomeScreen(
    specialistId: Long,
    childrenFlow: Flow<List<Child>>,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()

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
                SpecialistChildrenScreen(childrenFlow)
            }
            composable("profile") {
                SpecialistProfileScreen(specialistId)
            }
            composable("settings") {
                SpecialistSettingsScreen(onLogout)
            }
        }
    }
}
