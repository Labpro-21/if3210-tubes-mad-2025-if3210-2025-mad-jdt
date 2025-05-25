package com.purrytify.mobile.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.purrytify.mobile.data.SongRepository
import com.purrytify.mobile.data.room.TopSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GlobalSongViewModel(private val repository: SongRepository) : ViewModel() {
    private val _topSongs = MutableStateFlow<List<TopSong>>(emptyList())
    val topSongs: StateFlow<List<TopSong>> = _topSongs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    private val _downloadedSongs = MutableStateFlow<Set<Int>>(emptySet())
    val downloadedSongs: StateFlow<Set<Int>> = _downloadedSongs.asStateFlow()

    init {
        fetchTopSongs()
    }

    private fun fetchTopSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            repository
                    .getTopSongs()
                    .onSuccess { songs ->
                        _topSongs.value = songs
                        viewModelScope.launch { checkDownloadedSongs(songs) }
                    }
                    .onFailure { exception ->
                        // Handle error
                    }
            _isLoading.value = false
        }
    }

    private suspend fun checkDownloadedSongs(songs: List<TopSong>) {
        val downloadedIds = mutableSetOf<Int>()
        songs.forEach { song ->
            if (repository.isSongDownloaded(song.id)) {
                downloadedIds.add(song.id)
            }
        }
        _downloadedSongs.value = downloadedIds
    }

    fun downloadSong(song: TopSong, context: Context) {
        viewModelScope.launch {
            try {
                _downloadProgress.value = _downloadProgress.value + (song.id.toString() to 0f)

                repository.downloadSong(
                        song = song,
                        onProgress = { progress ->
                            _downloadProgress.value =
                                    _downloadProgress.value + (song.id.toString() to progress)
                        },
                        onComplete = { downloadedSong ->
                            // Switch to Main thread for UI updates
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
                // Switch to Main thread for UI updates
                viewModelScope.launch(Dispatchers.Main) {
                    _downloadProgress.value = _downloadProgress.value - song.id.toString()
                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG)
                            .show()
                }
            }
        }
    }

    companion object {
        fun provideFactory(repository: SongRepository): ViewModelProvider.Factory =
                viewModelFactory {
                    initializer { GlobalSongViewModel(repository) }
                }
    }
}
