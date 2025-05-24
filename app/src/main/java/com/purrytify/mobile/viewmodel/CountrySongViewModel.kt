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

class CountrySongViewModel(private val repository: CountrySongRepository) : ViewModel() {
    private val _countrySongs = MutableStateFlow<List<CountrySong>>(emptyList())
    val countrySongs: StateFlow<List<CountrySong>> = _countrySongs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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

    companion object {
        fun provideFactory(repository: CountrySongRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CountrySongViewModel(repository)
            }
        }
    }
}