package com.purrytify.mobile.data.room

data class CountrySong(
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
                id = 0,
                title = title,
                artist = artist,
                filePath = downloadedFilePath ?: url,
                artworkPath = artwork,
                duration = duration.toLongOrNull() ?: 0L,
                isLiked = false,
                isDownloaded = downloadedFilePath != null,
                topSongId = id
        )
    }
}
