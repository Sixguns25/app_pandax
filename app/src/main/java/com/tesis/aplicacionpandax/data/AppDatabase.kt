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
    entities = [User::class, Specialist::class, Child::class, GameSession::class, Specialty::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun specialistDao(): SpecialistDao
    abstract fun childDao(): ChildDao
    abstract fun gameSessionDao(): GameSessionDao
    abstract fun specialtyDao(): SpecialtyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `specialties` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Crea tabla temporal con la nueva estructura
                db.execSQL("""
                    CREATE TABLE specialists_new (
                        userId INTEGER PRIMARY KEY NOT NULL,
                        firstName TEXT NOT NULL,
                        lastName TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        email TEXT NOT NULL,
                        specialtyId INTEGER NOT NULL,
                        FOREIGN KEY(userId) REFERENCES users(userId) ON DELETE CASCADE,
                        FOREIGN KEY(specialtyId) REFERENCES specialties(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                // Copia datos existentes, asigna specialtyId=1 (Conducta) por defecto
                db.execSQL("""
                    INSERT INTO specialists_new (userId, firstName, lastName, phone, email, specialtyId)
                    SELECT userId, firstName, lastName, phone, email, 1 FROM specialists
                """.trimIndent())
                // Elimina tabla antigua
                db.execSQL("DROP TABLE specialists")
                // Renombra tabla nueva
                db.execSQL("ALTER TABLE specialists_new RENAME TO specialists")
            }
        }

        fun getInstance(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .addCallback(DatabaseCallback(scope)).build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch {
                    val userDao = database.userDao()
                    if (userDao.count() == 0) {
                        // Crea usuario admin por defecto
                        val (salt, hash) = PasswordUtils.hashPasswordWithSalt("admin123")
                        val admin = User(
                            username = "admin",
                            passwordHash = hash,
                            salt = salt,
                            role = "ADMIN"
                        )
                        userDao.insert(admin)
                    }
                    // Inserta especialidades iniciales
                    val specialtyDao = database.specialtyDao()
                    listOf(
                        "Conducta",
                        "Fonoaudiología",
                        "Lenguaje",
                        "Educación",
                        "Terapia de motricidad"
                    ).forEach { name ->
                        specialtyDao.insert(Specialty(name = name))
                    }
                }
            }
        }
    }
}