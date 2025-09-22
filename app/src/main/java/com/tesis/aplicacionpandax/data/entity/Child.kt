package com.tesis.aplicacionpandax.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "children",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Specialist::class,
            parentColumns = ["userId"],
            childColumns = ["specialistId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Child(
    @PrimaryKey val userId: Long, // FK a User
    val firstName: String,
    val lastName: String,
    val dni: String,
    val condition: String,
    val sex: String,
    val birthDateMillis: Long,
    val guardianName: String,
    val guardianPhone: String,
    val specialistId: Long? // FK a Specialist (nullable)
)
