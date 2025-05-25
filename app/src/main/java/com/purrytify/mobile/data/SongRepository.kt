package com.purrytify.mobile.data

import com.purrytify.mobile.api.ApiClient
import com.purrytify.mobile.api.SongService
import com.purrytify.mobile.data.room.TopSong
import retrofit2.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SongRepository(private val songService: SongService) {

    suspend fun getTopSongs(): Result<List<TopSong>> = withContext(Dispatchers.IO) {
        try {
            val response = songService.getTopSongs()
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Response body is empty"))
            } else {
                Result.failure(Exception("Failed to fetch top songs: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSongById(songId: Int): TopSong? {
        val response = songService.getTopSongs(songId)
        return if (response.isSuccessful) response.body() else null
    }
}

// Example of how to create the repository:
fun createSongRepository(tokenManager: TokenManager): SongRepository {
    val retrofit = ApiClient.buildRetrofit(tokenManager)
    val songService = ApiClient.createSongService(retrofit)
    return SongRepository(songService)
}