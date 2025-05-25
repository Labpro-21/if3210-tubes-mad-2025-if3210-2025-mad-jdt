package com.purrytify.mobile.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "listening_sessions")
data class ListeningSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val songId: Long,
    val songTitle: String,
    val artist: String,
    val startTime: Long, // Timestamp when playback started
    val endTime: Long, // Timestamp when playback ended
    val duration: Long, // Duration listened in milliseconds
    val month: Int, // Month (1-12)
    val year: Int // Year
) 