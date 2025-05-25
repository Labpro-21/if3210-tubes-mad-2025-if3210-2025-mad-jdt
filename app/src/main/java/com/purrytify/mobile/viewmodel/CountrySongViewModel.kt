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

class CountrySongViewModel(private val repository: CountrySongRepository) : ViewModel() {
    private val _countrySongs = MutableStateFlow<List<CountrySong>>(emptyList())
    val countrySongs: StateFlow<List<CountrySong>> = _countrySongs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    init {
        fetchCountrySongs()
    }

    private fun fetchCountrySongs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getCountrySongs()
                    .onSuccess { songs ->
                        Log.d("CountrySongViewModel", "Fetched ${songs.size} songs")
                        _countrySongs.value = songs
                    }
                    .onFailure { error ->
                        Log.e("CountrySongViewModel", "Error fetching songs: ${error.message}")
                    }
            } catch (e: Exception) {
                Log.e("CountrySongViewModel", "Exception in fetchCountrySongs: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun downloadSong(song: CountrySong, context: Context) {
        viewModelScope.launch {
            try {
                _downloadProgress.value = _downloadProgress.value + (song.id.toString() to 0f)
                
                repository.downloadSong(
                    song = song,
                    onProgress = { progress ->
                        _downloadProgress.value = _downloadProgress.value + (song.id.toString() to progress)
                    },
                    onComplete = { downloadedSong ->
                        // Switch to Main thread for UI updates
                        viewModelScope.launch(Dispatchers.Main) {
                            _downloadProgress.value = _downloadProgress.value - song.id.toString()
                            Toast.makeText(context, "Download complete: ${song.title}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } catch (e: Exception) {
                // Switch to Main thread for UI updates
                viewModelScope.launch(Dispatchers.Main) {
                    _downloadProgress.value = _downloadProgress.value - song.id.toString()
                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    companion object {
        fun provideFactory(repository: CountrySongRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CountrySongViewModel(repository)
            }
        }
    }
}