package com.purrytify.mobile.viewmodel // Or your viewmodel package

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.purrytify.mobile.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class LoginUiState {
    object Initial : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val isLoggedIn: Boolean) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _loginUiState = MutableStateFlow<LoginUiState>(LoginUiState.Initial)
    val loginUiState: StateFlow<LoginUiState> = _loginUiState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginUiState.value = LoginUiState.Loading
            Log.d("LoginViewModel", "Calling authRepository.login")
            try {
                val isLoggedIn = authRepository.login(email, password)
                Log.d("LoginViewModel", "authRepository.login returned: $isLoggedIn")
                _loginUiState.value = LoginUiState.Success(isLoggedIn)
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Exception during login: ${e.message}", e)
                _loginUiState.value =
                        LoginUiState.Error("Login failed: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun resetState() {
        _loginUiState.value = LoginUiState.Initial
    }
}

class LoginViewModelFactory(private val authRepository: AuthRepository) :
        ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") // Necessary cast
            return LoginViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
