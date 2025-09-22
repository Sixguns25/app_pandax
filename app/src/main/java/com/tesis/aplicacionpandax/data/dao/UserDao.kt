package com.tesis.aplicacionpandax.data.dao

import androidx.room.*
import com.tesis.aplicacionpandax.data.entity.User

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: User): Long

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE userId = :id LIMIT 1")
    suspend fun getById(id: Long): User?

    @Update
    suspend fun update(user: User)

    @Delete
    suspend fun delete(user: User)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int
}
