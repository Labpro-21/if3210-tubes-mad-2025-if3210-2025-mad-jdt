package com.purrytify.mobile.data.room

data class TopSong(
    val id: Int,
    val title: String,
    val artist: String,
    val artwork: String,
    val url: String,
    val duration: String,
    val country: String,
    val rank: Int
){
    fun toLocalSong(): LocalSong {
        return LocalSong(
            id = id.toLong(), // Convert id to Long for LocalSong
            title = title,
            artist = artist,
            filePath = url, // Use the URL as filePath
            artworkPath = artwork,
            duration = duration.toLongOrNull() ?: 0L, // Convert duration to Long, default to 0 if parsing fails
            isLiked = false
        )
    }
}