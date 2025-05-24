package com.purrytify.mobile.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.purrytify.mobile.data.SongRepository
import com.purrytify.mobile.data.room.TopSong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GlobalSongViewModel(private val repository: SongRepository) : ViewModel() {
    private val _topSongs = MutableStateFlow<List<TopSong>>(emptyList())
    val topSongs: StateFlow<List<TopSong>> = _topSongs.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        fetchTopSongs()
    }
    
    private fun fetchTopSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getTopSongs()
                .onSuccess { songs ->
                    _topSongs.value = songs
                }
                .onFailure { exception ->
                    // Handle error
                }
            _isLoading.value = false
        }
    }

    companion object {
        fun provideFactory(repository: SongRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                GlobalSongViewModel(repository)
            }
        }
    }
}