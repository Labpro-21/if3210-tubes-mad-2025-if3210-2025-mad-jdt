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
import com.purrytify.mobile.data.room.TopArtistData
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TopArtistsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ListeningSessionRepository
    private val localSongRepository: LocalSongRepository
    private val songRepository: SongRepository
    private val countrySongRepository: CountrySongRepository

    private val _topArtists = MutableStateFlow<List<TopArtistData>>(emptyList())
    val topArtists: StateFlow<List<TopArtistData>> = _topArtists.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentMonth = MutableStateFlow("")
    val currentMonth: StateFlow<String> = _currentMonth.asStateFlow()

    private val _totalArtists = MutableStateFlow(0)
    val totalArtists: StateFlow<Int> = _totalArtists.asStateFlow()

    // Cache to prevent repeated fetching
    private var lastFetchTime = 0L
    private val cacheValidityDuration = 30_000L // 30 seconds

    // Cache for artist images
    private val artistImageCache = mutableMapOf<String, String?>()

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
            loadTopArtists()
        }
    }

    private fun shouldFetchData(): Boolean {
        val currentTime = System.currentTimeMillis()
        return _topArtists.value.isEmpty() || (currentTime - lastFetchTime) > cacheValidityDuration
    }

    private fun loadTopArtists() {
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
                val artists = repository.getTop5ArtistsForMonth(currentMonth, currentYear)
                _topArtists.value = artists
                _totalArtists.value = artists.size
                lastFetchTime = System.currentTimeMillis()
            } catch (e: Exception) {
                android.util.Log.e("TopArtistsViewModel", "Error loading top artists", e)
                _topArtists.value = emptyList()
                _totalArtists.value = 0
            }

            _isLoading.value = false
        }
    }

    fun getArtistImageUrl(artistName: String): StateFlow<String?> {
        val imageUrlFlow = MutableStateFlow<String?>(null)

        // Check cache first
        if (artistImageCache.containsKey(artistName)) {
            imageUrlFlow.value = artistImageCache[artistName]
            return imageUrlFlow.asStateFlow()
        }

        viewModelScope.launch {
            try {
                // First, try to find in local songs database
                val allLocalSongs = localSongRepository.allSongs.first()
                val localSongByArtist =
                        allLocalSongs.find { it.artist.equals(artistName, ignoreCase = true) }
                if (localSongByArtist != null) {
                    val imageUrl = localSongByArtist.artworkPath
                    artistImageCache[artistName] = imageUrl
                    imageUrlFlow.value = imageUrl
                    return@launch
                }

                // If not found in local songs, try online songs (TopSongs)
                val topSongsResult = songRepository.getTopSongs()
                if (topSongsResult.isSuccess) {
                    val topSongs = topSongsResult.getOrNull() ?: emptyList()
                    val onlineSongByArtist =
                            topSongs.find { it.artist.equals(artistName, ignoreCase = true) }
                    if (onlineSongByArtist != null) {
                        val imageUrl = onlineSongByArtist.artwork
                        artistImageCache[artistName] = imageUrl
                        imageUrlFlow.value = imageUrl
                        return@launch
                    }
                }

                // If still not found, try country songs
                val countrySongsResult = countrySongRepository.getCountrySongs()
                if (countrySongsResult.isSuccess) {
                    val countrySongs = countrySongsResult.getOrNull() ?: emptyList()
                    val countrySongByArtist =
                            countrySongs.find { it.artist.equals(artistName, ignoreCase = true) }
                    if (countrySongByArtist != null) {
                        val imageUrl = countrySongByArtist.artwork
                        artistImageCache[artistName] = imageUrl
                        imageUrlFlow.value = imageUrl
                        return@launch
                    }
                }

                // If no image found, cache null result
                artistImageCache[artistName] = null
                imageUrlFlow.value = null
            } catch (e: Exception) {
                android.util.Log.e(
                        "TopArtistsViewModel",
                        "Error getting artist image for $artistName",
                        e
                )
                artistImageCache[artistName] = null
                imageUrlFlow.value = null
            }
        }

        return imageUrlFlow.asStateFlow()
    }

    fun refreshData() {
        lastFetchTime = 0L // Reset cache
        artistImageCache.clear() // Clear image cache
        loadTopArtists()
    }
}
