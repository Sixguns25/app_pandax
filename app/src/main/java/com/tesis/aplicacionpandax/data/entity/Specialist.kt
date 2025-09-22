package com.tesis.aplicacionpandax.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "specialists",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Specialist(
    @PrimaryKey val userId: Long, // FK a User
    val firstName: String,
    val lastName: String,
    val phone: String,
    val email: String,
    val specialty: String
)
