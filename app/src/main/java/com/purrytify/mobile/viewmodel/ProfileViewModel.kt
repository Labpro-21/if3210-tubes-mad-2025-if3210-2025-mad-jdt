// filepath: app/src/main/java/com/purrytify/mobile/viewmodel/ProfileViewModel.kt
package com.purrytify.mobile.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.purrytify.mobile.api.ProfileResponse
import com.purrytify.mobile.data.AuthRepository
import com.purrytify.mobile.utils.NetworkConnectivityObserver
import com.purrytify.mobile.utils.LocationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

// --- UI State ---
sealed class ProfileUiState {
    object Initial : ProfileUiState()
    object Loading : ProfileUiState()
    data class Success(val profile: ProfileResponse) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
    object NetworkError : ProfileUiState() // Add this state
}

// --- Edit Profile State ---
sealed class EditProfileState {
    object Initial : EditProfileState()
    object Loading : EditProfileState()
    object Success : EditProfileState()
    data class Error(val message: String) : EditProfileState()
}

// --- ViewModel ---
class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val networkObserver: NetworkConnectivityObserver,
    private val locationService: LocationService
) : ViewModel() {

    private val _profileUiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Initial)
    val profileUiState: StateFlow<ProfileUiState> = _profileUiState
    
    private val _editProfileState = MutableStateFlow<EditProfileState>(EditProfileState.Initial)
    val editProfileState: StateFlow<EditProfileState> = _editProfileState
    
    private val _networkStatus = MutableStateFlow(NetworkConnectivityObserver.Status.AVAILABLE)
    val networkStatus: StateFlow<NetworkConnectivityObserver.Status> = _networkStatus
    
    init {
        observeNetworkStatus()
    }
    
    private fun observeNetworkStatus() {
        viewModelScope.launch {
            networkObserver.observe().collect { status ->
                _networkStatus.value = status
                
                // If network becomes available and we're in network error state, try fetching again
                if (status == NetworkConnectivityObserver.Status.AVAILABLE && 
                    _profileUiState.value is ProfileUiState.NetworkError) {
                    fetchProfile()
                }
            }
        }
    }

    fun fetchProfile() {
        if (_profileUiState.value is ProfileUiState.Loading) return // Prevent multiple loads
        
        if (!networkObserver.isNetworkAvailable()) {
            _profileUiState.value = ProfileUiState.NetworkError
            return
        }

        viewModelScope.launch {
            _profileUiState.value = ProfileUiState.Loading
            Log.d("ProfileViewModel", "Fetching profile...")
            val result = authRepository.getProfile()
            result.onSuccess { profile ->
                Log.d("ProfileViewModel", "Profile fetch success: $profile")
                _profileUiState.value = ProfileUiState.Success(profile)
            }.onFailure { exception ->
                Log.e("ProfileViewModel", "Profile fetch failed", exception)
                if (!networkObserver.isNetworkAvailable()) {
                    _profileUiState.value = ProfileUiState.NetworkError
                } else {
                    _profileUiState.value = ProfileUiState.Error(exception.message ?: "Unknown error")
                }
            }
        }
    }

    fun editProfile(location: String?, profilePhoto: File?) {
        if (!networkObserver.isNetworkAvailable()) {
            _editProfileState.value = EditProfileState.Error("No network connection")
            return
        }

        viewModelScope.launch {
            _editProfileState.value = EditProfileState.Loading
            try {
                val result = authRepository.editProfile(location, profilePhoto)
                result.onSuccess { profile ->
                    _editProfileState.value = EditProfileState.Success
                    _profileUiState.value = ProfileUiState.Success(profile)
                }.onFailure { exception ->
                    _editProfileState.value = EditProfileState.Error(exception.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                _editProfileState.value = EditProfileState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun editProfileWithAutoLocation(profilePhoto: File?) {
        if (!networkObserver.isNetworkAvailable()) {
            _editProfileState.value = EditProfileState.Error("No network connection")
            return
        }

        viewModelScope.launch {
            _editProfileState.value = EditProfileState.Loading
            try {
                // Get current location country code
                val countryCode = locationService.getCurrentCountryCode()
                if (countryCode == null) {
                    _editProfileState.value = EditProfileState.Error("Unable to detect location. Please check location permissions.")
                    return@launch
                }
                
                val result = authRepository.editProfile(countryCode, profilePhoto)
                result.onSuccess { profile ->
                    _editProfileState.value = EditProfileState.Success
                    _profileUiState.value = ProfileUiState.Success(profile)
                }.onFailure { exception ->
                    _editProfileState.value = EditProfileState.Error(exception.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                _editProfileState.value = EditProfileState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun hasLocationPermission(): Boolean {
        return locationService.hasLocationPermission()
    }
}

// --- ViewModel Factory ---
class ProfileViewModelFactory(
    private val authRepository: AuthRepository,
    private val networkObserver: NetworkConnectivityObserver,
    private val locationService: LocationService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(authRepository, networkObserver, locationService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}