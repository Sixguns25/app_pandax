package com.tesis.aplicacionpandax.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class Game(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,  // Código interno, e.g., "MEMORY"
    val displayName: String,  // Nombre visible, e.g., "Juego de Memoria"
    val route: String  // Ruta de navegación, e.g., "memory_game/{childUserId}"
)