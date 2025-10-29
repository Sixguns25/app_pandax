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
    entities = [User::class, Specialist::class, Child::class, GameSession::class, Specialty::class, Game::class, SpecialtyGame::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun specialistDao(): SpecialistDao
    abstract fun childDao(): ChildDao
    abstract fun gameSessionDao(): GameSessionDao
    abstract fun specialtyDao(): SpecialtyDao
    abstract fun gameDao(): GameDao
    abstract fun specialtyGameDao(): SpecialtyGameDao

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
                db.execSQL("""
                    INSERT INTO specialists_new (userId, firstName, lastName, phone, email, specialtyId)
                    SELECT userId, firstName, lastName, phone, email, 1 FROM specialists
                """.trimIndent())
                db.execSQL("DROP TABLE specialists")
                db.execSQL("ALTER TABLE specialists_new RENAME TO specialists")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `games` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `route` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `specialty_games` (
                        `specialtyId` INTEGER NOT NULL,
                        `gameId` INTEGER NOT NULL,
                        PRIMARY KEY(`specialtyId`, `gameId`),
                        FOREIGN KEY(`specialtyId`) REFERENCES `specialties`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`gameId`) REFERENCES `games`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Renombra la columna 'score' a 'stars' en la tabla 'game_sessions'
                db.execSQL("ALTER TABLE game_sessions RENAME COLUMN score TO stars")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Añade la nueva columna 'level' a la tabla 'game_sessions'
                // DEFAULT 1 asignará 1 a todas las filas existentes
                db.execSQL("ALTER TABLE game_sessions ADD COLUMN level INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getInstance(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
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
                    val specialtyIds = listOf(
                        "Conducta",
                        "Fonoaudiología",
                        "Lenguaje",
                        "Educación",
                        "Terapia de motricidad"
                    ).map { name ->
                        specialtyDao.insert(Specialty(name = name))
                    }
                    // Inserta juegos
                    val gameDao = database.gameDao()
                    val specialtyGameDao = database.specialtyGameDao()

                    // Juegos reales
                    val memoryId = gameDao.insert(Game(name = "MEMORY", displayName = "Juego de Memoria", route = "memory_game/{childUserId}"))
                    val emotionsId = gameDao.insert(Game(name = "EMOTIONS", displayName = "Juego de Emociones", route = "emotions_game/{childUserId}"))
                    val coordinationId = gameDao.insert(Game(name = "COORDINATION", displayName = "Juego de Coordinación", route = "coordination_game/{childUserId}"))
                    val pronunciationId = gameDao.insert(Game(name = "PRONUNCIATION", displayName = "Juego de Pronunciación", route = "pronunciation_game/{childUserId}"))

                    // Asignaciones a especialidades
                    // Conducta (specialtyIds[0]): MEMORY, EMOTIONS
                    specialtyGameDao.insert(SpecialtyGame(specialtyId = specialtyIds[0], gameId = memoryId))
                    specialtyGameDao.insert(SpecialtyGame(specialtyId = specialtyIds[0], gameId = emotionsId))
                    // Fonoaudiología (specialtyIds[1]): EMOTIONS
                    specialtyGameDao.insert(SpecialtyGame(specialtyId = specialtyIds[1], gameId = emotionsId))
                    specialtyGameDao.insert(SpecialtyGame(specialtyId = specialtyIds[1], gameId = pronunciationId))
                    // Lenguaje (specialtyIds[2]): EMOTIONS
                    specialtyGameDao.insert(SpecialtyGame(specialtyId = specialtyIds[2], gameId = emotionsId))
                    // Educación (specialtyIds[3]): MEMORY
                    specialtyGameDao.insert(SpecialtyGame(specialtyId = specialtyIds[3], gameId = memoryId))
                    // Terapia de motricidad (specialtyIds[4]): COORDINATION
                    specialtyGameDao.insert(SpecialtyGame(specialtyId = specialtyIds[4], gameId = coordinationId))
                }
            }
        }
    }
}