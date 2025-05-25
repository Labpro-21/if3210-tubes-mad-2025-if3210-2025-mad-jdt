package com.purrytify.mobile.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_songs")
data class LocalSong(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val title: String,
        val artist: String,
        val duration: Long,
        val filePath: String,
        val artworkPath: String?,
        val isLiked: Boolean = false,
        val isDownloaded: Boolean = false,
        val dateAdded: Long = System.currentTimeMillis(),
        val topSongId: Int? = null
)
