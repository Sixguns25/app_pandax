package com.tesis.aplicacionpandax.ui.games

import android.media.SoundPool
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures // Importar detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale // Importar scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity // Importar LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesis.aplicacionpandax.R
import com.tesis.aplicacionpandax.data.entity.GameSession
import com.tesis.aplicacionpandax.repository.ProgressRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// --- PASO 1: Modificar Firma ---
@Composable
fun CoordinationGame(
    childUserId: Long,
    progressRepo: ProgressRepository,
    level: Int, // <-- Nivel añadido
    onGameEnd: (score: Int, timeTaken: Long, attempts: Int, level: Int) -> Unit // <-- Level añadido a onGameEnd
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Sonidos (sin cambios)
    val soundPool = remember { SoundPool.Builder().setMaxStreams(3).build() }
    val soundCorrect = remember { soundPool.load(context, R.raw.correct, 1) }
    val soundWrong = remember { soundPool.load(context, R.raw.wrong, 1) }

    // --- PASO 1 (cont.): Definir Propiedades del Nivel ---
    val (targetRadiusDp, targetVisibilityMillis, hasDistractors) = remember(level) {
        when (level) {
            1 -> Triple(40.dp, 1500L, false) // Nivel 1: Grande, Lento, Sin distractores
            2 -> Triple(25.dp, 1000L, false) // Nivel 2: Medio, Normal
            3 -> Triple(15.dp, 750L, true)   // Nivel 3: Pequeño, Rápido, Con distractores
            else -> Triple(25.dp, 1000L, false) // Default Nivel 2
        }
    }
    val targetRadiusPx = with(LocalDensity.current) { targetRadiusDp.toPx() } // Radio en Px
    val distractorRadiusPx = 20f // Radio fijo para distractores

    // --- PASO 1 (cont.): Estados del Juego Actualizados ---
    var score by remember { mutableStateOf(0) } // Aciertos al objetivo
    var attempts by remember { mutableStateOf(0) } // Clics totales
    var targetPosition by remember { mutableStateOf<Offset?>(null) } // Target (puede ser null)
    var distractors by remember { mutableStateOf<List<Offset>>(emptyList()) } // Lista de distractores
    var startTime by remember { mutableStateOf<Long?>(null) }
    var timeRemaining by remember { mutableStateOf(30_000L) } // 30 segundos
    var gameActive by remember { mutableStateOf(false) } // Indica si el juego está corriendo
    var showInstructions by remember { mutableStateOf(true) }
    var gameFinished by remember { mutableStateOf(false) }
    var stars by remember { mutableStateOf(0) }
    var feedbackColor by remember { mutableStateOf(Color.Transparent) }
    var canTap by remember { mutableStateOf(true) } // Controlar toques rápidos

    // Estado para dimensiones del Canvas
    var canvasWidth by remember { mutableStateOf(0f) }
    var canvasHeight by remember { mutableStateOf(0f) }


    // Animaciones (sin cambios)
    val scale by animateFloatAsState(
        targetValue = if (gameActive) 1f else 0f,
        animationSpec = tween(durationMillis = 500), label = ""
    )

    // --- PASO 2: Ajustar Lógica del Juego ---

    // Temporizador principal del juego (30 segundos)
    LaunchedEffect(gameActive) {
        if (gameActive && !gameFinished) {
            startTime = System.currentTimeMillis()
            var elapsed = 0L
            while (elapsed < 30_000L && gameActive) {
                delay(100L)
                // Asegurarse que startTime no sea null antes de calcular
                startTime?.let { start ->
                    elapsed = System.currentTimeMillis() - start
                    timeRemaining = (30_000L - elapsed).coerceAtLeast(0L)
                } ?: run {
                    // Si startTime es null inesperadamente, detener el juego
                    Log.e("CoordinationGame", "startTime es null durante el juego activo!")
                    gameActive = false
                }
            }
            if (gameActive) { // Si el tiempo se acabó y el juego seguía activo
                gameActive = false
                gameFinished = true
            }
        }
    }

    // Lógica para mostrar/ocultar Target y Distractores
    LaunchedEffect(gameActive, targetPosition, canvasWidth, canvasHeight) {
        if (gameActive && canvasWidth > 0f && canvasHeight > 0f) { // Asegura que las dimensiones sean válidas
            // Si NO hay un target visible actualmente, crea uno nuevo
            if (targetPosition == null) {
                delay(Random.nextLong(200, 500)) // Pausa aleatoria antes de aparecer

                // Nueva posición aleatoria asegurando que cabe dentro del canvas
                val minX = targetRadiusPx
                val maxX = canvasWidth - targetRadiusPx
                val minY = targetRadiusPx
                val maxY = canvasHeight - targetRadiusPx

                // Verifica que los límites sean válidos antes de generar
                if (maxX > minX && maxY > minY) {
                    targetPosition = Offset(
                        x = Random.nextFloat() * (maxX - minX) + minX,
                        y = Random.nextFloat() * (maxY - minY) + minY
                    )

                    // Generar distractores si corresponde
                    if (hasDistractors) {
                        val numDistractors = Random.nextInt(1, 4)
                        val newDistractors = mutableListOf<Offset>()
                        val distractorMinX = distractorRadiusPx
                        val distractorMaxX = canvasWidth - distractorRadiusPx
                        val distractorMinY = distractorRadiusPx
                        val distractorMaxY = canvasHeight - distractorRadiusPx

                        if (distractorMaxX > distractorMinX && distractorMaxY > distractorMinY) {
                            repeat(numDistractors) {
                                var distractorPos: Offset
                                var attempts = 0
                                do { // Intenta encontrar una posición no solapada
                                    distractorPos = Offset(
                                        x = Random.nextFloat() * (distractorMaxX - distractorMinX) + distractorMinX,
                                        y = Random.nextFloat() * (distractorMaxY - distractorMinY) + distractorMinY
                                    )
                                    attempts++
                                } while (
                                // Comprueba solapamiento con target y otros distractores (básico)
                                    (distractorPos - targetPosition!!).getDistance() < targetRadiusPx + distractorRadiusPx + 10f ||
                                    newDistractors.any { (distractorPos - it).getDistance() < distractorRadiusPx * 2 + 10f } &&
                                    attempts < 10 // Limita intentos para evitar bucle infinito
                                )
                                if(attempts < 10) newDistractors.add(distractorPos) // Añade si se encontró posición válida
                            }
                        }
                        distractors = newDistractors
                    } else {
                        distractors = emptyList() // Sin distractores
                    }

                    // Espera el tiempo de visibilidad y luego oculta
                    delay(targetVisibilityMillis)
                    if (gameActive) { // Solo oculta si el juego sigue activo
                        targetPosition = null
                        distractors = emptyList()
                    }
                } else {
                    Log.w("CoordinationGame", "Dimensiones del canvas inválidas para generar posición: $minX, $maxX, $minY, $maxY")
                    targetPosition = null // Resetea si las dimensiones son inválidas
                    distractors = emptyList()
                }
            } // Fin if targetPosition == null
        } // Fin if gameActive
    } // Fin LaunchedEffect


    // --- PASO 4: Ajustar Cálculo de Estrellas y Guardado (en su propio LaunchedEffect) ---
    LaunchedEffect(gameFinished) {
        if (gameFinished) {
            val timeTaken = System.currentTimeMillis() - (startTime ?: System.currentTimeMillis())

            // Calcular estrellas basado en nivel y aciertos (score)
            val scoreThreshold3Stars = when (level) { 1 -> 15; 2 -> 20; 3 -> 25; else -> 20 }
            val scoreThreshold2Stars = when (level) { 1 -> 8;  2 -> 10; 3 -> 15; else -> 10 }

            stars = when {
                score >= scoreThreshold3Stars -> 3
                score >= scoreThreshold2Stars -> 2
                else -> 1
            }
            Log.d("CoordinationGame", "Fin Nivel $level: score=$score, attempts=$attempts, timeTaken=$timeTaken, stars=$stars")

            // Guardar sesión (incluyendo el nivel)
            scope.launch {
                try {
                    progressRepo.saveSession(
                        GameSession(
                            childUserId = childUserId,
                            gameType = "COORDINATION",
                            stars = stars,
                            timeTaken = timeTaken,
                            attempts = attempts, // Guarda intentos totales (clics)
                            level = level // <-- Guardar el nivel
                        )
                    )
                    Log.d("CoordinationGame", "Sesión guardada para Nivel $level")
                } catch (e: Exception) {
                    Log.e("CoordinationGame", "Error al guardar sesión: ${e.message}")
                }
            } // Fin launch guardado
        } // Fin if gameFinished
    } // Fin LaunchedEffect(gameFinished)


    // --- PASO 5: Actualizar Instrucciones y Pantalla Final ---
    Scaffold( // Usar Scaffold para estructura
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            if (showInstructions) {
                AlertDialog(
                    onDismissRequest = { /* No cerrar */ },
                    confirmButton = {
                        Button(onClick = {
                            showInstructions = false
                            gameActive = true
                            // startTime se inicializa en su LaunchedEffect
                        }) {
                            Text("Comenzar Nivel $level ▶️") // Muestra nivel
                        }
                    },
                    title = { Text("Instrucciones (Nivel $level)") }, // Muestra nivel
                    text = {
                        Column {
                            Text("Toca los círculos ROJOS antes de que desaparezcan.")
                            if (hasDistractors) { // Nota sobre distractores
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("¡CUIDADO! No toques los círculos AZULES.", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("¡Tienes 30 segundos!")
                        }
                    }
                )
            } else if (gameFinished) {
                // Pantalla Final
                Column(
                    modifier = Modifier.fillMaxSize(), // Ocupa espacio
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center // Centra contenido
                ) {
                    Text("¡Juego Terminado! (Nivel $level)", fontSize = 24.sp, fontWeight = FontWeight.Bold) // Muestra nivel
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Aciertos: $score", fontSize = 20.sp) // Cambiado 'Puntaje' a 'Aciertos'
                    Text("Clics Totales: $attempts", fontSize = 20.sp) // Cambiado 'Intentos' a 'Clics Totales'
                    Text("Estrellas: ${"⭐".repeat(stars)}", fontSize = 30.sp) // Estrellas más grandes
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            // Llamar a onGameEnd pasando el level
                            val timeTaken = System.currentTimeMillis() - (startTime ?: System.currentTimeMillis())
                            onGameEnd(stars, timeTaken, attempts, level) // <-- Pasar level
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Volver al Menú", fontSize = 18.sp)
                    }
                }
            } else { // Juego activo
                // --- Información durante el juego ---
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tiempo: ${timeRemaining / 1000}s", fontSize = 20.sp, fontWeight = FontWeight.Medium)
                    Text("Aciertos: $score", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))

                // --- PASO 3: Actualizar Canvas y Lógica de Toque ---
                // AnimatedVisibility para el Canvas
                AnimatedVisibility(visible = gameActive, enter = fadeIn(), exit = fadeOut()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize() // Ocupa el espacio restante
                            .scale(scale) // Aplica animación de escala
                            .pointerInput(gameActive, canTap) { // Relanzar si cambia gameActive o canTap
                                detectTapGestures { tapOffset ->
                                    if (gameActive && canTap) {
                                        canTap = false
                                        attempts++ // Registrar clic

                                        var hitTarget = false
                                        var hitDistractor = false

                                        // 1. ¿Tocó el objetivo?
                                        targetPosition?.let { currentTargetPos ->
                                            if ((tapOffset - currentTargetPos).getDistance() < targetRadiusPx) {
                                                hitTarget = true
                                                score++
                                                soundPool.play(soundCorrect, 1f, 1f, 0, 0, 1f)
                                                feedbackColor = Color.Green
                                                targetPosition = null // Oculta al acertar
                                                distractors = emptyList()
                                            }
                                        }

                                        // 2. ¿Tocó un distractor? (Solo si hay y no se tocó el target)
                                        if (hasDistractors && !hitTarget) {
                                            var touchedDistractorIndex = -1
                                            distractors.forEachIndexed { index, distractorPos ->
                                                if ((tapOffset - distractorPos).getDistance() < distractorRadiusPx) {
                                                    hitDistractor = true
                                                    touchedDistractorIndex = index
                                                }
                                            }
                                            if(hitDistractor){
                                                // Penalización (opcional): score = (score - 1).coerceAtLeast(0)
                                                soundPool.play(soundWrong, 1f, 1f, 0, 0, 1f)
                                                feedbackColor = Color.Magenta // Feedback para distractor
                                                // Opcional: Remover el distractor tocado
                                                // distractors = distractors.filterIndexed { index, _ -> index != touchedDistractorIndex }
                                            }
                                        }

                                        // 3. Falló (no tocó nada relevante)
                                        if (!hitTarget && !hitDistractor) {
                                            soundPool.play(soundWrong, 1f, 1f, 0, 0, 1f)
                                            feedbackColor = Color.Red
                                        }

                                        // Resetea feedback y permite taps
                                        scope.launch {
                                            delay(300)
                                            feedbackColor = Color.Transparent
                                            // Solo habilita taps si el target se ocultó (acierto) o si no hubo acierto
                                            if (hitTarget || (!hitTarget && !hitDistractor) || (hitDistractor)) {
                                                canTap = true
                                            }
                                            // Si falló pero el target sigue visible, no necesita hacer canTap = true aquí,
                                            // porque el LaunchedEffect(gameActive, targetPosition) lo volverá a activar
                                            // después de ocultar el target fallado.
                                        }
                                    } // Fin if (gameActive && canTap)
                                } // Fin detectTapGestures
                            } // Fin pointerInput
                            .semantics { contentDescription = "Juego de Coordinación Nivel $level" }
                    ) {
                        // Guarda las dimensiones del canvas
                        canvasWidth = size.width
                        canvasHeight = size.height

                        // Dibuja feedback de fondo
                        drawRect( color = feedbackColor, size = size, alpha = 0.3f )

                        // Dibuja el OBJETIVO (si existe)
                        targetPosition?.let { pos ->
                            drawCircle( color = Color.Red, radius = targetRadiusPx, center = pos )
                        }

                        // Dibuja los DISTRACTORES (si existen)
                        distractors.forEach { pos ->
                            drawCircle( color = Color.Blue, radius = distractorRadiusPx, center = pos )
                        }
                    } // Fin Canvas
                } // Fin AnimatedVisibility
            } // Fin else (juego activo)
        } // Fin Column principal
    } // Fin Scaffold

    // Liberar SoundPool (sin cambios)
    DisposableEffect(Unit) {
        onDispose { soundPool.release() }
    }
}