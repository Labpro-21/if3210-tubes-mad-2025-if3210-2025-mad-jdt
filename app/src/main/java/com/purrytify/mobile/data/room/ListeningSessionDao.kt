package com.purrytify.mobile.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ListeningSessionDao {
    @Insert
    suspend fun insert(session: ListeningSession): Long
    
    @Query("SELECT * FROM listening_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<ListeningSession>>
    
    @Query("SELECT SUM(duration) FROM listening_sessions WHERE month = :month AND year = :year")
    suspend fun getTotalListeningTimeForMonth(month: Int, year: Int): Long?
    
    @Query("""
        SELECT songTitle, artist, COUNT(*) as playCount, SUM(duration) as totalDuration
        FROM listening_sessions 
        WHERE month = :month AND year = :year 
        GROUP BY songTitle, artist 
        ORDER BY totalDuration DESC 
        LIMIT 1
    """)
    suspend fun getTopSongForMonth(month: Int, year: Int): TopSongData?
    
    @Query("""
        SELECT artist, SUM(duration) as totalDuration
        FROM listening_sessions 
        WHERE month = :month AND year = :year 
        GROUP BY artist 
        ORDER BY totalDuration DESC 
        LIMIT 1
    """)
    suspend fun getTopArtistForMonth(month: Int, year: Int): TopArtistData?
    
    @Query("DELETE FROM listening_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)
    
    @Query("DELETE FROM listening_sessions")
    suspend fun deleteAllSessions()
}

data class TopSongData(
    val songTitle: String,
    val artist: String,
    val playCount: Int,
    val totalDuration: Long
)

data class TopArtistData(
    val artist: String,
    val totalDuration: Long
) 