package com.tesis.aplicacionpandax.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tesis.aplicacionpandax.data.entity.User
import com.tesis.aplicacionpandax.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repo: AuthRepository) : ViewModel() {

    private val _loginState = MutableStateFlow<Result<User>?>(null)
    val loginState: StateFlow<Result<User>?> = _loginState

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = repo.login(username, password)
        }
    }

    fun clearState() {
        _loginState.value = null
    }
}
