package com.tesis.aplicacionpandax.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "game_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Child::class,
            parentColumns = ["userId"],
            childColumns = ["childUserId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GameSession(
    @PrimaryKey(autoGenerate = true) val sessionId: Long = 0L,
    val childUserId: Long,  // FK a Child.userId
    val gameType: String,   // e.g., "MEMORY", "ATTENTION", "ASSOCIATION"
    val score: Int,
    val timeTaken: Long,    // en millis
    val attempts: Int,
    val timestamp: Long = System.currentTimeMillis()  // Fecha de la sesión
)