package com.purrytify.mobile.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalSongDao {
    @Query("SELECT * FROM local_songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<LocalSong>>
    
    @Query("SELECT * FROM local_songs WHERE isLiked = 1 ORDER BY title ASC")
    fun getLikedSongs(): Flow<List<LocalSong>>

    @Query("SELECT * FROM local_songs WHERE isDownloaded = 1 ORDER BY dateAdded DESC")
    fun getDownloadedSongs(): Flow<List<LocalSong>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: LocalSong): Long
    
    @Update
    suspend fun update(song: LocalSong)
    
    @Delete
    suspend fun delete(song: LocalSong)
    
    @Query("UPDATE local_songs SET isLiked = :isLiked WHERE id = :songId")
    suspend fun updateLikeStatus(songId: Long, isLiked: Boolean)
    
    @Query("SELECT * FROM local_songs WHERE id = :songId")
    suspend fun getSongById(songId: Long): LocalSong?
    
    @Query("SELECT * FROM local_songs WHERE topSongId = :topSongId AND isDownloaded = 1")
    suspend fun getDownloadedSongByTopSongId(topSongId: Int): LocalSong?
}