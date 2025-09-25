package com.tesis.aplicacionpandax.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "specialties")
data class Specialty(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String  // e.g., "Conducta"
)