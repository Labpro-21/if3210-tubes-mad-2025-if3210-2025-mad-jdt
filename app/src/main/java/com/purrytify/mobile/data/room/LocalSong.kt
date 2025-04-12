package com.purrytify.mobile.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_songs")
data class LocalSong(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String,
    val duration: Long, // Duration in milliseconds
    val filePath: String, // URI path to the audio file in external storage
    val artworkPath: String?, // URI path to the artwork file in external storage
    val isLiked: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis()
)