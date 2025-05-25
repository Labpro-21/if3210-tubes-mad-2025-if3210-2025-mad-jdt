package com.purrytify.mobile.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.purrytify.mobile.data.room.AppDatabase
import com.purrytify.mobile.data.room.ListeningSessionRepository
import com.purrytify.mobile.data.room.TopArtistData
import com.purrytify.mobile.data.room.TopSongData
import com.purrytify.mobile.data.room.LocalSongRepository
import com.purrytify.mobile.data.SongRepository
import com.purrytify.mobile.data.CountrySongRepository
import com.purrytify.mobile.data.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

data class MonthlyListeningData(
    val month: String,
    val totalMinutes: Long,
    val formattedTime: String,
    val topArtist: String?,
    val topArtistImageUrl: String?,
    val topSong: String?,
    val topSongImageUrl: String?,
    val hasAchievement: Boolean = false,
    val achievementText: String? = null,
    val achievementSubtext: String? = null,
    val achievementDate: String? = null
)

class SoundCapsuleViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ListeningSessionRepository
    private val localSongRepository: LocalSongRepository
    private val songRepository: SongRepository
    private val countrySongRepository: CountrySongRepository
    
    private val _monthlyData = MutableStateFlow<List<MonthlyListeningData>>(emptyList())
    val monthlyData: StateFlow<List<MonthlyListeningData>> = _monthlyData.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        val database = AppDatabase.getDatabase(application)
        val listeningSessionDao = database.listeningSessionDao()
        val localSongDao = database.localSongDao()
        repository = ListeningSessionRepository(listeningSessionDao)
        localSongRepository = LocalSongRepository(localSongDao)
        
        // Initialize online song repositories
        val tokenManager = TokenManager(application)
        songRepository = com.purrytify.mobile.data.createSongRepository(tokenManager)
        countrySongRepository = com.purrytify.mobile.data.createCountrySongRepository(tokenManager)
        
        loadMonthlyData()
    }
    
    private fun loadMonthlyData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based
            
            val monthlyDataList = mutableListOf<MonthlyListeningData>()
            
            // Get data for current month and previous months, but only include months with actual data
            for (monthOffset in 0..5) { // Check up to 6 months back
                val targetMonth = if (currentMonth - monthOffset > 0) {
                    currentMonth - monthOffset
                } else {
                    12 + (currentMonth - monthOffset)
                }
                val targetYear = if (currentMonth - monthOffset > 0) {
                    currentYear
                } else {
                    currentYear - 1
                }
                
                val totalTime = repository.getTotalListeningTimeForMonth(targetMonth, targetYear)
                
                // Only add months that have actual listening data (more than 0 minutes)
                if (totalTime > 0) {
                    val topArtist = repository.getTopArtistForMonth(targetMonth, targetYear)
                    val topSong = repository.getTopSongForMonth(targetMonth, targetYear)
                    
                    val monthName = getMonthName(targetMonth, targetYear)
                    val totalMinutes = totalTime / (1000 * 60) // Convert to minutes
                    val formattedTime = "$totalMinutes minutes"
                    
                    // Add achievement for current month if listening time is significant
                    val hasAchievement = monthOffset == 0 && totalMinutes > 300 // 5+ hours
                    val achievementText = if (hasAchievement) "You had a great listening month!" else null
                    val achievementSubtext = if (hasAchievement) "You listened to music for over 5 hours this month. Keep it up!" else null
                    val achievementDate = if (hasAchievement) getDateRange(targetMonth, targetYear) else null
                    
                    // Get image URLs from local songs database
                    val topArtistImageUrl = getArtistImageUrl(topArtist?.artist)
                    val topSongImageUrl = getSongImageUrl(topSong?.songTitle, topSong?.artist)
                    
                    monthlyDataList.add(
                        MonthlyListeningData(
                            month = monthName,
                            totalMinutes = totalMinutes,
                            formattedTime = formattedTime,
                            topArtist = topArtist?.artist,
                            topArtistImageUrl = topArtistImageUrl,
                            topSong = topSong?.songTitle,
                            topSongImageUrl = topSongImageUrl,
                            hasAchievement = hasAchievement,
                            achievementText = achievementText,
                            achievementSubtext = achievementSubtext,
                            achievementDate = achievementDate
                        )
                    )
                }
                
                // Limit to showing maximum 2 months with data
                if (monthlyDataList.size >= 2) break
            }
            
            _monthlyData.value = monthlyDataList
            _isLoading.value = false
        }
    }
    
    private fun getMonthName(month: Int, year: Int): String {
        val monthNames = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        return "${monthNames[month - 1]} $year"
    }
    
    private fun getDateRange(month: Int, year: Int): String {
        val monthNames = arrayOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
        return "${monthNames[month - 1]} 1-${getDaysInMonth(month, year)}, $year"
    }
    
    private fun getDaysInMonth(month: Int, year: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1) // Calendar.MONTH is 0-based
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    
    fun refreshData() {
        loadMonthlyData()
    }
    
    private suspend fun getArtistImageUrl(artistName: String?): String? {
        if (artistName == null) return null
        
        android.util.Log.d("SoundCapsule", "Looking for artist: '$artistName'")
        
        // First, try to find in local songs database
        try {
            val allLocalSongs = localSongRepository.allSongs.first()
            val localSongByArtist = allLocalSongs.find { it.artist.equals(artistName, ignoreCase = true) }
            if (localSongByArtist != null) {
                android.util.Log.d("SoundCapsule", "Found local artist song: ${localSongByArtist.title}, artworkPath: ${localSongByArtist.artworkPath}")
                return localSongByArtist.artworkPath
            }
        } catch (e: Exception) {
            android.util.Log.e("SoundCapsule", "Error searching local songs for artist", e)
        }
        
        // If not found in local songs, try online songs (TopSongs)
        try {
            val topSongsResult = songRepository.getTopSongs()
            if (topSongsResult.isSuccess) {
                val topSongs = topSongsResult.getOrNull() ?: emptyList()
                val onlineSongByArtist = topSongs.find { it.artist.equals(artistName, ignoreCase = true) }
                if (onlineSongByArtist != null) {
                    android.util.Log.d("SoundCapsule", "Found online artist song: ${onlineSongByArtist.title}, artwork: ${onlineSongByArtist.artwork}")
                    return onlineSongByArtist.artwork
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SoundCapsule", "Error searching online top songs for artist", e)
        }
        
        // If still not found, try country songs
        try {
            val countrySongsResult = countrySongRepository.getCountrySongs()
            if (countrySongsResult.isSuccess) {
                val countrySongs = countrySongsResult.getOrNull() ?: emptyList()
                val countrySongByArtist = countrySongs.find { it.artist.equals(artistName, ignoreCase = true) }
                if (countrySongByArtist != null) {
                    android.util.Log.d("SoundCapsule", "Found country artist song: ${countrySongByArtist.title}, artwork: ${countrySongByArtist.artwork}")
                    return countrySongByArtist.artwork
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SoundCapsule", "Error searching country songs for artist", e)
        }
        
        android.util.Log.d("SoundCapsule", "Artist not found in any repository")
        return null
    }
    
    private suspend fun getSongImageUrl(songTitle: String?, artist: String?): String? {
        if (songTitle == null) return null
        
        android.util.Log.d("SoundCapsule", "Looking for song: '$songTitle' by '$artist'")
        
        // First, try to find in local songs database
        try {
            val allLocalSongs = localSongRepository.allSongs.first()
            android.util.Log.d("SoundCapsule", "Total local songs in database: ${allLocalSongs.size}")
            
            val localSong = allLocalSongs.find { 
                it.title.equals(songTitle, ignoreCase = true) && 
                (artist == null || it.artist.equals(artist, ignoreCase = true))
            }
            
            if (localSong != null) {
                android.util.Log.d("SoundCapsule", "Found local song: ${localSong.title}, artworkPath: ${localSong.artworkPath}")
                return localSong.artworkPath
            }
        } catch (e: Exception) {
            android.util.Log.e("SoundCapsule", "Error searching local songs", e)
        }
        
        // If not found in local songs, try online songs (TopSongs)
        try {
            val topSongsResult = songRepository.getTopSongs()
            if (topSongsResult.isSuccess) {
                val topSongs = topSongsResult.getOrNull() ?: emptyList()
                android.util.Log.d("SoundCapsule", "Total top songs available: ${topSongs.size}")
                
                val onlineSong = topSongs.find { 
                    it.title.equals(songTitle, ignoreCase = true) && 
                    (artist == null || it.artist.equals(artist, ignoreCase = true))
                }
                
                if (onlineSong != null) {
                    android.util.Log.d("SoundCapsule", "Found online song: ${onlineSong.title}, artwork: ${onlineSong.artwork}")
                    return onlineSong.artwork
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SoundCapsule", "Error searching online top songs", e)
        }
        
        // If still not found, try country songs
        try {
            val countrySongsResult = countrySongRepository.getCountrySongs()
            if (countrySongsResult.isSuccess) {
                val countrySongs = countrySongsResult.getOrNull() ?: emptyList()
                android.util.Log.d("SoundCapsule", "Total country songs available: ${countrySongs.size}")
                
                val countrySong = countrySongs.find { 
                    it.title.equals(songTitle, ignoreCase = true) && 
                    (artist == null || it.artist.equals(artist, ignoreCase = true))
                }
                
                if (countrySong != null) {
                    android.util.Log.d("SoundCapsule", "Found country song: ${countrySong.title}, artwork: ${countrySong.artwork}")
                    return countrySong.artwork
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SoundCapsule", "Error searching country songs", e)
        }
        
        android.util.Log.d("SoundCapsule", "Song not found in any repository")
        return null
    }
} 