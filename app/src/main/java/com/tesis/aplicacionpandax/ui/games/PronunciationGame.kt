package com.tesis.aplicacionpandax.ui.games

import android.Manifest
import android.media.MediaPlayer
import android.media.MediaRecorder
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesis.aplicacionpandax.R
import com.tesis.aplicacionpandax.data.entity.GameSession
import com.tesis.aplicacionpandax.repository.ProgressRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.media.SoundPool
import androidx.compose.ui.draw.scale
import java.io.File

@Composable
fun PronunciationGame(
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
    val soundWords = remember {
        mapOf(
            "sol" to soundPool.load(context, R.raw.word_sun, 1),
            "casa" to soundPool.load(context, R.raw.word_house, 1),
            "perro" to soundPool.load(context, R.raw.word_dog, 1),
            "gato" to soundPool.load(context, R.raw.word_cat, 1),
            "árbol" to soundPool.load(context, R.raw.word_tree, 1)
        )
    }

    // Permiso de grabación
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Log.d("PronunciationGame", "Permiso de grabación denegado")
        }
    }

    // MediaRecorder y MediaPlayer
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }

    // Lista de palabras
    val words = listOf(
        Word("sol", R.drawable.sun),
        Word("casa", R.drawable.house),
        Word("perro", R.drawable.dog),
        Word("gato", R.drawable.cat),
        Word("árbol", R.drawable.tree)
    )
    var currentWordIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var attempts by remember { mutableStateOf(0) }
    var startTime by remember { mutableStateOf<Long?>(null) }
    var gameActive by remember { mutableStateOf(false) }
    var showInstructions by remember { mutableStateOf(true) }
    var gameFinished by remember { mutableStateOf(false) }
    var stars by remember { mutableStateOf(0) }
    var feedbackColor by remember { mutableStateOf(Color.Transparent) }
    var canInteract by remember { mutableStateOf(true) }

    // Animaciones
    val scale by animateFloatAsState(
        targetValue = if (gameActive) 1f else 0f,
        animationSpec = tween(durationMillis = 500)
    )

    // Reproducir sonido de la palabra actual
    LaunchedEffect(currentWordIndex, gameActive) {
        if (gameActive && currentWordIndex < words.size) {
            soundPool.play(soundWords[words[currentWordIndex].text] ?: 0, 1f, 1f, 0, 0, 1f)
        }
    }

    // Función para repetir el audio
    fun repeatWord() {
        if (gameActive && currentWordIndex < words.size) {
            soundPool.play(soundWords[words[currentWordIndex].text] ?: 0, 1f, 1f, 0, 0, 1f)
        }
    }

    // Función para iniciar grabación
    fun startRecording() {
        if (!isRecording && canInteract) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            recordingFile = File(context.cacheDir, "recording_${System.currentTimeMillis()}.3gp")
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setOutputFile(recordingFile?.absolutePath)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                try {
                    prepare()
                    start()
                    isRecording = true
                    scope.launch {
                        delay(3000) // Graba máximo 3 segundos
                        if (isRecording) {
                            recorder?.apply {
                                try {
                                    stop()
                                    release()
                                } catch (e: Exception) {
                                    Log.e("PronunciationGame", "Error al detener grabación: ${e.message}")
                                }
                            }
                            recorder = null
                            isRecording = false
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PronunciationGame", "Error al grabar: ${e.message}")
                    isRecording = false
                }
            }
        }
    }

    // Función para detener grabación
    fun stopRecording() {
        if (isRecording) {
            recorder?.apply {
                try {
                    stop()
                    release()
                } catch (e: Exception) {
                    Log.e("PronunciationGame", "Error al detener grabación: ${e.message}")
                }
            }
            recorder = null
            isRecording = false
        }
    }

    // Función para reproducir grabación
    fun playRecording() {
        if (!isPlaying && recordingFile != null && canInteract) {
            player = MediaPlayer().apply {
                try {
                    setDataSource(recordingFile!!.absolutePath)
                    prepare()
                    start()
                    isPlaying = true
                    setOnCompletionListener {
                        isPlaying = false
                        release()
                        player = null
                    }
                } catch (e: Exception) {
                    Log.e("PronunciationGame", "Error al reproducir: ${e.message}")
                    isPlaying = false
                }
            }
        }
    }

    // Función para finalizar el juego
    fun endGame() {
        gameActive = false
        gameFinished = true
        val timeTaken = System.currentTimeMillis() - (startTime ?: System.currentTimeMillis())
        stars = when {
            (score >= 4 && timeTaken <= 31000) || (score == attempts && score >= 4) -> 3
            score >= 3 -> 2
            else -> 1
        }
        scope.launch {
            progressRepo.saveSession(
                GameSession(
                    childUserId = childUserId,
                    gameType = "PRONUNCIATION",
                    score = stars,
                    timeTaken = timeTaken,
                    attempts = attempts,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(feedbackColor.copy(alpha = 0.3f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        if (showInstructions) {
            AlertDialog(
                onDismissRequest = { },
                confirmButton = {
                    Button(onClick = {
                        showInstructions = false
                        gameActive = true
                        startTime = System.currentTimeMillis() // Inicializar startTime aquí
                    }) {
                        Text("Comenzar")
                    }
                },
                title = { Text("Instrucciones") },
                text = { Text("Escucha la palabra, graba tu voz diciendo la palabra, escúchala, y toca 'Correcto' si lo hiciste bien o 'Incorrecto' si quieres intentarlo de nuevo.") }
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
                    val timeTaken = System.currentTimeMillis() - (startTime ?: System.currentTimeMillis())
                    onGameEnd(stars, timeTaken, attempts)
                }) {
                    Text("Volver al Menú")
                }
            }
        } else {
            Text("Puntaje: $score", fontSize = 20.sp, fontWeight = FontWeight.Bold)

            AnimatedVisibility(visible = gameActive, enter = fadeIn(), exit = fadeOut()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(scale),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (currentWordIndex < words.size) {
                        Text(
                            words[currentWordIndex].text.uppercase(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Image(
                            painter = painterResource(id = words[currentWordIndex].imageRes),
                            contentDescription = words[currentWordIndex].text,
                            modifier = Modifier.size(150.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedButton(
                                onClick = { startRecording() },
                                enabled = !isRecording && canInteract
                            ) {
                                Text(if (isRecording) "Grabando..." else "Grabar")
                            }
                            OutlinedButton(
                                onClick = { playRecording() },
                                enabled = !isRecording && !isPlaying && recordingFile != null && canInteract
                            ) {
                                Text("Escuchar")
                            }
                            OutlinedButton(
                                onClick = { repeatWord() },
                                enabled = canInteract
                            ) {
                                Text("Repetir Palabra")
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (gameActive && canInteract) {
                                        canInteract = false
                                        attempts++
                                        score++
                                        soundPool.play(soundCorrect, 1f, 1f, 0, 0, 1f)
                                        feedbackColor = Color.Green
                                        scope.launch {
                                            delay(500)
                                            feedbackColor = Color.Transparent
                                            currentWordIndex++
                                            canInteract = true
                                            if (currentWordIndex >= words.size) {
                                                endGame()
                                            }
                                        }
                                    }
                                },
                                enabled = canInteract
                            ) {
                                Text("Correcto")
                            }
                            Button(
                                onClick = {
                                    if (gameActive && canInteract) {
                                        canInteract = false
                                        attempts++
                                        soundPool.play(soundWrong, 1f, 1f, 0, 0, 1f)
                                        feedbackColor = Color.Red
                                        scope.launch {
                                            delay(500)
                                            feedbackColor = Color.Transparent
                                            currentWordIndex++
                                            canInteract = true
                                            if (currentWordIndex >= words.size) {
                                                endGame()
                                            }
                                        }
                                    }
                                },
                                enabled = canInteract
                            ) {
                                Text("Incorrecto")
                            }
                        }
                    }
                }
            }
        }
    }

    // Liberar recursos
    DisposableEffect(Unit) {
        onDispose {
            soundPool.release()
            recorder?.release()
            player?.release()
        }
    }
}

data class Word(val text: String, val imageRes: Int)