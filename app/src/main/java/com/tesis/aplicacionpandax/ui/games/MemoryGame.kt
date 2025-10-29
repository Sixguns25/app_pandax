package com.tesis.aplicacionpandax.ui.games

import android.media.SoundPool
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer // Importar graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesis.aplicacionpandax.R
import com.tesis.aplicacionpandax.repository.ProgressRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// --- CAMBIO 1: Modificar la Firma ---
@Composable
fun MemoryGame(
    childUserId: Long,
    progressRepo: ProgressRepository,
    level: Int,
    onGameEnd: (Int, Long, Int, Int) -> Unit // (stars, timeTaken, attempts, level)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val soundPool = remember { SoundPool.Builder().setMaxStreams(3).build() }
    val soundCorrect = remember { soundPool.load(context, R.raw.correct, 1) }
    val soundWrong = remember { soundPool.load(context, R.raw.wrong, 1) }
    val soundComplete = remember { soundPool.load(context, R.raw.complete, 1) }

    val allEmojis = remember { listOf("🐶", "🐱", "🐰", "🐸", "🦁", "🐯", "🐼", "🐵", "🐘", "🦒", "🦓", "🐻").shuffled() }

    val (cards, columns) = remember(level) {
        val numPairs = when (level) {
            1 -> 2
            2 -> 4
            3 -> 6
            else -> 4
        }
        val gridColumns = when (level) {
            1 -> 2
            2 -> 4
            3 -> 4
            else -> 4
        }
        val selectedEmojis = allEmojis.take(numPairs)
        val cardList = (selectedEmojis + selectedEmojis).shuffled(Random(System.currentTimeMillis()))
        Pair(cardList, gridColumns)
    }

    var flippedCards by remember { mutableStateOf(listOf<Int>()) }
    var matchedCards by remember { mutableStateOf(setOf<Int>()) }
    var attempts by remember { mutableStateOf(0) }
    var gameFinished by remember { mutableStateOf(false) }
    var stars by remember { mutableStateOf(0) }
    var isProcessing by remember { mutableStateOf(false) }
    var showInstructions by remember { mutableStateOf(true) }
    var startTime by remember { mutableStateOf<Long?>(null) }
    var elapsedTime by remember { mutableStateOf(0L) }

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

    LaunchedEffect(matchedCards) {
        if (!gameFinished && matchedCards.size == cards.size && cards.isNotEmpty()) {
            Log.d("MemoryGame", "Juego terminado: Nivel $level, Pares=${cards.size / 2}, Intentos=$attempts, Tiempo=$elapsedTime")
            gameFinished = true

            val durationSeconds = (elapsedTime / 1000).toInt()
            val pairs = cards.size / 2

            val (perfectTime, perfectAttemptsFactor) = when (level) {
                1 -> Pair(15, 1.5)
                2 -> Pair(30, 1.5)
                3 -> Pair(60, 1.7)
                else -> Pair(30, 1.5)
            }
            val (goodTime, goodAttemptsFactor) = when (level) {
                1 -> Pair(25, 2.5)
                2 -> Pair(45, 2.0)
                3 -> Pair(90, 2.0)
                else -> Pair(45, 2.0)
            }
            val perfectAttemptsLimit = (pairs * perfectAttemptsFactor).toInt()
            val goodAttemptsLimit = (pairs * goodAttemptsFactor).toInt()

            stars = when {
                attempts <= perfectAttemptsLimit && durationSeconds <= perfectTime -> 3
                attempts <= goodAttemptsLimit && durationSeconds <= goodTime -> 2
                else -> 1
            }
            Log.d("MemoryGame", "Estrellas calculadas: $stars (Criterios Nivel $level: 3* <= $perfectAttemptsLimit/$perfectTime s, 2* <= $goodAttemptsLimit/$goodTime s)")

            soundPool.play(soundComplete, 1f, 1f, 0, 0, 1f)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎮 Juego de Memoria (Nivel $level)",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { contentDescription = "Título del Juego de Memoria Nivel $level" }
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (!showInstructions) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ){
                    Text( "Intentos: $attempts", style = MaterialTheme.typography.bodyLarge )
                    Text( "Tiempo: ${elapsedTime / 1000} s", style = MaterialTheme.typography.bodyLarge )
                }
                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (columns == 2) 32.dp else 0.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cards.size, key = { index -> index }) { index ->
                        MemoryCard(
                            content = cards[index],
                            visible = flippedCards.contains(index) || matchedCards.contains(index),
                            isMatched = matchedCards.contains(index),
                            onClick = {
                                if (!gameFinished && !isProcessing && !flippedCards.contains(index) && !matchedCards.contains(index) && flippedCards.size < 2) {
                                    flippedCards = flippedCards + index
                                    if (flippedCards.size == 2) {
                                        attempts++
                                        if (cards[flippedCards[0]] == cards[flippedCards[1]]) {
                                            matchedCards = matchedCards + flippedCards
                                            soundPool.play(soundCorrect, 1f, 1f, 0, 0, 1f)
                                            flippedCards = emptyList()
                                        } else {
                                            isProcessing = true
                                            soundPool.play(soundWrong, 1f, 1f, 0, 0, 1f)
                                            scope.launch {
                                                delay(1000)
                                                flippedCards = emptyList()
                                                isProcessing = false
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                AnimatedVisibility( visible = gameFinished, enter = fadeIn() + scaleIn() ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text( "🎉 ¡Felicidades! Nivel $level completado en $attempts intentos", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row { repeat(stars) { Text( "⭐", style = MaterialTheme.typography.headlineLarge ) } }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when (stars) {
                                3 -> "¡Excelente memoria! 🧠"
                                2 -> "¡Muy bien! Sigue así 👍"
                                else -> "¡Lo completaste! 🎉"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            // --- CAMBIO 2: Modificar la llamada a onClick ---
                            onClick = {
                                onGameEnd(stars, elapsedTime, attempts, level) // <-- Pasa el nivel aquí
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Volver al Menú")
                        }
                    }
                }
            }
        }
    }

    if (showInstructions) {
        AlertDialog(
            onDismissRequest = { /* No permitir cerrar */ },
            confirmButton = {
                Button(
                    onClick = {
                        showInstructions = false
                        startTime = System.currentTimeMillis()
                        Log.d("MemoryGame", "Juego Nivel $level iniciado")
                    }
                ) { Text("Comenzar Nivel $level ▶️") }
            },
            title = { Text("📖 Instrucciones (Nivel $level)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text( "Encuentra todos los pares de ${cards.size / 2} animales iguales.", style = MaterialTheme.typography.bodyLarge )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Criterios para estrellas:", style = MaterialTheme.typography.labelLarge)
                    Text( "⭐ 3 estrellas: ¡Muy rápido y pocos intentos!", style = MaterialTheme.typography.bodyMedium )
                    Text( "⭐ 2 estrellas: ¡Buen tiempo o pocos intentos!", style = MaterialTheme.typography.bodyMedium )
                    Text( "⭐ 1 estrella: ¡Completado!", style = MaterialTheme.typography.bodyMedium )
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose { soundPool.release() }
    }
}

@Composable
fun MemoryCard(
    content: String,
    visible: Boolean,
    isMatched: Boolean,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (visible || isMatched) 0f else 180f,
        label = "rotation"
    )
    val cardColor by animateColorAsState(
        targetValue = when {
            isMatched -> Color(0xFFC8E6C9)
            visible -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.primary
        }, label = "cardColor"
    )

    Card(
        modifier = Modifier
            .size(80.dp)
            .graphicsLayer { rotationY = rotation }
            .clickable(enabled = !isMatched && !visible) { onClick() },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (visible || isMatched) 2.dp else 4.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (rotation < 90f) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.headlineLarge,
                    fontSize = 40.sp,
                    color = MaterialTheme.colorScheme.onSurface // Ajusta el color si es necesario
                )
            }
        }
    }
}