package com.tesis.aplicacionpandax

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.dao.UserDao
import com.tesis.aplicacionpandax.data.entity.User
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Prueba de instrumentación para UserDao.
 */
@RunWith(AndroidJUnit4::class)
class UserDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDao

    // Se ejecuta ANTES de cada prueba
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userDao = db.userDao()
    }

    // Se ejecuta DESPUÉS de cada prueba
    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    // Prueba 1: Insertar un usuario y recuperarlo por ID
    @Test
    @Throws(Exception::class)
    fun insertAndGetUserById() = runTest {
        // ARRANGE (Preparar)
        val user = User(
            username = "testuser",
            passwordHash = "hash123",
            salt = "salt123",
            role = "CHILD"
        )

        // ACT (Actuar)
        // 'insert' devuelve el nuevo ID generado (Long)
        val newId = userDao.insert(user)
        val retrievedUser = userDao.getById(newId)

        // ASSERT (Verificar)
        // Verificamos que el usuario recuperado sea igual al original (con el ID actualizado)
        assertEquals(user.copy(userId = newId), retrievedUser)
    }

    // Prueba 2: Insertar un usuario y recuperarlo por nombre de usuario
    @Test
    @Throws(Exception::class)
    fun insertAndGetUserByUsername() = runTest {
        // ARRANGE
        val user = User(
            username = "testuser2",
            passwordHash = "hash456",
            salt = "salt456",
            role = "SPECIALIST"
        )
        val newId = userDao.insert(user)

        // ACT
        val retrievedUser = userDao.getByUsername("testuser2")

        // ASSERT
        assertEquals(user.copy(userId = newId), retrievedUser)
    }

    // Prueba 3: Intentar obtener un usuario que no existe
    @Test
    @Throws(Exception::class)
    fun getUserByUsername_returnsNull_ifNotExists() = runTest {
        // ARRANGE (No insertamos nada)

        // ACT
        val retrievedUser = userDao.getByUsername("usuario_inexistente")

        // ASSERT
        assertNull("El usuario recuperado debería ser nulo", retrievedUser)
    }

    // Prueba 4: Probar la función de conteo
    @Test
    @Throws(Exception::class)
    fun countUsers() = runTest {
        // ARRANGE
        val user1 = User(username = "user1", passwordHash = "h", salt = "s", role = "CHILD")
        val user2 = User(username = "user2", passwordHash = "h", salt = "s", role = "ADMIN")
        userDao.insert(user1)
        userDao.insert(user2)

        // ACT
        val count = userDao.count()

        // ASSERT
        assertEquals(2, count)
    }
}