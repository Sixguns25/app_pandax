package com.tesis.aplicacionpandax.ui.games

import android.Manifest // Para permisos
import android.app.Activity // Para resultado de Activity
import android.content.Intent // Para Speech Recognizer
import android.media.MediaPlayer // QUITA: Ya no es necesario si no usas player
import android.media.MediaRecorder // QUITA: Ya no es necesario si no usas recorder
import android.media.SoundPool
import android.speech.RecognizerIntent // Para Speech Recognizer
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape // Para botón de mic
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesis.aplicacionpandax.R
import com.tesis.aplicacionpandax.data.entity.GameSession
import com.tesis.aplicacionpandax.repository.ProgressRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.* // Para Locale

// --- Data class (debe estar fuera del Composable) ---
// (Asegúrate que esta sea la única definición de 'Word' en este archivo)
data class Word(val text: String, val imageRes: Int, val soundRes: Int) // Añadir soundRes

// --- Firma de la Función Actualizada ---
@Composable
fun PronunciationGame(
    childUserId: Long,
    progressRepo: ProgressRepository,
    level: Int, // <-- Parámetro de nivel
    onGameEnd: (score: Int, timeTaken: Long, attempts: Int, level: Int) -> Unit // <-- Callback actualizado
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() } // Para mostrar errores

    // --- Lógica de Niveles y Sonidos ---

    // Define los pools de palabras
    // ¡¡IMPORTANTE!!: Reemplaza los R.drawable y R.raw placeholders
    // con tus propios recursos.
    val level1Words = remember { listOf(
        Word("sol", R.drawable.sun, R.raw.word_sun), // -
        Word("pan", R.drawable.bread, R.raw.word_bread), // Placeholder -
        Word("tren", R.drawable.train, R.raw.word_train), // Placeholder -
        Word("flor", R.drawable.flower, R.raw.word_flower) // Placeholder
    )}
    val level2Words = remember { listOf(
        Word("casa", R.drawable.house, R.raw.word_house), // -
        Word("perro", R.drawable.dog, R.raw.word_dog), // -
        Word("gato", R.drawable.cat, R.raw.word_cat), // -
        Word("árbol", R.drawable.tree, R.raw.word_tree), // -
    )}
    val level3Words = remember { listOf(
        Word("elefante", R.drawable.elephant, R.raw.word_elephant), // Placeholder -
        Word("plátano", R.drawable.banana, R.raw.word_banana), // Placeholder -
        Word("bicicleta", R.drawable.bicycle, R.raw.word_bicycle) // Placeholder -
    )}

    // Selecciona el pool de palabras basado en el nivel
    val wordPool = remember(level) {
        when (level) {
            1 -> level1Words
            3 -> level3Words
            else -> level2Words // Nivel 2 por defecto
        }.shuffled()
    }

    val numRounds = wordPool.size

    // Carga dinámica de sonidos
    val soundPool = remember { SoundPool.Builder().setMaxStreams(2).build() }
    val soundResources = remember(wordPool) {
        val resources = wordPool.associate { it.text to soundPool.load(context, it.soundRes, 1) }
        resources + mapOf(
            "correct" to soundPool.load(context, R.raw.correct, 1),
            "wrong" to soundPool.load(context, R.raw.wrong, 1)
        )
    }

    // Permiso de grabación (necesario para Speech Recognizer)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Log.d("PronunciationGame", "Permiso de grabación denegado")
            scope.launch { snackbarHostState.showSnackbar("Necesito permiso para usar el micrófono.") }
        }
    }

    // --- Estados del Juego ---
    var currentWordIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) } // Aciertos
    var attempts by remember { mutableStateOf(0) } // Intentos de habla
    var startTime by remember { mutableStateOf<Long?>(null) }
    var gameActive by remember { mutableStateOf(false) }
    var showInstructions by remember { mutableStateOf(true) }
    var gameFinished by remember { mutableStateOf(false) }
    var stars by remember { mutableStateOf(0) }
    var canInteract by remember { mutableStateOf(true) }
    var feedbackText by remember { mutableStateOf("") }
    var showFeedback by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }

    // Animaciones
    val scale by animateFloatAsState( targetValue = if (gameActive) 1f else 0f, animationSpec = tween(durationMillis = 500), label = "" )

    // --- Lógica de Reconocimiento de Voz ---

    // Función para procesar el resultado
    val handleSpeechResult: (String?) -> Unit = { spokenText ->
        attempts++
        val currentWord = wordPool[currentWordIndex].text

        if (spokenText == null) {
            isCorrect = false
            feedbackText = "No te escuché. ¡Intenta otra vez!"
        } else if (spokenText.equals(currentWord, ignoreCase = true)) {
            // Acierto
            isCorrect = true
            score++
            feedbackText = "¡Correcto! Dijiste: '$spokenText'"
            soundPool.play(soundResources["correct"] ?: 0, 1f, 1f, 0, 0, 1f)
            // Avanza automáticamente
            scope.launch {
                showFeedback = true
                delay(2000) // Muestra feedback
                showFeedback = false
                feedbackText = ""
                if (currentWordIndex + 1 >= numRounds) { // Si fue la última palabra
                    gameFinished = true
                } else {
                    currentWordIndex++ // Siguiente palabra
                    canInteract = true
                }
            }
        } else {
            // Error
            isCorrect = false
            feedbackText = "Escuché: '$spokenText'. ¡Intenta otra vez!"
            soundPool.play(soundResources["wrong"] ?: 0, 1f, 1f, 0, 0, 1f)
        }

        showFeedback = true
        if (!isCorrect) {
            canInteract = true // Permite reintentar si se equivocó
        }
    }

    // Lanzador del Activity de Reconocimiento de Voz
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val spokenWords = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            handleSpeechResult(spokenWords?.get(0))
        } else {
            Log.d("PronunciationGame", "Reconocimiento cancelado o fallido.")
            canInteract = true // Reactiva el botón
        }
    }

    // Función para iniciar el reconocimiento
    fun launchSpeechRecognizer() {
        canInteract = false
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla la palabra \"${wordPool[currentWordIndex].text}\"...")
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e("PronunciationGame", "Error al lanzar reconocimiento: ${e.message}")
            scope.launch { snackbarHostState.showSnackbar("Error: No se pudo iniciar el micrófono.") }
            canInteract = true
        }
    }

    // --- Lógica del Juego ---

    // Función para repetir el audio
    fun repeatWord() {
        if (gameActive && currentWordIndex < numRounds) {
            val currentWord = wordPool[currentWordIndex]
            soundPool.play(soundResources[currentWord.text] ?: 0, 1f, 1f, 0, 0, 1f)
        }
    }

    // Reproducir sonido al cambiar de palabra
    LaunchedEffect(currentWordIndex, gameActive) {
        if (gameActive && currentWordIndex < numRounds) {
            delay(500)
            repeatWord()
        }
    }

    // Calcular estrellas y guardar sesión AL FINALIZAR
    LaunchedEffect(gameFinished) {
        if (gameFinished) {
            val timeTaken = System.currentTimeMillis() - (startTime ?: System.currentTimeMillis())

            // Calcular estrellas basado en aciertos
            stars = when {
                score == numRounds -> 3 // 100%
                score >= (numRounds * 0.6).toInt() -> 2 // 60%+
                else -> 1 // Menos del 60%
            }
            Log.d("PronunciationGame", "Fin Nivel $level: score=$score, attempts=$attempts, timeTaken=$timeTaken, stars=$stars")

            // Guardar sesión
            scope.launch {
                try {
                    progressRepo.saveSession(
                        GameSession(
                            childUserId = childUserId,
                            gameType = "PRONUNCIATION",
                            stars = stars,
                            timeTaken = timeTaken,
                            attempts = attempts, // Intentos de habla
                            level = level // Guardar el nivel
                        )
                    )
                    Log.d("PronunciationGame", "Sesión Nivel $level guardada")
                } catch (e: Exception) {
                    Log.e("PronunciationGame", "Error al guardar sesión: ${e.message}")
                }
            }
        }
    }

    // --- UI de la Pantalla ---
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween // Distribuye espacio
        ) {
            // --- Pantalla de Instrucciones ---
            if (showInstructions) {
                AlertDialog(
                    onDismissRequest = { /* No cerrar */ },
                    confirmButton = {
                        Button(onClick = {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) // Pide permiso
                            showInstructions = false
                            gameActive = true
                            startTime = System.currentTimeMillis()
                        }) {
                            Text("Comenzar Nivel $level ▶️")
                        }
                    },
                    title = { Text("Instrucciones (Nivel $level)") },
                    text = {
                        Column {
                            Text("Escucha la palabra que dice la app.")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Presiona el botón 🎙️ y repite la palabra en voz alta.")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("¡La app te dirá si lo hiciste bien!")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Jugarás con $numRounds palabras.")
                        }
                    }
                )
            }
            // --- Pantalla de Fin de Juego ---
            else if (gameFinished) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text("¡Juego Terminado! (Nivel $level)", fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Aciertos: $score / $numRounds", fontSize = 20.sp)
                    Text("Intentos de Habla: $attempts", fontSize = 18.sp)
                    Text("Estrellas: ${"⭐".repeat(stars)}", fontSize = 30.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            val timeTaken = System.currentTimeMillis() - (startTime ?: System.currentTimeMillis())
                            onGameEnd(stars, timeTaken, attempts, level) // Pasa 'attempts' (intentos de habla)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Volver al Menú", fontSize = 18.sp)
                    }
                }
            }
            // --- Pantalla de Juego Activo ---
            else {
                // Sección Superior: Progreso
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Aciertos: $score", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Palabra ${currentWordIndex + 1} de $numRounds", fontSize = 18.sp)
                }

                // Sección Central: Juego
                AnimatedVisibility(visible = gameActive, enter = fadeIn(), exit = fadeOut()) {
                    Column(
                        modifier = Modifier.scale(scale),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (currentWordIndex < wordPool.size) {
                            val currentWord = wordPool[currentWordIndex]
                            // Palabra e Imagen
                            Text(
                                currentWord.text.uppercase(),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Image(
                                painter = painterResource(id = currentWord.imageRes),
                                contentDescription = currentWord.text,
                                modifier = Modifier.size(200.dp).padding(vertical = 16.dp)
                            )
                        }
                    }
                }

                // Sección Media: Feedback de Voz
                AnimatedVisibility(visible = showFeedback, enter = fadeIn()) {
                    Text(
                        text = feedbackText,
                        color = if (isCorrect) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Sección Inferior: Botones de Acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón Hablar (Micrófono)
                    Button(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            launchSpeechRecognizer()
                        },
                        enabled = canInteract,
                        modifier = Modifier
                            .size(100.dp)
                            .padding(8.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            Icons.Filled.Mic,
                            contentDescription = "Hablar",
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    // Botón Repetir Palabra
                    OutlinedButton(
                        onClick = { repeatWord() },
                        enabled = canInteract,
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            Icons.Filled.VolumeUp,
                            contentDescription = "Repetir Palabra",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } // Fin Row Botones
            } // Fin else (juego activo)
        } // Fin Column principal
    } // Fin Scaffold

    // --- CORRECCIÓN AQUÍ ---
    // Liberar recursos
    DisposableEffect(Unit) {
        onDispose {
            soundPool.release()
            // QUITA: player?.release()
            // QUITA: recorder?.release()
        }
    }
}