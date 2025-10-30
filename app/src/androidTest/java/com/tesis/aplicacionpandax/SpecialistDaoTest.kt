package com.tesis.aplicacionpandax

import android.content.Context
import android.database.sqlite.SQLiteConstraintException // Importar para la prueba de restricción
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.dao.SpecialistDao
import com.tesis.aplicacionpandax.data.dao.SpecialtyDao
import com.tesis.aplicacionpandax.data.dao.UserDao
import com.tesis.aplicacionpandax.data.entity.Specialist
import com.tesis.aplicacionpandax.data.entity.Specialty
import com.tesis.aplicacionpandax.data.entity.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Prueba de instrumentación para SpecialistDao.
 * Prueba las relaciones de clave foránea.
 */
@RunWith(AndroidJUnit4::class)
class SpecialistDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDao
    private lateinit var specialtyDao: SpecialtyDao
    private lateinit var specialistDao: SpecialistDao

    // Datos de prueba
    private val testUser = User(userId = 1, username = "testspec", passwordHash = "h", salt = "s", role = "SPECIALIST")
    private val testSpecialty = Specialty(id = 1, name = "Fonoaudiología")
    private val testSpecialist = Specialist(
        userId = 1, // Coincide con testUser
        firstName = "Ana",
        lastName = "Gomez",
        phone = "123456",
        email = "ana@g.com",
        specialtyId = 1 // Coincide con testSpecialty
    )

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        // Obtenemos todos los DAOs que necesitamos
        userDao = db.userDao()
        specialtyDao = db.specialtyDao()
        specialistDao = db.specialistDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    // Prueba 1: Insertar y obtener un especialista
    @Test
    @Throws(Exception::class)
    fun insertAndGetSpecialist() = runTest {
        // ARRANGE: Insertar dependencias primero
        userDao.insert(testUser)
        specialtyDao.insert(testSpecialty)

        // ACT
        specialistDao.insert(testSpecialist)

        // ASSERT
        val retrievedSpecialist = specialistDao.getByUserId(testUser.userId)
        val allSpecialists = specialistDao.getAll().first()

        assertEquals(testSpecialist, retrievedSpecialist)
        assertEquals(1, allSpecialists.size)
    }

    // Prueba 2: Probar que 'onDelete = CASCADE' para 'userId' funciona
    // Es decir, si borramos el User, el Specialist asociado también se borra.
    @Test
    @Throws(Exception::class)
    fun deletingUser_cascades_deletesSpecialist() = runTest {
        // ARRANGE
        userDao.insert(testUser)
        specialtyDao.insert(testSpecialty)
        specialistDao.insert(testSpecialist)

        // Verificar que existe antes de borrar
        assertNotNull(specialistDao.getByUserId(testUser.userId))

        // ACT
        userDao.delete(testUser) // Borramos el User

        // ASSERT
        // El Specialist debería ser nulo (borrado en cascada)
        val retrievedSpecialist = specialistDao.getByUserId(testUser.userId)
        assertNull("El especialista no debería existir después de borrar el usuario", retrievedSpecialist)
    }

    // Prueba 3: Probar que 'onDelete = RESTRICT' para 'specialtyId' funciona
    // Es decir, NO podemos borrar una Specialty si un Specialist la está usando.
    @Test
    @Throws(Exception::class)
    fun deletingSpecialty_isRestricted_ifInUse() = runTest {
        // ARRANGE
        userDao.insert(testUser)
        specialtyDao.insert(testSpecialty)
        specialistDao.insert(testSpecialist)

        // ACT & ASSERT
        try {
            specialtyDao.delete(testSpecialty) // Intentamos borrar la Specialty
            // Si llegamos aquí, la prueba falló porque no lanzó la excepción
            fail("Debería haber lanzado SQLiteConstraintException")
        } catch (e: SQLiteConstraintException) {
            // ¡Éxito! La base de datos restringió la eliminación como esperábamos
            assertTrue(true)
        }

        // Doble verificación: El especialista todavía debe existir
        val retrievedSpecialist = specialistDao.getByUserId(testUser.userId)
        assertNotNull("El especialista aún debe existir", retrievedSpecialist)
    }
}