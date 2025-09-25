package com.tesis.aplicacionpandax.ui.screens.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesis.aplicacionpandax.R
import com.tesis.aplicacionpandax.data.entity.GameSession
import kotlinx.coroutines.delay

@Composable
fun EmotionsGame(
    childUserId: Long,
    onSessionComplete: (GameSession) -> Unit,
    modifier: Modifier = Modifier
) {
    var score by remember { mutableStateOf(0) }  // Aciertos (0-4)
    var attempts by remember { mutableStateOf(0) }
    var startTime by remember { mutableStateOf<Long?>(null) }
    var elapsedTime by remember { mutableStateOf(0L) }
    var showFeedback by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }
    var gameFinished by remember { mutableStateOf(false) }
    var stars by remember { mutableStateOf(0) }
    var showInstructions by remember { mutableStateOf(true) }

    // Lista de emociones con sus imágenes
    val emotions = listOf(
        Pair(R.drawable.happy_face, "Feliz"),
        Pair(R.drawable.sad_face, "Triste"),
        Pair(R.drawable.angry_face, "Enojado"),
        Pair(R.drawable.surprised_face, "Sorprendido")
    )
    // Lista mutable para evitar repeticiones
    var availableEmotions by remember { mutableStateOf(emotions.shuffled()) }
    var currentEmotion by remember { mutableStateOf(availableEmotions.firstOrNull() ?: emotions[0]) }
    val options = emotions.map { it.second }.shuffled()

    // Contexto para text-to-speech (opcional)
    val context = LocalContext.current

    // Actualizar cronómetro cada segundo
    LaunchedEffect(gameFinished, showInstructions) {
        while (!gameFinished && !showInstructions) {
            delay(1000)
            startTime?.let {
                elapsedTime = System.currentTimeMillis() - it
            }
        }
    }

    // Manejar retroalimentación y cambio de emoción
    LaunchedEffect(attempts) {
        if (attempts > 0 && !gameFinished) {
            showFeedback = true
            delay(1000) // Mostrar retroalimentación por 1 segundo
            showFeedback = false
            // Avanzar a la siguiente emoción
            if (availableEmotions.size > 1) {
                availableEmotions = availableEmotions.drop(1) // Eliminar la emoción actual
                currentEmotion = availableEmotions.firstOrNull() ?: emotions[0]
            } else {
                // Reiniciar la lista si se acaban las emociones (aunque con 4 intentos no debería llegar aquí)
                availableEmotions = emotions.shuffled()
                currentEmotion = availableEmotions.firstOrNull() ?: emotions[0]
            }
        }
    }

    // Completar sesión después de 4 intentos
    LaunchedEffect(attempts) {
        if (attempts >= 4) {
            gameFinished = true
            val durationSeconds = (elapsedTime / 1000).toInt()

            // Calcular estrellas basado en aciertos y tiempo
            stars = when {
                score == 4 && durationSeconds <= 20 -> 3  // Todo correcto y rápido (≤20s)
                score >= 3 && durationSeconds <= 40 -> 2  // Mayoría correcta y tiempo moderado (≤40s)
                else -> 1  // Completado con más errores o tiempo
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "¡Encuentra la emoción!",
            style = MaterialTheme.typography.headlineMedium,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { contentDescription = "Título del juego de emociones" }
        )

        if (!showInstructions && !gameFinished) {
            // Imagen de la emoción
            Image(
                painter = painterResource(id = currentEmotion.first),
                contentDescription = "Emoción ${currentEmotion.second}",
                modifier = Modifier
                    .size(150.dp)
                    .semantics { contentDescription = "Imagen de la emoción ${currentEmotion.second}" }
            )

            // Opciones
            options.forEach { option ->
                Button(
                    onClick = {
                        if (attempts < 4) { // Evitar clics después de completar la sesión
                            attempts++
                            isCorrect = option == currentEmotion.second
                            if (isCorrect) {
                                score += 1  // Incrementar aciertos (máximo 4)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .semantics { contentDescription = "Botón para seleccionar $option" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = attempts < 4 // Deshabilitar botones tras 4 intentos
                ) {
                    Text(
                        text = option,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Retroalimentación
            AnimatedVisibility(
                visible = showFeedback,
                enter = fadeIn(animationSpec = tween(500)),
                exit = fadeOut(animationSpec = tween(500))
            ) {
                Text(
                    text = if (isCorrect) "¡Correcto! 🎉" else "¡Intenta de nuevo! 😊",
                    color = if (isCorrect) Color.Green else Color.Red,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { contentDescription = "Retroalimentación del juego" }
                )
            }

            // Puntuación actual (aciertos)
            Text(
                text = "Aciertos: $score",
                fontSize = 18.sp,
                modifier = Modifier.semantics { contentDescription = "Aciertos actuales: $score" }
            )
        }

        // Mensaje final al terminar
        AnimatedVisibility(
            visible = gameFinished,
            enter = fadeIn() + scaleIn()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "🎉 ¡Juego completado!",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { contentDescription = "Mensaje de juego completado" }
                )
                Text(
                    text = "Aciertos: $score de 4",
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { contentDescription = "Aciertos: $score de 4" }
                )
                Text(
                    text = "Tiempo: ${elapsedTime / 1000} s",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.semantics { contentDescription = "Tiempo: ${elapsedTime / 1000} segundos" }
                )

                // Estrellas obtenidas
                Row {
                    repeat(stars) {
                        Text(
                            text = "⭐",
                            style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.semantics { contentDescription = "Estrella obtenida" }
                        )
                    }
                }

                Text(
                    text = when (stars) {
                        3 -> "¡Excelente! Todo correcto y rápido 🎯"
                        2 -> "Muy bien, pero puedes mejorar ⏳"
                        else -> "¡Sigue practicando! 💪"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.semantics { contentDescription = "Mensaje de retroalimentación por estrellas" }
                )

                Button(
                    onClick = {
                        val timeTaken = System.currentTimeMillis() - startTime!!
                        onSessionComplete(
                            GameSession(
                                childUserId = childUserId,
                                gameType = "EMOTIONS",
                                score = stars,
                                timeTaken = timeTaken,
                                attempts = attempts,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    },
                    modifier = Modifier.semantics { contentDescription = "Botón para volver al menú" }
                ) {
                    Text("Volver al Menú")
                }
            }
        }
    }

    // Modal de instrucciones al inicio
    if (showInstructions) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {
                Button(onClick = {
                    showInstructions = false
                    startTime = System.currentTimeMillis()
                }) {
                    Text("Comenzar ▶️")
                }
            },
            title = { Text("📖 Instrucciones") },
            text = {
                Column {
                    Text("Encuentra la emoción correcta para cada imagen.", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("⭐ 3 estrellas → Todo correcto y rápido (≤20s)", style = MaterialTheme.typography.bodyMedium)
                    Text("⭐ 2 estrellas → Mayoría correcta y tiempo moderado (≤40s)", style = MaterialTheme.typography.bodyMedium)
                    Text("⭐ 1 estrella → Completado con más errores o tiempo", style = MaterialTheme.typography.bodyMedium)
                }
            }
        )
    }
}