package com.tesis.aplicacionpandax.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "specialty_games",
    primaryKeys = ["specialtyId", "gameId"],
    foreignKeys = [
        ForeignKey(
            entity = Specialty::class,
            parentColumns = ["id"],
            childColumns = ["specialtyId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Game::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SpecialtyGame(
    val specialtyId: Long,
    val gameId: Long
)