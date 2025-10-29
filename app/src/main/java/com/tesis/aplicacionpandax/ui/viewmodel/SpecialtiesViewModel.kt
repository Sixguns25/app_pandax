package com.tesis.aplicacionpandax.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tesis.aplicacionpandax.data.dao.SpecialistDao
import com.tesis.aplicacionpandax.data.dao.SpecialtyDao
import com.tesis.aplicacionpandax.data.entity.Specialty
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Estado de la UI que manejará el ViewModel
data class SpecialtiesUiState(
    val specialties: List<Specialty> = emptyList(),
    val currentInputName: String = "", // Para el TextField
    val editingSpecialty: Specialty? = null, // Null si estamos creando, no-null si editando
    val message: String? = null, // Mensajes para Snackbar
    val isLoading: Boolean = false // Para indicar si se está procesando algo
)

class SpecialtiesViewModel(
    private val specialtyDao: SpecialtyDao,
    private val specialistDao: SpecialistDao // Necesario para la validación al eliminar
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpecialtiesUiState())
    val uiState: StateFlow<SpecialtiesUiState> = _uiState.asStateFlow()

    init {
        // Observa el Flow del DAO y actualiza el estado
        viewModelScope.launch {
            specialtyDao.getAll().collect { specialtiesList ->
                _uiState.update { it.copy(specialties = specialtiesList) }
            }
        }
    }

    // Acción: Actualizar el valor del campo de texto
    fun updateInputName(name: String) {
        _uiState.update { it.copy(currentInputName = name) }
    }

    // Acción: Iniciar la edición de una especialidad existente
    fun startEditing(specialty: Specialty) {
        _uiState.update {
            it.copy(
                editingSpecialty = specialty,
                currentInputName = specialty.name // Pre-rellena el campo
            )
        }
    }

    // Acción: Cancelar la edición
    fun cancelEditing() {
        _uiState.update {
            it.copy(
                editingSpecialty = null,
                currentInputName = "" // Limpia el campo
            )
        }
    }

    // Acción: Guardar (ya sea crear una nueva o actualizar una existente)
    fun saveSpecialty() {
        val currentState = _uiState.value
        val nameToSave = currentState.currentInputName.trim()

        if (nameToSave.isBlank()) {
            _uiState.update { it.copy(message = "Error: El nombre no puede estar vacío") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                if (currentState.editingSpecialty != null) {
                    // Actualizar
                    val updatedSpecialty = currentState.editingSpecialty.copy(name = nameToSave)
                    specialtyDao.update(updatedSpecialty)
                    _uiState.update {
                        it.copy(
                            message = "Especialidad actualizada correctamente ✅",
                            editingSpecialty = null, // Sale del modo edición
                            currentInputName = ""   // Limpia el campo
                        )
                    }
                } else {
                    // Crear nueva
                    specialtyDao.insert(Specialty(name = nameToSave))
                    _uiState.update {
                        it.copy(
                            message = "Especialidad agregada correctamente ✅",
                            currentInputName = "" // Limpia el campo
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Error al guardar: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // Acción: Eliminar una especialidad
    fun deleteSpecialty(specialty: Specialty) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Verificar si hay especialistas asignados ANTES de intentar borrar
                val assignedSpecialists = specialistDao.getSpecialistsBySpecialtyId(specialty.id)
                if (assignedSpecialists.isEmpty()) {
                    specialtyDao.delete(specialty)
                    _uiState.update { it.copy(message = "Especialidad eliminada correctamente ✅") }
                } else {
                    _uiState.update { it.copy(message = "Error: No se puede eliminar, hay especialistas asignados") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Error al eliminar: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // Acción: Limpiar el mensaje (después de mostrarlo en Snackbar)
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}


// --- Factory para el ViewModel ---
// Necesitamos esto porque el ViewModel tiene dependencias (los DAOs)
class SpecialtiesViewModelFactory(
    private val specialtyDao: SpecialtyDao,
    private val specialistDao: SpecialistDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SpecialtiesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SpecialtiesViewModel(specialtyDao, specialistDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}