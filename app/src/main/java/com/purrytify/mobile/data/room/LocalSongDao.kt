package com.purrytify.mobile.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalSongDao {
    @Query("SELECT * FROM local_songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<LocalSong>>
    
    @Query("SELECT * FROM local_songs WHERE isLiked = 1 ORDER BY title ASC")
    fun getLikedSongs(): Flow<List<LocalSong>>
    
    @Insert
    suspend fun insert(song: LocalSong): Long
    
    @Update
    suspend fun update(song: LocalSong)
    
    @Delete
    suspend fun delete(song: LocalSong)
    
    @Query("UPDATE local_songs SET isLiked = :isLiked WHERE id = :songId")
    suspend fun updateLikeStatus(songId: Long, isLiked: Boolean)
    
    @Query("SELECT * FROM local_songs WHERE id = :songId")
    suspend fun getSongById(songId: Long): LocalSong?
}