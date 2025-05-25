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
) {
    fun toLocalSong(downloadedFilePath: String? = null): LocalSong {
        return LocalSong(
            id = 0, // Let Room auto-generate the ID
            title = title,
            artist = artist,
            filePath = downloadedFilePath ?: url,
            artworkPath = artwork,
            duration = duration.toLongOrNull() ?: 0L,
            isLiked = false,
            isDownloaded = downloadedFilePath != null,
            topSongId = id // Add this field to track the original TopSong ID
        )
    }
}