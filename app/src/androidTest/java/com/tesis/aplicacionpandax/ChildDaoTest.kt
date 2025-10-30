package com.tesis.aplicacionpandax

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.dao.ChildDao
import com.tesis.aplicacionpandax.data.dao.SpecialistDao
import com.tesis.aplicacionpandax.data.dao.SpecialtyDao
import com.tesis.aplicacionpandax.data.dao.UserDao
import com.tesis.aplicacionpandax.data.entity.Child
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
 * Prueba de instrumentación para ChildDao.
 * Verifica las relaciones de clave foránea.
 */
@RunWith(AndroidJUnit4::class)
class ChildDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDao
    private lateinit var specialtyDao: SpecialtyDao
    private lateinit var specialistDao: SpecialistDao
    private lateinit var childDao: ChildDao

    // --- Datos de prueba ---
    // (Usamos IDs fijos para facilitar las aserciones)
    private val testUserChild = User(userId = 1, username = "testchild", passwordHash = "h1", salt = "s1", role = "CHILD")
    private val testUserSpec = User(userId = 2, username = "testspec", passwordHash = "h2", salt = "s2", role = "SPECIALIST")
    private val testSpecialty = Specialty(id = 1, name = "Fonoaudiología")
    private val testSpecialist = Specialist(
        userId = 2, // ID del usuario especialista
        firstName = "Dr.",
        lastName = "Prueba",
        phone = "999",
        email = "spec@test.com",
        specialtyId = 1 // ID de la especialidad
    )
    private val testChild = Child(
        userId = 1, // ID del usuario niño
        firstName = "Pepe",
        lastName = "Grillo",
        dni = "12345678",
        condition = "TEA",
        sex = "M",
        birthDateMillis = 123456789L,
        guardianName = "Gepetto",
        guardianPhone = "888",
        specialistId = 2 // ID del especialista
    )

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        // Obtenemos todos los DAOs
        userDao = db.userDao()
        specialtyDao = db.specialtyDao()
        specialistDao = db.specialistDao()
        childDao = db.childDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    /**
     * Inserta todas las dependencias necesarias para una prueba de Child.
     */
    private suspend fun insertPrerequisites() {
        userDao.insert(testUserChild)
        userDao.insert(testUserSpec)
        specialtyDao.insert(testSpecialty)
        specialistDao.insert(testSpecialist)
    }

    // Prueba 1: Insertar y obtener un niño
    @Test
    @Throws(Exception::class)
    fun insertAndGetChild() = runTest {
        // ARRANGE
        insertPrerequisites()

        // ACT
        childDao.insert(testChild)

        // ASSERT
        val retrievedChild = childDao.getByUserId(testUserChild.userId)
        assertEquals(testChild, retrievedChild)
    }

    // Prueba 2: Probar 'onDelete = CASCADE' para el User del niño
    // Si borramos el User (el login del niño), el perfil Child también debe borrarse.
    @Test
    @Throws(Exception::class)
    fun deletingChildUser_cascades_deletesChildProfile() = runTest {
        // ARRANGE
        insertPrerequisites()
        childDao.insert(testChild)

        // Verificar que existe
        assertNotNull(childDao.getByUserId(testUserChild.userId))

        // ACT
        userDao.delete(testUserChild) // Borramos el User del niño

        // ASSERT
        val retrievedChild = childDao.getByUserId(testUserChild.userId)
        assertNull("El perfil del niño debería haberse borrado en cascada", retrievedChild)
    }

    // Prueba 3: Probar 'onDelete = SET_NULL' para el Specialist
    // Si borramos el Specialist, el Child NO debe borrarse, pero su specialistId debe ser null.
    @Test
    @Throws(Exception::class)
    fun deletingSpecialist_setsSpecialistIdToNull_inChild() = runTest {
        // ARRANGE
        insertPrerequisites()
        childDao.insert(testChild)

        // Verificar que el ID está asignado
        var retrievedChild = childDao.getByUserId(testUserChild.userId)
        assertEquals(testSpecialist.userId, retrievedChild?.specialistId)

        // ACT
        // Borramos el User del especialista (lo que borra en cascada al Specialist)
        userDao.delete(testUserSpec)

        // ASSERT
        retrievedChild = childDao.getByUserId(testUserChild.userId)
        assertNotNull("El niño NO debería haberse borrado", retrievedChild)
        assertNull("El specialistId del niño debería ser null", retrievedChild?.specialistId)
    }

    // Prueba 4: Probar la consulta personalizada con JOIN
    @Test
    @Throws(Exception::class)
    fun getAllWithSpecialist_returnsCorrectData() = runTest {
        // ARRANGE
        insertPrerequisites()
        childDao.insert(testChild)

        // ACT
        val resultList = childDao.getAllWithSpecialist().first()

        // ASSERT
        assertEquals(1, resultList.size)
        val result = resultList[0]

        assertEquals(testChild, result.child)
        assertEquals(testSpecialist.firstName, result.specialistFirstName)
        assertEquals(testSpecialist.lastName, result.specialistLastName)
        assertEquals("Dr. Prueba", result.specialistName) // Verifica el getter
    }

    // Prueba 5: Probar la consulta con JOIN cuando el especialista es null
    @Test
    @Throws(Exception::class)
    fun getAllWithSpecialist_handlesNullSpecialist() = runTest {
        // ARRANGE
        insertPrerequisites()
        // Insertar niño sin especialista
        val childNoSpec = testChild.copy(specialistId = null)
        childDao.insert(childNoSpec)

        // ACT
        val resultList = childDao.getAllWithSpecialist().first()

        // ASSERT
        assertEquals(1, resultList.size)
        val result = resultList[0]

        assertEquals(childNoSpec, result.child)
        assertNull(result.specialistFirstName) // El nombre debe ser null
        assertNull(result.specialistLastName) // El apellido debe ser null
        assertEquals("Sin especialista", result.specialistName) // Verifica el getter
    }
}