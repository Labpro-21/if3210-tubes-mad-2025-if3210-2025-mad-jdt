package com.purrytify.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.purrytify.mobile.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.purrytify.mobile.api.AuthService

class AuthViewModel( // Remove @HiltViewModel and @Inject
    private val repository: AuthRepository,
    private val authService: AuthService
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    fun login(email: String, password: String) {
        viewModelScope.launch {
            val result = repository.login(email, password)
            _isLoggedIn.value = result
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _isLoggedIn.value = false
        }
    }
}
