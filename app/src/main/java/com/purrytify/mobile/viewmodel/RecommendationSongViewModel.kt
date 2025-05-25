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
import com.purrytify.mobile.data.SongRepository
import com.purrytify.mobile.data.room.CountrySong
import com.purrytify.mobile.data.room.TopSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecommendationSong(
    val id: Int,
    val title: String,
    val artist: String,
    val artwork: String,
    val url: String,
    val duration: String,
    val country: String,
    val rank: Int,
    val type: String // "global" or "country"
) {
    companion object {
        fun fromTopSong(topSong: TopSong): RecommendationSong {
            return RecommendationSong(
                id = topSong.id,
                title = topSong.title,
                artist = topSong.artist,
                artwork = topSong.artwork,
                url = topSong.url,
                duration = topSong.duration,
                country = topSong.country,
                rank = topSong.rank,
                type = "global"
            )
        }
        
        fun fromCountrySong(countrySong: CountrySong): RecommendationSong {
            return RecommendationSong(
                id = countrySong.id,
                title = countrySong.title,
                artist = countrySong.artist,
                artwork = countrySong.artwork,
                url = countrySong.url,
                duration = countrySong.duration,
                country = countrySong.country,
                rank = countrySong.rank,
                type = "country"
            )
        }
    }
    
    fun toTopSong(): TopSong {
        return TopSong(
            id = id,
            title = title,
            artist = artist,
            artwork = artwork,
            url = url,
            duration = duration,
            country = country,
            rank = rank
        )
    }
    
    fun toCountrySong(): CountrySong {
        return CountrySong(
            id = id,
            title = title,
            artist = artist,
            artwork = artwork,
            url = url,
            duration = duration,
            country = country,
            rank = rank
        )
    }
}

class RecommendationSongViewModel(
    private val globalSongRepository: SongRepository,
    private val countrySongRepository: CountrySongRepository
) : ViewModel() {
    
    private val _recommendationSongs = MutableStateFlow<List<RecommendationSong>>(emptyList())
    val recommendationSongs: StateFlow<List<RecommendationSong>> = _recommendationSongs.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()
    
    init {
        fetchRecommendationSongs()
    }
    
    private fun fetchRecommendationSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val globalSongsResult = globalSongRepository.getTopSongs()
                val countrySongsResult = countrySongRepository.getCountrySongs()
                
                val globalSongs = globalSongsResult.getOrNull()?.take(5) ?: emptyList()
                val countrySongs = countrySongsResult.getOrNull()?.take(5) ?: emptyList()
                
                val combinedSongs = mutableListOf<RecommendationSong>()
                
                // Add global songs
                globalSongs.forEach { topSong ->
                    combinedSongs.add(RecommendationSong.fromTopSong(topSong))
                }
                
                // Add country songs
                countrySongs.forEach { countrySong ->
                    combinedSongs.add(RecommendationSong.fromCountrySong(countrySong))
                }
                
                // Shuffle the combined list for better recommendation feel
                _recommendationSongs.value = combinedSongs.shuffled()
                
                Log.d("RecommendationViewModel", "Fetched ${globalSongs.size} global songs and ${countrySongs.size} country songs")
                
            } catch (e: Exception) {
                Log.e("RecommendationViewModel", "Error fetching recommendation songs: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun downloadSong(song: RecommendationSong, context: Context) {
        viewModelScope.launch {
            try {
                _downloadProgress.value = _downloadProgress.value + (song.id.toString() to 0f)
                
                when (song.type) {
                    "global" -> {
                        globalSongRepository.downloadSong(
                            song = song.toTopSong(),
                            onProgress = { progress ->
                                _downloadProgress.value = _downloadProgress.value + (song.id.toString() to progress)
                            },
                            onComplete = { downloadedSong ->
                                viewModelScope.launch(Dispatchers.Main) {
                                    _downloadProgress.value = _downloadProgress.value - song.id.toString()
                                    Toast.makeText(context, "Download complete: ${song.title}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                    "country" -> {
                        countrySongRepository.downloadSong(
                            song = song.toCountrySong(),
                            onProgress = { progress ->
                                _downloadProgress.value = _downloadProgress.value + (song.id.toString() to progress)
                            },
                            onComplete = { downloadedSong ->
                                viewModelScope.launch(Dispatchers.Main) {
                                    _downloadProgress.value = _downloadProgress.value - song.id.toString()
                                    Toast.makeText(context, "Download complete: ${song.title}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                viewModelScope.launch(Dispatchers.Main) {
                    _downloadProgress.value = _downloadProgress.value - song.id.toString()
                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    companion object {
        fun provideFactory(
            globalSongRepository: SongRepository,
            countrySongRepository: CountrySongRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                RecommendationSongViewModel(globalSongRepository, countrySongRepository)
            }
        }
    }
}