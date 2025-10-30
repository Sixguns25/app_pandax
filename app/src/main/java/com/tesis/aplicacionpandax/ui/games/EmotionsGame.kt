package com.tesis.aplicacionpandax.ui.screens.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape // Para botones
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign // Importar
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesis.aplicacionpandax.R
// QUITA: import com.tesis.aplicacionpandax.data.entity.GameSession
import kotlinx.coroutines.delay
import kotlin.random.Random // Importar Random

// --- Definiciones de Clases de Datos (fuera del Composable) ---

// Define la estructura de una emoción
data class Emotion(val drawableId: Int, val name: String)

// Define la estructura para los criterios de estrellas
data class StarCriteria(val score3Stars: Int, val time3Stars: Int, val score2Stars: Int, val time2Stars: Int)

// --- CORRECCIÓN 1: Reemplaza Triple con una data class ---
// Define la configuración del nivel
private data class LevelConfig(
    val emotionPool: List<Emotion>,
    val numRounds: Int,
    val numOptions: Int,
    val starCriteria: StarCriteria
)

@Composable
fun EmotionsGame(
    childUserId: Long,
    level: Int, // <-- Parámetro de nivel
    onGameEnd: (stars: Int, timeTaken: Long, attempts: Int, level: Int) -> Unit, // <-- Callback actualizado
    modifier: Modifier = Modifier
) {
    // --- ESTADOS ---
    var score by remember { mutableStateOf(0) }  // Aciertos
    var currentRound by remember { mutableStateOf(0) } // Ronda actual (0, 1, 2...)
    var startTime by remember { mutableStateOf<Long?>(null) }
    var elapsedTime by remember { mutableStateOf(0L) }
    var showFeedback by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }
    var gameFinished by remember { mutableStateOf(false) }
    var stars by remember { mutableStateOf(0) }
    var showInstructions by remember { mutableStateOf(true) }

    // --- LÓGICA DE NIVELES ---

    // Pools de emociones
    // NOTA: Añade R.drawable.scared_face y R.drawable.bored_face a tus 'drawable'
    // (Uso surprised y sad como placeholders temporales)
    val easyEmotions = remember { listOf(
        Emotion(R.drawable.happy_face, "Feliz"),
        Emotion(R.drawable.sad_face, "Triste")
    )}
    val normalEmotions = remember { easyEmotions + listOf(
        Emotion(R.drawable.angry_face, "Enojado"),
        Emotion(R.drawable.surprised_face, "Sorprendido")
    )}
    val hardEmotions = remember { normalEmotions + listOf(
        // TODO: Reemplaza estos placeholders por tus propios drawables
        Emotion(R.drawable.surprised_face, "Asustado"), // Placeholder
        Emotion(R.drawable.sad_face, "Aburrido")      // Placeholder
    )}

    // Propiedades del nivel (Usando LevelConfig en lugar de Triple)
    val (emotionPool, numRounds, numOptions, starCriteria) = remember(level) {
        when (level) {
            1 -> LevelConfig(easyEmotions, 4, 2, StarCriteria(4, 30, 3, 45)) // 4 rondas, 2 opciones
            3 -> LevelConfig(hardEmotions, 6, 4, StarCriteria(6, 40, 4, 60)) // 6 rondas, 4 opciones
            else -> LevelConfig(normalEmotions, 4, 4, StarCriteria(4, 20, 3, 40)) // Nivel 2
        }
    }

    // Lista de preguntas para esta sesión
    val questionList by remember {
        mutableStateOf(
            List(numRounds) { emotionPool.random(Random(System.currentTimeMillis() + it)) }
        )
    }

    // Emoción actual (la pregunta)
    var currentEmotion by remember { mutableStateOf(questionList[0]) }

    // Opciones de botones (se recalculan dinámicamente)
    val options by remember(currentEmotion, numOptions, emotionPool) {
        derivedStateOf {
            val correctAnswer = currentEmotion.name
            val incorrectAnswers = emotionPool
                .map { it.name }
                .filter { it != correctAnswer }
                .shuffled()
                .take(numOptions - 1)
            (incorrectAnswers + correctAnswer).shuffled()
        }
    }

    // Cronómetro
    LaunchedEffect(gameFinished, showInstructions) {
        if (!gameFinished && !showInstructions) {
            startTime?.let { start ->
                while (!gameFinished) {
                    delay(1000)
                    elapsedTime = System.currentTimeMillis() - start
                }
            }
        }
    }

    // Lógica de Juego (Game Loop)
    LaunchedEffect(currentRound) {
        if (currentRound > 0 && !gameFinished) { // Si se ha jugado una ronda (no en la ronda 0)

            showFeedback = true
            delay(1000)
            showFeedback = false

            if (currentRound >= numRounds) { // Si se completaron todas las rondas
                gameFinished = true
                val durationSeconds = (elapsedTime / 1000).toInt()

                // Extraer criterios
                val (score3, time3, score2, time2) = starCriteria

                // --- CORRECCIÓN 2: Usar .compareTo() ---
                stars = when {
                    // score == score3 && durationSeconds <= time3
                    score == score3 && durationSeconds.compareTo(time3) <= 0 -> 3

                    // score >= score2 && durationSeconds <= time2
                    score.compareTo(score2) >= 0 && durationSeconds.compareTo(time2) <= 0 -> 2

                    else -> 1
                }

            } else { // Si aún quedan rondas, avanzar a la siguiente
                currentEmotion = questionList[currentRound]
            }
        }
    }

    // --- UI ---
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "¡Encuentra la emoción! (Nivel $level)",
            style = MaterialTheme.typography.headlineMedium,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { contentDescription = "Título del juego de emociones Nivel $level" }
        )

        if (!showInstructions && !gameFinished) {
            Image(
                painter = painterResource(id = currentEmotion.drawableId),
                contentDescription = "Emoción ${currentEmotion.name}",
                modifier = Modifier
                    .size(150.dp)
                    .semantics { contentDescription = "Imagen de la emoción ${currentEmotion.name}" }
            )

            // Opciones
            options.forEach { option ->
                Button(
                    onClick = {
                        if (!gameFinished && !showFeedback) {
                            isCorrect = option == currentEmotion.name
                            if (isCorrect) {
                                score += 1
                            }
                            currentRound++ // Avanza a la siguiente ronda
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = ButtonDefaults.buttonColors( containerColor = MaterialTheme.colorScheme.primary ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !gameFinished && !showFeedback
                ) {
                    Text( text = option, fontSize = 18.sp, fontWeight = FontWeight.Medium )
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
                    color = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Puntuación actual
            Text(
                text = "Progreso: $currentRound / $numRounds",
                fontSize = 18.sp,
            )
        }

        // Mensaje final al terminar
        AnimatedVisibility(
            visible = gameFinished,
            enter = fadeIn() + scaleIn(),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize().padding(16.dp)
            ) {
                Text( "🎉 ¡Nivel $level completado!", style = MaterialTheme.typography.headlineMedium )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Aciertos: $score de $numRounds",
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tiempo: ${elapsedTime / 1000} s",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row { repeat(stars) { Text( "⭐", style = MaterialTheme.typography.headlineLarge ) } }
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when (stars) {
                        3 -> "¡Excelente! Todo correcto y rápido 🎯"
                        2 -> "Muy bien, pero puedes mejorar ⏳"
                        else -> "¡Sigue practicando! 💪"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val timeTaken = if (startTime == null) 0L else (System.currentTimeMillis() - startTime!!)
                        // Llama al callback con los resultados (numRounds es el total de "intentos")
                        onGameEnd(stars, timeTaken, numRounds, level)
                    },
                    modifier = Modifier.semantics { contentDescription = "Botón para volver al menú" }
                ) {
                    Text("Volver al Menú")
                }
            }
        }
    }

    // Modal de instrucciones
    if (showInstructions) {
        val (score3, time3, score2, time2) = starCriteria
        AlertDialog(
            onDismissRequest = { /* No cerrar */ },
            confirmButton = {
                Button(onClick = {
                    showInstructions = false
                    startTime = System.currentTimeMillis()
                }) {
                    Text("Comenzar Nivel $level ▶️")
                }
            },
            title = { Text("📖 Instrucciones (Nivel $level)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Encuentra la emoción correcta. Jugarás $numRounds rondas.", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Criterios para estrellas:", style = MaterialTheme.typography.labelLarge)
                    Text("⭐ 3 estrellas → $score3 aciertos (≤$time3 s)", style = MaterialTheme.typography.bodyMedium)
                    Text("⭐ 2 estrellas → $score2 aciertos (≤$time2 s)", style = MaterialTheme.typography.bodyMedium)
                    Text("⭐ 1 estrella → ¡Completado!", style = MaterialTheme.typography.bodyMedium)
                }
            }
        )
    }
}