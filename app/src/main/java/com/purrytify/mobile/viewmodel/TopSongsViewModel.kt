package com.purrytify.mobile.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.purrytify.mobile.data.CountrySongRepository
import com.purrytify.mobile.data.SongRepository
import com.purrytify.mobile.data.TokenManager
import com.purrytify.mobile.data.createCountrySongRepository
import com.purrytify.mobile.data.createSongRepository
import com.purrytify.mobile.data.room.AppDatabase
import com.purrytify.mobile.data.room.ListeningSessionRepository
import com.purrytify.mobile.data.room.LocalSongRepository
import com.purrytify.mobile.data.room.TopSongData
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TopSongsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ListeningSessionRepository
    private val localSongRepository: LocalSongRepository
    private val songRepository: SongRepository
    private val countrySongRepository: CountrySongRepository

    private val _topSongs = MutableStateFlow<List<TopSongData>>(emptyList())
    val topSongs: StateFlow<List<TopSongData>> = _topSongs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentMonth = MutableStateFlow("")
    val currentMonth: StateFlow<String> = _currentMonth.asStateFlow()

    private val _totalSongs = MutableStateFlow(0)
    val totalSongs: StateFlow<Int> = _totalSongs.asStateFlow()

    // Cache to prevent repeated fetching
    private var lastFetchTime = 0L
    private val cacheValidityDuration = 30_000L // 30 seconds

    // Cache for song images
    private val songImageCache = mutableMapOf<String, String?>()

    init {
        val database = AppDatabase.getDatabase(application)
        val listeningSessionDao = database.listeningSessionDao()
        val localSongDao = database.localSongDao()
        repository = ListeningSessionRepository(listeningSessionDao)
        localSongRepository = LocalSongRepository(localSongDao)

        // Initialize online song repositories
        val tokenManager = TokenManager(application)
        songRepository = createSongRepository(tokenManager)
        countrySongRepository = createCountrySongRepository(tokenManager)

        // Only load if not already loaded recently
        if (shouldFetchData()) {
            loadTopSongs()
        }
    }

    private fun shouldFetchData(): Boolean {
        val currentTime = System.currentTimeMillis()
        return _topSongs.value.isEmpty() || (currentTime - lastFetchTime) > cacheValidityDuration
    }

    private fun loadTopSongs() {
        if (_isLoading.value) return // Prevent multiple simultaneous loads

        viewModelScope.launch {
            _isLoading.value = true

            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based
            val currentYear = calendar.get(Calendar.YEAR)

            // Set current month string
            val monthNames =
                    arrayOf(
                            "January",
                            "February",
                            "March",
                            "April",
                            "May",
                            "June",
                            "July",
                            "August",
                            "September",
                            "October",
                            "November",
                            "December"
                    )
            _currentMonth.value = "${monthNames[currentMonth - 1]} $currentYear"

            try {
                val songs = repository.getTop5SongsForMonth(currentMonth, currentYear)
                _topSongs.value = songs
                _totalSongs.value = songs.size
                lastFetchTime = System.currentTimeMillis()
            } catch (e: Exception) {
                android.util.Log.e("TopSongsViewModel", "Error loading top songs", e)
                _topSongs.value = emptyList()
                _totalSongs.value = 0
            }

            _isLoading.value = false
        }
    }

    fun getSongImageUrl(songTitle: String, artist: String): StateFlow<String?> {
        val imageUrlFlow = MutableStateFlow<String?>(null)
        val cacheKey = "$songTitle-$artist"

        // Check cache first
        if (songImageCache.containsKey(cacheKey)) {
            imageUrlFlow.value = songImageCache[cacheKey]
            return imageUrlFlow.asStateFlow()
        }

        viewModelScope.launch {
            try {
                // First, try to find in local songs database
                val allLocalSongs = localSongRepository.allSongs.first()
                val localSong =
                        allLocalSongs.find {
                            it.title.equals(songTitle, ignoreCase = true) &&
                                    it.artist.equals(artist, ignoreCase = true)
                        }

                if (localSong != null) {
                    val imageUrl = localSong.artworkPath
                    songImageCache[cacheKey] = imageUrl
                    imageUrlFlow.value = imageUrl
                    return@launch
                }

                // If not found in local songs, try online songs (TopSongs)
                val topSongsResult = songRepository.getTopSongs()
                if (topSongsResult.isSuccess) {
                    val topSongs = topSongsResult.getOrNull() ?: emptyList()
                    val onlineSong =
                            topSongs.find {
                                it.title.equals(songTitle, ignoreCase = true) &&
                                        it.artist.equals(artist, ignoreCase = true)
                            }

                    if (onlineSong != null) {
                        val imageUrl = onlineSong.artwork
                        songImageCache[cacheKey] = imageUrl
                        imageUrlFlow.value = imageUrl
                        return@launch
                    }
                }

                // If still not found, try country songs
                val countrySongsResult = countrySongRepository.getCountrySongs()
                if (countrySongsResult.isSuccess) {
                    val countrySongs = countrySongsResult.getOrNull() ?: emptyList()
                    val countrySong =
                            countrySongs.find {
                                it.title.equals(songTitle, ignoreCase = true) &&
                                        it.artist.equals(artist, ignoreCase = true)
                            }

                    if (countrySong != null) {
                        val imageUrl = countrySong.artwork
                        songImageCache[cacheKey] = imageUrl
                        imageUrlFlow.value = imageUrl
                        return@launch
                    }
                }

                // If no image found, cache null result
                songImageCache[cacheKey] = null
                imageUrlFlow.value = null
            } catch (e: Exception) {
                android.util.Log.e(
                        "TopSongsViewModel",
                        "Error getting song image for $songTitle by $artist",
                        e
                )
                songImageCache[cacheKey] = null
                imageUrlFlow.value = null
            }
        }

        return imageUrlFlow.asStateFlow()
    }

    fun refreshData() {
        lastFetchTime = 0L // Reset cache
        songImageCache.clear() // Clear image cache
        loadTopSongs()
    }
}
