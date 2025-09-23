package com.tesis.aplicacionpandax.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tesis.aplicacionpandax.data.dao.*
import com.tesis.aplicacionpandax.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(
    entities = [User::class, Specialist::class, Child::class, GameSession::class],  // Agrega GameSession
    version = 2,  // Bump versión para migración
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun specialistDao(): SpecialistDao
    abstract fun childDao(): ChildDao
    abstract fun gameSessionDao(): GameSessionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `game_sessions` (
                        `sessionId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `childUserId` INTEGER NOT NULL,
                        `gameType` TEXT NOT NULL,
                        `score` INTEGER NOT NULL,
                        `timeTaken` INTEGER NOT NULL,
                        `attempts` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        FOREIGN KEY(`childUserId`) REFERENCES `children`(`userId`) ON DELETE CASCADE
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_1_2)  // Agrega migración
                    .addCallback(DatabaseCallback(scope)).build()
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
