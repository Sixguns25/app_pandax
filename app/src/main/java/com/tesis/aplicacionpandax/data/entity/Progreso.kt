package com.tesis.aplicacionpandax.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "progreso")
data class Progreso(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val childId: Long,
    val game: String,
    val score: Int,
    val moves: Int,
    val timeMillis: Long,
    val date: Long = System.currentTimeMillis()
)