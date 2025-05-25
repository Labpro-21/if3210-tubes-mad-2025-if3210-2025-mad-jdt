package com.purrytify.mobile.data.room

import kotlinx.coroutines.flow.Flow

class LocalSongRepository(private val localSongDao: LocalSongDao) {
    val allSongs: Flow<List<LocalSong>> = localSongDao.getAllSongs()
    val likedSongs: Flow<List<LocalSong>> = localSongDao.getLikedSongs()
    val downloadedSongs: Flow<List<LocalSong>> = localSongDao.getDownloadedSongs()

    suspend fun insert(song: LocalSong): Long {
        return localSongDao.insert(song)
    }

    suspend fun update(song: LocalSong) {
        localSongDao.update(song)
    }

    suspend fun delete(song: LocalSong) {
        localSongDao.delete(song)
    }

    suspend fun updateLikeStatus(songId: Long, isLiked: Boolean) {
        localSongDao.updateLikeStatus(songId, isLiked)
    }

    suspend fun getSongById(songId: Long): LocalSong? {
        return localSongDao.getSongById(songId)
    }
}
