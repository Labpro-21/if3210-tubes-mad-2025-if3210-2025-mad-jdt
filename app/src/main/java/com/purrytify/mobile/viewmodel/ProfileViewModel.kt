// filepath: app/src/main/java/com/purrytify/mobile/viewmodel/ProfileViewModel.kt
package com.purrytify.mobile.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.purrytify.mobile.api.ProfileResponse
import com.purrytify.mobile.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// --- UI State ---
sealed class ProfileUiState {
    object Initial : ProfileUiState()
    object Loading : ProfileUiState()
    data class Success(val profile: ProfileResponse) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

// --- ViewModel ---
class ProfileViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _profileUiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Initial)
    val profileUiState: StateFlow<ProfileUiState> = _profileUiState

    fun fetchProfile() {
        if (_profileUiState.value is ProfileUiState.Loading) return // Prevent multiple loads

        viewModelScope.launch {
            _profileUiState.value = ProfileUiState.Loading
            Log.d("ProfileViewModel", "Fetching profile...")
            val result = authRepository.getProfile()
            result.onSuccess { profile ->
                Log.d("ProfileViewModel", "Profile fetch success: $profile")
                _profileUiState.value = ProfileUiState.Success(profile)
            }.onFailure { exception ->
                Log.e("ProfileViewModel", "Profile fetch failed", exception)
                _profileUiState.value = ProfileUiState.Error(exception.message ?: "Unknown error")
            }
        }
    }
}

// --- ViewModel Factory ---
class ProfileViewModelFactory(private val authRepository: AuthRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}