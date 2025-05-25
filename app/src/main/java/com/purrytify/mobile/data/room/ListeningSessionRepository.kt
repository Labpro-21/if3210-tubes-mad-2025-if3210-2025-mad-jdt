package com.purrytify.mobile.data.room

import java.util.Calendar
import kotlinx.coroutines.flow.Flow

class ListeningSessionRepository(private val listeningSessionDao: ListeningSessionDao) {

    val allSessions: Flow<List<ListeningSession>> = listeningSessionDao.getAllSessions()

    suspend fun insert(session: ListeningSession): Long {
        return listeningSessionDao.insert(session)
    }

    suspend fun recordListeningSession(
            songId: Long,
            songTitle: String,
            artist: String,
            startTime: Long,
            endTime: Long
    ) {
        val duration = endTime - startTime
        if (duration > 0) { // Only record if there was actual listening time
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = startTime

            val session =
                    ListeningSession(
                            songId = songId,
                            songTitle = songTitle,
                            artist = artist,
                            startTime = startTime,
                            endTime = endTime,
                            duration = duration,
                            month = calendar.get(Calendar.MONTH) + 1, // Calendar.MONTH is 0-based
                            year = calendar.get(Calendar.YEAR)
                    )

            insert(session)
        }
    }

    suspend fun getTotalListeningTimeForMonth(month: Int, year: Int): Long {
        return listeningSessionDao.getTotalListeningTimeForMonth(month, year) ?: 0L
    }

    suspend fun getTopSongForMonth(month: Int, year: Int): TopSongData? {
        return listeningSessionDao.getTopSongForMonth(month, year)
    }

    suspend fun getTopArtistForMonth(month: Int, year: Int): TopArtistData? {
        return listeningSessionDao.getTopArtistForMonth(month, year)
    }

    suspend fun getTop5SongsForMonth(month: Int, year: Int): List<TopSongData> {
        return listeningSessionDao.getTop5SongsForMonth(month, year)
    }

    suspend fun getTop5ArtistsForMonth(month: Int, year: Int): List<TopArtistData> {
        return listeningSessionDao.getTop5ArtistsForMonth(month, year)
    }

    suspend fun deleteSession(sessionId: Long) {
        listeningSessionDao.deleteSession(sessionId)
    }

    suspend fun deleteAllSessions() {
        listeningSessionDao.deleteAllSessions()
    }

    suspend fun getDailyListeningTimeForMonth(month: Int, year: Int): List<DailyListeningData> {
        return listeningSessionDao.getDailyListeningTimeForMonth(month, year)
    }

    // Helper function to format milliseconds to readable time
    fun formatListeningTime(milliseconds: Long): String {
        val totalMinutes = milliseconds / (1000 * 60)
        return if (totalMinutes < 60) {
            "$totalMinutes minutes"
        } else {
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            if (minutes == 0L) {
                "$hours hours"
            } else {
                "$hours hours $minutes minutes"
            }
        }
    }
}
