package com.purrytify.mobile.viewmodel

import android.app.Application
import android.content.ContentUris
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.purrytify.mobile.data.room.AppDatabase
import com.purrytify.mobile.data.room.LocalSong
import com.purrytify.mobile.data.room.LocalSongRepository
import com.purrytify.mobile.ui.MiniPlayerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

class LocalSongViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: LocalSongRepository
    
    // UI States
    private val _allSongs = MutableStateFlow<List<LocalSong>>(emptyList())
    val allSongs: StateFlow<List<LocalSong>> = _allSongs.asStateFlow()
    
    private val _likedSongs = MutableStateFlow<List<LocalSong>>(emptyList())
    val likedSongs: StateFlow<List<LocalSong>> = _likedSongs.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        val localSongDao = AppDatabase.getDatabase(application).localSongDao()
        repository = LocalSongRepository(localSongDao)
        
        viewModelScope.launch {
            repository.allSongs.collectLatest { songs ->
                _allSongs.value = songs
            }
        }
        
        viewModelScope.launch {
            repository.likedSongs.collectLatest { songs ->
                _likedSongs.value = songs
            }
        }
    }
    
    /**
     * Extract metadata from audio file URI and add to database
     */
    fun addSong(audioFileUri: Uri, coverImageUri: Uri?, title: String? = null, artist: String? = null) {
    viewModelScope.launch(Dispatchers.IO) {
        _isLoading.value = true
        try {
            val context = getApplication<Application>()
            val retriever = MediaMetadataRetriever()
            
            // Get the actual file path from content URI
            val realPath = getRealPathFromUri(audioFileUri)
            
            try {
                retriever.setDataSource(context, audioFileUri)
                
                // Extract metadata
                val extractedTitle = title 
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) 
                    ?: File(audioFileUri.toString()).nameWithoutExtension
                
                val extractedArtist = artist 
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) 
                    ?: "Unknown Artist"
                
                val durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val duration = durationString?.toLongOrNull() ?: 0
                
                val localSong = LocalSong(
                    title = extractedTitle,
                    artist = extractedArtist,
                    duration = duration,
                    filePath = realPath ?: audioFileUri.toString(), // Use real path if available
                    artworkPath = coverImageUri?.toString(),
                    isLiked = false
                )
                
                repository.insert(localSong)
                _errorMessage.value = null
            } catch (e: Exception) {
                Log.e("LocalSongViewModel", "Error extracting metadata: ${e.message}")
                _errorMessage.value = "Error adding song: ${e.message}"
            } finally {
                retriever.release()
            }
        } catch (e: Exception) {
            Log.e("LocalSongViewModel", "Error adding song: ${e.message}")
            _errorMessage.value = "Error adding song: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }
}

private fun getRealPathFromUri(uri: Uri): String? {
    val context = getApplication<Application>()
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                if (columnIndex != -1) {
                    return cursor.getString(columnIndex)
                }
            }
        }
    } catch (e: Exception) {
        Log.e("LocalSongViewModel", "Error getting real path: ${e.message}")
    }
    return null
}
    
    /**
     * Update existing song in the database
     */
    fun updateSong(song: LocalSong) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.update(song)
            } catch (e: Exception) {
                Log.e("LocalSongViewModel", "Error updating song: ${e.message}")
                _errorMessage.value = "Error updating song: ${e.message}"
            }
        }
    }
    
    /**
     * Delete song from database
     */
    fun deleteSong(song: LocalSong) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.delete(song)
            } catch (e: Exception) {
                Log.e("LocalSongViewModel", "Error deleting song: ${e.message}")
                _errorMessage.value = "Error deleting song: ${e.message}"
            }
        }
    }
    
    /**
     * Update the liked status of a song
     */
    fun toggleLikeStatus(song: LocalSong) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Create updated song copy with toggled like status
                val updatedSong = song.copy(isLiked = !song.isLiked)
                // Update in database
                repository.update(updatedSong)
                val current = MiniPlayerState.currentSong
                if (current is LocalSong && current.id == song.id) {
                    MiniPlayerState.currentSong = updatedSong
                }
            } catch (e: Exception) {
                Log.e("LocalSongViewModel", "Error updating like status: ${e.message}")
                _errorMessage.value = "Error updating like status: ${e.message}"
            }
        }
    }
    
    /**
     * Format milliseconds to readable duration string
     */
    fun formatDuration(durationMs: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) - 
                TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%d:%02d", minutes, seconds)
    }
    
    /**
     * Clear any error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}