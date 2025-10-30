package com.tesis.aplicacionpandax

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tesis.aplicacionpandax.data.AppDatabase
import com.tesis.aplicacionpandax.data.dao.SpecialtyDao
import com.tesis.aplicacionpandax.data.entity.Specialty
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Prueba de instrumentación para SpecialtyDao.
 * Esta prueba se ejecuta en un dispositivo o emulador Android.
 */
@RunWith(AndroidJUnit4::class)
class SpecialtyDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var specialtyDao: SpecialtyDao

    // 1. Se ejecuta ANTES de cada prueba
    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Crea una base de datos "en memoria" (temporal) solo para esta prueba
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            // Permite consultas en el hilo principal (solo para pruebas)
            .allowMainThreadQueries()
            .build()
        specialtyDao = db.specialtyDao()
    }

    // 2. Se ejecuta DESPUÉS de cada prueba
    @After
    @Throws(IOException::class)
    fun closeDb() {
        // Cierra la base de datos en memoria
        db.close()
    }

    // 3. La Prueba: Insertar y Obtener una Especialidad
    @Test
    @Throws(Exception::class)
    fun insertAndGetSpecialty() = runTest { // Usa runTest para coroutines
        // ARRANGE (Preparar)
        val specialty = Specialty(id = 1, name = "Fonoaudiología")

        // ACT (Actuar)
        specialtyDao.insert(specialty)

        // ASSERT (Verificar)
        // Usa .first() para obtener el primer (y único) valor del Flow
        val allSpecialties = specialtyDao.getAll().first()
        val retrievedSpecialty = specialtyDao.getById(1)

        // Verifica que la lista no esté vacía y contenga nuestro item
        assertEquals(1, allSpecialties.size)
        assertEquals(specialty, allSpecialties[0])

        // Verifica que podamos obtenerlo por ID
        assertEquals(specialty, retrievedSpecialty)
    }

    // 4. (Opcional) Prueba de eliminación
    @Test
    @Throws(Exception::class)
    fun deleteSpecialty() = runTest {
        // ARRANGE
        val specialty = Specialty(id = 1, name = "Conducta")
        specialtyDao.insert(specialty)

        // ACT
        specialtyDao.delete(specialty)

        // ASSERT
        val allSpecialties = specialtyDao.getAll().first()
        val retrievedSpecialty = specialtyDao.getById(1)

        assertTrue(allSpecialties.isEmpty()) // La lista debe estar vacía
        assertNull(retrievedSpecialty) // El item ya no debe existir
    }
}