package com.tesis.aplicacionpandax.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tesis.aplicacionpandax.data.AppDatabase // Necesitamos AppDatabase para los DAOs
import com.tesis.aplicacionpandax.data.entity.Specialty
import com.tesis.aplicacionpandax.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Data class que representa todo el estado de la pantalla RegisterSpecialistScreen
 */
data class RegisterSpecialistUiState(
    val username: String = "",
    val password: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val email: String = "",
    val selectedSpecialtyId: Long? = null,

    val specialties: List<Specialty> = emptyList(), // Lista de especialidades para el dropdown

    val isLoading: Boolean = false, // Para mostrar indicador de carga
    val isLoadingData: Boolean = false, // Para cargar datos en modo edición
    val message: String? = null, // Para SnackBar
    val navigationEvent: NavigationEvent? = null, // Para navegar atrás
    val isEditing: Boolean = false, // Para saber si es modo edición o registro
    val specialistId: Long = -1L
)

/**
 * Evento de navegación para notificar a la UI que debe navegar.
 * Usamos una clase wrapper para que el evento se consuma una sola vez.
 */
sealed class NavigationEvent {
    object NavigateBack : NavigationEvent()
}


/**
 * ViewModel para la pantalla RegisterSpecialistScreen
 */
class RegisterSpecialistViewModel(
    private val authRepository: AuthRepository,
    private val db: AppDatabase, // Acceso a DAOs
    private val specialistIdToEdit: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterSpecialistUiState())
    val uiState: StateFlow<RegisterSpecialistUiState> = _uiState.asStateFlow()

    init {
        // Carga la lista de especialidades al iniciar
        loadSpecialties()

        // Si se pasó un ID válido, carga los datos para edición
        if (specialistIdToEdit != -1L) {
            loadSpecialistForEditing()
        }
    }

    private fun loadSpecialties() {
        viewModelScope.launch {
            db.specialtyDao().getAll().collectLatest { specialtyList ->
                _uiState.update { it.copy(specialties = specialtyList) }
            }
        }
    }

    private fun loadSpecialistForEditing() {
        _uiState.update { it.copy(isLoadingData = true, isEditing = true, specialistId = specialistIdToEdit) }
        viewModelScope.launch {
            val specialist = db.specialistDao().getByUserId(specialistIdToEdit)
            val user = db.userDao().getById(specialistIdToEdit)

            if (specialist != null && user != null) {
                _uiState.update {
                    it.copy(
                        username = user.username,
                        firstName = specialist.firstName,
                        lastName = specialist.lastName,
                        phone = specialist.phone,
                        email = specialist.email,
                        selectedSpecialtyId = specialist.specialtyId,
                        isLoadingData = false
                    )
                }
            } else {
                _uiState.update { it.copy(message = "Error al cargar datos del especialista", isLoadingData = false) }
            }
        }
    }

    // --- Funciones para actualizar el estado desde la UI ---
    fun onUsernameChange(value: String) { _uiState.update { it.copy(username = value) } }
    fun onPasswordChange(value: String) { _uiState.update { it.copy(password = value) } }
    fun onFirstNameChange(value: String) { _uiState.update { it.copy(firstName = value) } }
    fun onLastNameChange(value: String) { _uiState.update { it.copy(lastName = value) } }
    fun onPhoneChange(value: String) { _uiState.update { it.copy(phone = value) } }
    fun onEmailChange(value: String) { _uiState.update { it.copy(email = value) } }
    fun onSpecialtySelected(id: Long) { _uiState.update { it.copy(selectedSpecialtyId = id) } }
    fun onMessageShown() { _uiState.update { it.copy(message = null) } }
    fun onNavigationDone() { _uiState.update { it.copy(navigationEvent = null) } }


    // --- Función de Guardado ---
    fun saveSpecialist() {
        val state = _uiState.value

        // Validación
        val validationError = validateInputs(state)
        if (validationError != null) {
            _uiState.update { it.copy(message = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = if (state.isEditing) {
                // Lógica de Actualización
                authRepository.updateSpecialist(
                    specialistId = state.specialistId,
                    firstName = state.firstName.trim(),
                    lastName = state.lastName.trim(),
                    phone = state.phone.trim(),
                    email = state.email.trim(),
                    specialtyId = state.selectedSpecialtyId!!, // Sabemos que no es null por la validación
                    password = if (state.password.isNotBlank()) state.password else null
                )
            } else {
                // Lógica de Registro
                authRepository.registerSpecialist(
                    username = state.username.trim(),
                    password = state.password,
                    firstName = state.firstName.trim(),
                    lastName = state.lastName.trim(),
                    phone = state.phone.trim(),
                    email = state.email.trim(),
                    specialtyId = state.selectedSpecialtyId!! // Sabemos que no es null
                )
            }

            result.fold(
                onSuccess = {
                    val successMessage = if (state.isEditing) "Especialista actualizado ✅" else "Especialista registrado ✅"
                    _uiState.update { it.copy(isLoading = false, message = successMessage, navigationEvent = NavigationEvent.NavigateBack) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, message = "Error: ${error.message} ❌") }
                }
            )
        }
    }

    // --- Función de Validación Interna ---
    private fun validateInputs(state: RegisterSpecialistUiState): String? {
        if (state.username.isBlank() || state.firstName.isBlank() || state.lastName.isBlank() ||
            state.phone.isBlank() || state.email.isBlank() || state.selectedSpecialtyId == null) {
            return "Por favor, completa todos los campos obligatorios (*)"
        }
        if (!state.isEditing && state.password.isBlank()) { // Contraseña obligatoria solo al registrar
            return "La contraseña es obligatoria al registrar (*)"
        }
        if (state.password.isNotBlank() && state.password.length < 6) {
            return "La contraseña debe tener al menos 6 caracteres"
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            return "Formato de correo inválido"
        }
        if (!state.phone.matches(Regex("\\d{9,15}"))) {
            return "Teléfono debe tener entre 9 y 15 dígitos"
        }
        return null // Sin errores
    }
}


// --- Factory para el ViewModel ---
class RegisterSpecialistViewModelFactory(
    private val authRepository: AuthRepository,
    private val db: AppDatabase,
    private val specialistId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterSpecialistViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegisterSpecialistViewModel(authRepository, db, specialistId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}