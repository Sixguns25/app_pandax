package com.tesis.aplicacionpandax.ui.games

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesis.aplicacionpandax.data.entity.GameSession
import com.tesis.aplicacionpandax.repository.ProgressRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import android.media.SoundPool
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.scale
import com.tesis.aplicacionpandax.R

@Composable
fun CoordinationGame(
    childUserId: Long,
    progressRepo: ProgressRepository,
    onGameEnd: (score: Int, timeTaken: Long, attempts: Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Sonidos
    val soundPool = remember { SoundPool.Builder().setMaxStreams(3).build() }
    val soundCorrect = remember { soundPool.load(context, R.raw.correct, 1) }
    val soundWrong = remember { soundPool.load(context, R.raw.wrong, 1) }

    var score by remember { mutableStateOf(0) }
    var attempts by remember { mutableStateOf(0) }
    var targetPosition by remember { mutableStateOf(Offset(100f, 100f)) }
    var startTime by remember { mutableStateOf<Long?>(null) }
    var timeRemaining by remember { mutableStateOf(30_000L) } // 30 segundos
    var gameActive by remember { mutableStateOf(false) }
    var showInstructions by remember { mutableStateOf(true) }
    var gameFinished by remember { mutableStateOf(false) }
    var stars by remember { mutableStateOf(0) }
    var feedbackColor by remember { mutableStateOf(Color.Transparent) }
    var canTap by remember { mutableStateOf(true) } // Controlar toques rápidos

    // Animaciones
    val scale by animateFloatAsState(
        targetValue = if (gameActive) 1f else 0f,
        animationSpec = tween(durationMillis = 500)
    )

    // Temporizador
    LaunchedEffect(gameActive) {
        if (gameActive) {
            startTime = System.currentTimeMillis()
            while (timeRemaining > 0) {
                delay(100L)
                timeRemaining -= 100L
            }
            gameActive = false
            gameFinished = true
            val timeTaken = System.currentTimeMillis() - startTime!!
            // Calcular estrellas
            stars = when {
                (score >= 20 && timeTaken <= 31000) || (score == attempts && score >= 15) -> 3
                score >= 10 -> 2
                else -> 1
            }
            Log.d("CoordinationGame", "Fin del juego: score=$score, attempts=$attempts, timeTaken=$timeTaken, stars=$stars")
            scope.launch {
                progressRepo.saveSession(
                    GameSession(
                        childUserId = childUserId,
                        gameType = "COORDINATION",
                        stars = stars,
                        timeTaken = timeTaken,
                        attempts = attempts,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        if (showInstructions) {
            AlertDialog(
                onDismissRequest = { },
                confirmButton = {
                    Button(onClick = {
                        showInstructions = false
                        gameActive = true
                    }) {
                        Text("Comenzar")
                    }
                },
                title = { Text("Instrucciones") },
                text = { Text("Toca los círculos rojos antes de que desaparezcan. ¡Tienes 30 segundos!") }
            )
        } else if (gameFinished) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("¡Juego Terminado!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Puntaje: $score", fontSize = 20.sp)
                Text("Intentos: $attempts", fontSize = 20.sp)
                Text("Estrellas: ${"⭐".repeat(stars)}", fontSize = 20.sp)
                Button(onClick = {
                    val timeTaken = System.currentTimeMillis() - startTime!!
                    onGameEnd(stars, timeTaken, attempts)
                }) {
                    Text("Volver al Menú")
                }
            }
        } else {
            Text("Tiempo restante: ${timeRemaining / 1000}s", fontSize = 20.sp)
            Text("Puntaje: $score", fontSize = 20.sp, fontWeight = FontWeight.Bold)

            AnimatedVisibility(visible = gameActive, enter = fadeIn(), exit = fadeOut()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(scale)
                        .pointerInput(Unit) {
                            if (gameActive && canTap) {
                                detectTapGestures { tapOffset ->
                                    canTap = false // Deshabilitar toques hasta terminar el feedback
                                    attempts++
                                    val distance = (tapOffset - targetPosition).getDistance()
                                    if (distance < 50f) {
                                        score++
                                        soundPool.play(soundCorrect, 1f, 1f, 0, 0, 1f)
                                        feedbackColor = Color.Green
                                    } else {
                                        soundPool.play(soundWrong, 1f, 1f, 0, 0, 1f)
                                        feedbackColor = Color.Red
                                    }
                                    targetPosition = Offset(
                                        x = Random.nextFloat() * 800f + 50f,
                                        y = Random.nextFloat() * 1200f + 50f
                                    )
                                    scope.launch {
                                        delay(500) // Pausa para feedback visual
                                        feedbackColor = Color.Transparent
                                        canTap = true // Rehabilitar toques
                                    }
                                }
                            }
                        }
                        .semantics { contentDescription = "Juego de Coordinación" }
                ) {
                    drawRect(
                        color = feedbackColor,
                        size = size,
                        alpha = 0.3f
                    )
                    drawCircle(
                        color = Color.Red,
                        radius = 25.dp.toPx(),
                        center = targetPosition
                    )
                }
            }
        }
    }

    // Liberar SoundPool
    DisposableEffect(Unit) {
        onDispose { soundPool.release() }
    }
}