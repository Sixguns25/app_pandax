package com.tesis.aplicacionpandax.ui.screens.child

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tesis.aplicacionpandax.data.entity.Child
import com.tesis.aplicacionpandax.repository.ProgressRepository

@Composable
fun GamesMenuScreen(
    child: Child?,
    specialtyId: Long?,
    navController: NavController,
    progressRepo: ProgressRepository,
    modifier: Modifier = Modifier
) {
    val games by progressRepo.getGamesForSpecialty(specialtyId ?: -1L).collectAsState(initial = emptyList())
    val specialtyName = when (specialtyId) {
        1L -> "Conducta"
        2L -> "Fonoaudiología"
        3L -> "Lenguaje"
        4L -> "Educación"
        5L -> "Terapia de motricidad"
        else -> "Desconocida"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Juegos de $specialtyName",
            style = MaterialTheme.typography.headlineMedium,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { contentDescription = "Título de la pantalla de selección de juegos para $specialtyName" }
        )

        if (child == null || specialtyId == null) {
            Text(
                text = "No se encontró información del niño o especialista.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.semantics { contentDescription = "Mensaje de error: No se encontró información del niño o especialista" }
            )
        } else if (games.isEmpty()) {
            Text(
                text = "No hay juegos disponibles para esta especialidad.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.semantics { contentDescription = "Mensaje de error: No hay juegos disponibles para la especialidad $specialtyName" }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(games) { game ->
                    Button(
                        onClick = {
                            val route = game.route.replace("{childUserId}", child.userId.toString())
                            navController.navigate(route)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .semantics { contentDescription = "Botón para jugar a ${game.displayName}" }
                    ) {
                        Text(
                            text = game.displayName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}