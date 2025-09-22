package com.tesis.aplicacionpandax.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tesis.aplicacionpandax.data.dao.*
import com.tesis.aplicacionpandax.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(
    entities = [User::class, Specialist::class, Child::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun specialistDao(): SpecialistDao
    abstract fun childDao(): ChildDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).addCallback(DatabaseCallback(scope)).build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch {
                    val userDao = database.userDao()
                    if (userDao.count() == 0) {
                        // crea usuario admin por defecto
                        val (salt, hash) = PasswordUtils.hashPasswordWithSalt("admin123")
                        val admin = User(
                            username = "admin",
                            passwordHash = hash,
                            salt = salt,
                            role = "ADMIN"
                        )
                        userDao.insert(admin)
                    }
                }
            }
        }
    }
}
