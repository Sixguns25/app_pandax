package com.tesis.aplicacionpandax.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val userId: Long = 0L,
    val username: String,
    val passwordHash: String,
    val salt: String,
    val role: String // "ADMIN", "SPECIALIST", "CHILD"
)
