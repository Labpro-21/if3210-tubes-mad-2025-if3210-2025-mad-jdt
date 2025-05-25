package com.purrytify.mobile.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.purrytify.mobile.data.CountrySongRepository
import com.purrytify.mobile.data.room.CountrySong
import kotlinx.coroutines.Dispatchers
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

class CountrySongViewModel(private val repository: CountrySongRepository) :
    ViewModel() {
    private val _uiState =
        MutableStateFlow<CountrySongUiState>(CountrySongUiState.Loading)
    val uiState: StateFlow<CountrySongUiState> = _uiState.asStateFlow()

    // Keep these for backward compatibility
    private val _countrySongs = MutableStateFlow<List<CountrySong>>(emptyList())
    val countrySongs: StateFlow<List<CountrySong>> = _countrySongs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> =
        _downloadProgress.asStateFlow()

    private val _downloadedSongs = MutableStateFlow<Set<Int>>(emptySet())
    val downloadedSongs: StateFlow<Set<Int>> = _downloadedSongs.asStateFlow()

    init {
        checkCountrySupportAndFetch()
    }

    private fun checkCountrySupportAndFetch() {
        viewModelScope.launch {
            _uiState.value = CountrySongUiState.Loading
            _isLoading.value = true
            try {
                val supportResult = repository.isCountrySupported()
                if (supportResult.isFailure) {
                    val error =
                        supportResult.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e(
                        "CountrySongViewModel",
                        "Error checking country support: $error"
                    )
                    _uiState.value = CountrySongUiState.Error(error)
                    return@launch
                }

                val isSupported = supportResult.getOrNull() ?: false
                if (!isSupported) {
                    Log.d("CountrySongViewModel", "Country not supported")
                    _uiState.value = CountrySongUiState.CountryNotSupported
                    return@launch
                }

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
                    viewModelScope.launch { checkDownloadedSongs(songs) }
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

    fun getSupportedCountries(): Set<String> = repository.getSupportedCountries()

    private suspend fun checkDownloadedSongs(songs: List<CountrySong>) {
        val downloadedIds = mutableSetOf<Int>()
        songs.forEach { song ->
            if (repository.isSongDownloaded(song.id)) {
                downloadedIds.add(song.id)
            }
        }
        _downloadedSongs.value = downloadedIds
    }

    fun downloadSong(song: CountrySong, context: Context) {
        viewModelScope.launch {
            try {
                // Initialize progress
                _downloadProgress.value =
                    _downloadProgress.value + (song.id.toString() to 0f)

                repository.downloadSong(
                    song = song,
                    onProgress = { progress ->
                        _downloadProgress.value =
                            _downloadProgress.value + (song.id.toString() to progress)
                    },
                    onComplete = { downloadedSong ->
                        viewModelScope.launch(Dispatchers.Main) {
                            _downloadProgress.value =
                                _downloadProgress.value - song.id.toString()
                            // Update downloaded songs set
                            _downloadedSongs.value = _downloadedSongs.value + song.id
                            Toast.makeText(
                                    context,
                                    "Download complete: ${song.title}",
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                        }
                    }
                )
            } catch (e: Exception) {
                viewModelScope.launch(Dispatchers.Main) {
                    _downloadProgress.value =
                        _downloadProgress.value - song.id.toString()
                    Toast.makeText(
                            context,
                            "Download failed: ${e.message}",
                            Toast.LENGTH_LONG
                        )
                        .show()
                }
            }
        }
    }

    companion object {
        fun provideFactory(repository: CountrySongRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { CountrySongViewModel(repository) } }
    }
}