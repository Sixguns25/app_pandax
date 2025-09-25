package com.tesis.aplicacionpandax.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tesis.aplicacionpandax.data.entity.Child
import kotlinx.coroutines.flow.Flow

@Dao
interface ChildDao {
    @Insert
    suspend fun insert(child: Child)

    @Query("SELECT * FROM children WHERE userId = :userId")
    suspend fun getByUserId(userId: Long): Child?

    @Query("SELECT * FROM children WHERE specialistId = :specialistId")
    suspend fun getBySpecialist(specialistId: Long): List<Child> // Cambiado de Flow a List

    @Query("SELECT * FROM children")
    fun getAll(): Flow<List<Child>>

    @Query("SELECT * FROM children WHERE specialistId = :specialistId")
    fun getChildrenForSpecialist(specialistId: Long): Flow<List<Child>>

    @Query("SELECT * FROM children WHERE userId = :userId LIMIT 1")
    fun getChildByUserId(userId: Long): Flow<Child?>
}