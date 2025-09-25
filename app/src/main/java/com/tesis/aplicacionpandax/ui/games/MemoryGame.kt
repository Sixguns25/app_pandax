package com.tesis.aplicacionpandax.ui.games

import android.media.SoundPool
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tesis.aplicacionpandax.R
import com.tesis.aplicacionpandax.repository.ProgressRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun MemoryGame(
    childUserId: Long,
    progressRepo: ProgressRepository,
    onGameEnd: (Int, Long, Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Sonidos
    val soundPool = remember { SoundPool.Builder().setMaxStreams(3).build() }
    val soundCorrect = remember { soundPool.load(context, R.raw.correct, 1) }
    val soundWrong = remember { soundPool.load(context, R.raw.wrong, 1) }
    val soundComplete = remember { soundPool.load(context, R.raw.complete, 1) }

    // Lista de cartas (pares de emojis)
    val emojis = listOf("🐶", "🐱", "🐰", "🐸")
    val cards = remember { (emojis + emojis).shuffled(Random(System.currentTimeMillis())).toMutableList() }

    // Estado del juego
    var flippedCards by remember { mutableStateOf(listOf<Int>()) }
    var matchedCards by remember { mutableStateOf(setOf<Int>()) }
    var attempts by remember { mutableStateOf(0) }
    var gameFinished by remember { mutableStateOf(false) }
    var stars by remember { mutableStateOf(0) }
    var isProcessing by remember { mutableStateOf(false) }

    // Estado del modal de inicio
    var showInstructions by remember { mutableStateOf(true) }

    // Tiempo de inicio y cronómetro
    var startTime by remember { mutableStateOf<Long?>(null) }
    var elapsedTime by remember { mutableStateOf(0L) }

    // Actualizar cronómetro cada segundo
    LaunchedEffect(gameFinished, showInstructions) {
        if (!gameFinished && !showInstructions) {
            startTime?.let {
                while (true) {
                    delay(1000)
                    elapsedTime = System.currentTimeMillis() - it
                }
            }
        }
    }

    // Log para depurar estado
    LaunchedEffect(flippedCards, matchedCards, attempts, gameFinished) {
        Log.d("MemoryGame", "Estado: flippedCards=$flippedCards, matchedCards=$matchedCards, matchedCards.size=${matchedCards.size}, cards.size=${cards.size}, attempts=$attempts, gameFinished=$gameFinished")
    }

    // Detectar fin del juego y calcular estrellas
    LaunchedEffect(matchedCards) {
        if (matchedCards.size == cards.size && !gameFinished) {
            Log.d("MemoryGame", "Juego terminado: matchedCards.size=${matchedCards.size}, cards.size=${cards.size}")
            gameFinished = true
            val durationSeconds = (elapsedTime / 1000).toInt()
            val pairs = cards.size / 2

            // Calcular estrellas
            stars = when {
                attempts <= pairs * 2 && durationSeconds <= 30 -> 3
                attempts <= pairs * 3 && durationSeconds <= 60 -> 2
                else -> 1
            }

            // Reproducir sonido de finalización
            soundPool.play(soundComplete, 1f, 1f, 0, 0, 1f)
        }
    }

    // Pantalla principal
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🎮 Juego de Memoria",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { contentDescription = "Título del Juego de Memoria" }
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (!showInstructions) {
            // Mostrar datos del juego
            Text(
                text = "Intentos: $attempts",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.semantics { contentDescription = "Intentos: $attempts" }
            )
            Text(
                text = "Tiempo: ${elapsedTime / 1000} s",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.semantics { contentDescription = "Tiempo transcurrido: ${elapsedTime / 1000} segundos" }
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Tablero 2x4
            Column {
                for (row in 0..1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (col in 0..3) {
                            val index = row * 4 + col
                            if (index < cards.size) {
                                MemoryCard(
                                    content = cards[index],
                                    visible = flippedCards.contains(index) || matchedCards.contains(index),
                                    isMatched = matchedCards.contains(index),
                                    onClick = {
                                        Log.d("MemoryGame", "Clic en carta $index, flippedCards=$flippedCards, matchedCards=$matchedCards")
                                        if (!gameFinished &&
                                            !isProcessing &&
                                            !flippedCards.contains(index) &&
                                            !matchedCards.contains(index) &&
                                            flippedCards.size < 2
                                        ) {
                                            flippedCards = flippedCards + index
                                            if (flippedCards.size == 2) {
                                                attempts++
                                                if (cards[flippedCards[0]] == cards[flippedCards[1]]) {
                                                    matchedCards = matchedCards + flippedCards
                                                    soundPool.play(soundCorrect, 1f, 1f, 0, 0, 1f)
                                                    flippedCards = emptyList()
                                                    Log.d("MemoryGame", "Acierto! Nuevo matchedCards=$matchedCards")
                                                } else {
                                                    isProcessing = true
                                                    soundPool.play(soundWrong, 1f, 1f, 0, 0, 1f)
                                                    scope.launch {
                                                        delay(1000)
                                                        flippedCards = emptyList()
                                                        isProcessing = false
                                                        Log.d("MemoryGame", "Error, cartas desvolteadas: flippedCards=$flippedCards")
                                                    }
                                                }
                                            }
                                        } else {
                                            Log.d("MemoryGame", "Clic ignorado: gameFinished=$gameFinished, isProcessing=$isProcessing, flippedCards=$flippedCards, matchedCards=$matchedCards")
                                        }
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Mensaje final con animación
            AnimatedVisibility(
                visible = gameFinished,
                enter = fadeIn() + scaleIn()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🎉 ¡Felicidades! Has completado en $attempts intentos",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { contentDescription = "Mensaje de finalización: Completado en $attempts intentos" }
                    )
                    Spacer(modifier = Modifier.height(8.dp))

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

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when (stars) {
                            3 -> "¡Excelente! Rápido y con pocos intentos 🎯"
                            2 -> "Muy bien, pero puedes mejorar ⏳"
                            else -> "¡Sigue practicando, lo importante es completar! 💪"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.semantics { contentDescription = "Mensaje de retroalimentación por estrellas" }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            onGameEnd(stars, elapsedTime, attempts)
                        },
                        modifier = Modifier.semantics { contentDescription = "Botón para volver al menú de juegos" }
                    ) {
                        Text("Volver al Menú")
                    }
                }
            }
        }
    }

    // Modal de instrucciones al inicio
    if (showInstructions) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {
                Button(
                    onClick = {
                        showInstructions = false
                        startTime = System.currentTimeMillis()
                        Log.d("MemoryGame", "Juego iniciado")
                    },
                    modifier = Modifier.semantics { contentDescription = "Botón para comenzar el juego" }
                ) {
                    Text("Comenzar ▶️")
                }
            },
            title = { Text("📖 Instrucciones") },
            text = {
                Column {
                    Text(
                        text = "Encuentra los pares de cartas iguales.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.semantics { contentDescription = "Instrucción: Encuentra los pares de cartas iguales" }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⭐ 3 estrellas → Rápido (≤30s) y pocos intentos",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { contentDescription = "Instrucción: 3 estrellas por rapidez y pocos intentos" }
                    )
                    Text(
                        text = "⭐ 2 estrellas → Tiempo ≤60s o algunos intentos extra",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { contentDescription = "Instrucción: 2 estrellas por tiempo moderado o más intentos" }
                    )
                    Text(
                        text = "⭐ 1 estrella → Completado con más intentos/tiempo",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { contentDescription = "Instrucción: 1 estrella por completar con más intentos o tiempo" }
                    )
                }
            }
        )
    }

    // Liberar SoundPool al salir
    DisposableEffect(Unit) {
        onDispose {
            soundPool.release()
            Log.d("MemoryGame", "SoundPool liberado")
        }
    }
}

@Composable
fun MemoryCard(
    content: String,
    visible: Boolean,
    isMatched: Boolean,
    onClick: () -> Unit
) {
    // Animación de giro
    val rotation by animateFloatAsState(
        targetValue = if (visible) 0f else 180f,
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .size(100.dp)
            .rotate(rotation)
            .background(
                color = if (isMatched) Color.Green else if (visible) Color.Cyan else Color.Gray,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .semantics { contentDescription = if (visible) "Carta visible: $content" else "Carta oculta" },
        contentAlignment = Alignment.Center
    ) {
        if (visible) {
            Text(
                text = content,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { contentDescription = "Emoji de la carta: $content" }
            )
        }
    }
}