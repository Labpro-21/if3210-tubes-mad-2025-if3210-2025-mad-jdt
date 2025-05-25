package com.purrytify.mobile.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.purrytify.mobile.data.CountrySongRepository
import com.purrytify.mobile.data.room.CountrySong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CountrySongUiState {
    object Loading : CountrySongUiState()
    data class Success(val songs: List<CountrySong>) : CountrySongUiState()
    data class Error(val message: String) : CountrySongUiState()
    object CountryNotSupported : CountrySongUiState()
}

class CountrySongViewModel(private val repository: CountrySongRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<CountrySongUiState>(CountrySongUiState.Loading)
    val uiState: StateFlow<CountrySongUiState> = _uiState.asStateFlow()

    // Keep these for backward compatibility
    private val _countrySongs = MutableStateFlow<List<CountrySong>>(emptyList())
    val countrySongs: StateFlow<List<CountrySong>> = _countrySongs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        checkCountrySupportAndFetch()
    }

    private fun checkCountrySupportAndFetch() {
        viewModelScope.launch {
            _uiState.value = CountrySongUiState.Loading
            _isLoading.value = true

            try {
                // First check if country is supported
                val supportResult = repository.isCountrySupported()
                if (supportResult.isFailure) {
                    val error = supportResult.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e("CountrySongViewModel", "Error checking country support: $error")
                    _uiState.value = CountrySongUiState.Error(error)
                    return@launch
                }

                val isSupported = supportResult.getOrNull() ?: false
                if (!isSupported) {
                    Log.d("CountrySongViewModel", "Country not supported")
                    _uiState.value = CountrySongUiState.CountryNotSupported
                    return@launch
                }

                // If supported, fetch songs
                fetchCountrySongs()
            } catch (e: Exception) {
                Log.e(
                        "CountrySongViewModel",
                        "Exception in checkCountrySupportAndFetch: ${e.message}"
                )
                _uiState.value = CountrySongUiState.Error(e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchCountrySongs() {
        try {
            repository
                    .getCountrySongs()
                    .onSuccess { songs ->
                        Log.d("CountrySongViewModel", "Fetched ${songs.size} songs")
                        _countrySongs.value = songs
                        _uiState.value = CountrySongUiState.Success(songs)
                    }
                    .onFailure { error ->
                        val errorMessage = error.message ?: "Unknown error"
                        Log.e("CountrySongViewModel", "Error fetching songs: $errorMessage")
                        _uiState.value = CountrySongUiState.Error(errorMessage)
                    }
        } catch (e: Exception) {
            Log.e("CountrySongViewModel", "Exception in fetchCountrySongs: ${e.message}")
            _uiState.value = CountrySongUiState.Error(e.message ?: "Unknown error")
        }
    }

    fun retry() {
        checkCountrySupportAndFetch()
    }

    fun getSupportedCountries(): Set<String> {
        return repository.getSupportedCountries()
    }

    companion object {
        fun provideFactory(repository: CountrySongRepository): ViewModelProvider.Factory =
                viewModelFactory {
                    initializer { CountrySongViewModel(repository) }
                }
    }
}
