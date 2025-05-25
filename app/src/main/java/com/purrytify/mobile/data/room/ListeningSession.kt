package com.purrytify.mobile.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "listening_sessions")
data class ListeningSession(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val songId: Long,
        val songTitle: String,
        val artist: String,
        val startTime: Long,
        val endTime: Long,
        val duration: Long,
        val month: Int,
        val year: Int
)
