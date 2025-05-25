package com.purrytify.mobile.utils

import android.content.Context
import com.purrytify.mobile.data.room.AppDatabase
import com.purrytify.mobile.data.room.ListeningSessionRepository
import com.purrytify.mobile.data.room.LocalSong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

object ListeningTracker {
    private var currentSessionStartTime: Long? = null
    private var lastRecordedTime: Long? = null
    private var currentSong: LocalSong? = null
    private var repository: ListeningSessionRepository? = null
    private var trackingJob: Job? = null
    
    private const val UPDATE_INTERVAL_MS = 10_000L // 10 seconds
    private const val MIN_SESSION_DURATION_MS = 10_000L // 10 seconds minimum
    
    fun initialize(context: Context) {
        if (repository == null) {
            val database = AppDatabase.getDatabase(context)
            repository = ListeningSessionRepository(database.listeningSessionDao())
        }
    }
    
    fun startListening(song: LocalSong) {
        // Stop any existing tracking
        stopTracking()
        
        currentSong = song
        currentSessionStartTime = System.currentTimeMillis()
        lastRecordedTime = currentSessionStartTime
        
        // Start periodic tracking
        startPeriodicTracking()
    }
    
    private fun startPeriodicTracking() {
        trackingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && currentSong != null) {
                delay(UPDATE_INTERVAL_MS)
                
                val song = currentSong
                val lastRecorded = lastRecordedTime
                
                if (song != null && lastRecorded != null) {
                    val currentTime = System.currentTimeMillis()
                    val sessionDuration = currentTime - lastRecorded
                    
                    // Record this 10-second segment
                    if (sessionDuration >= UPDATE_INTERVAL_MS) {
                        repository?.recordListeningSession(
                            songId = song.id,
                            songTitle = song.title,
                            artist = song.artist,
                            startTime = lastRecorded,
                            endTime = currentTime
                        )
                        lastRecordedTime = currentTime
                    }
                }
            }
        }
    }
    
    private fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
    }
    
    fun stopListening() {
        val song = currentSong
        val lastRecorded = lastRecordedTime
        
        if (song != null && lastRecorded != null) {
            val endTime = System.currentTimeMillis()
            val remainingDuration = endTime - lastRecorded
            
            // Record any remaining time if it's significant (at least 5 seconds)
            if (remainingDuration >= 5_000) {
                CoroutineScope(Dispatchers.IO).launch {
                    repository?.recordListeningSession(
                        songId = song.id,
                        songTitle = song.title,
                        artist = song.artist,
                        startTime = lastRecorded,
                        endTime = endTime
                    )
                }
            }
        }
        
        stopTracking()
        currentSong = null
        currentSessionStartTime = null
        lastRecordedTime = null
    }
    
    fun pauseListening() {
        // Record any remaining time and stop tracking
        stopListening()
    }
    
    fun resumeListening(song: LocalSong) {
        // Start a new tracking session
        startListening(song)
    }
    
    fun switchSong(newSong: LocalSong) {
        // Stop tracking the current song and start tracking the new one
        stopListening()
        startListening(newSong)
    }
} 