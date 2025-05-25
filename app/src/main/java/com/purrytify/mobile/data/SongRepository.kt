package com.purrytify.mobile.data

import android.content.Context
import android.util.Log

import com.purrytify.mobile.api.ApiClient
import com.purrytify.mobile.api.SongService
import com.purrytify.mobile.data.room.LocalSong
import com.purrytify.mobile.data.room.LocalSongDao
import com.purrytify.mobile.data.room.TopSong
import retrofit2.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class SongRepository(
    private val songService: SongService,
    private val localSongDao: LocalSongDao? = null,
    private val context: Context? = null
) {
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

    suspend fun downloadSong(
        song: TopSong,
        onProgress: (Float) -> Unit,
        onComplete: (LocalSong) -> Unit
    ) {
        if (context == null || localSongDao == null) {
            throw Exception("Context or LocalSongDao not available")
        }

        withContext(Dispatchers.IO) {
            try {
                // Check if song is already downloaded
                val existingSong = localSongDao.getDownloadedSongByTopSongId(song.id)
                if (existingSong != null) {
                    withContext(Dispatchers.Main) {
                        onComplete(existingSong)
                    }
                    return@withContext
                }

                val response = songService.downloadFile(song.url)
                val body = response.body()

                if (body != null) {
                    // Create downloads directory in internal storage
                    val downloadsDir = File(context.filesDir, "downloads")
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs()
                    }

                    val fileName = "${song.id}.mp3"
                    val file = File(downloadsDir, fileName)

                    FileOutputStream(file).use { outputStream ->
                        val buffer = ByteArray(8192)
                        val totalBytes = body.contentLength()
                        var currentBytes = 0L

                        body.byteStream().use { inputStream ->
                            var bytes = inputStream.read(buffer)
                            while (bytes >= 0) {
                                outputStream.write(buffer, 0, bytes)
                                currentBytes += bytes
                                if (totalBytes > 0) {
                                    onProgress(currentBytes.toFloat() / totalBytes.toFloat())
                                }
                                bytes = inputStream.read(buffer)
                            }
                        }
                    }

                    android.util.Log.d("SongRepository", "File downloaded to: ${file.absolutePath}")
                    android.util.Log.d("SongRepository", "File exists: ${file.exists()}")

                    val downloadedSong = song.toLocalSong(downloadedFilePath = file.absolutePath)
                    localSongDao.insert(downloadedSong)

                    withContext(Dispatchers.Main) {
                        onComplete(downloadedSong)
                    }
                } else {
                    throw Exception("Response body is null")
                }
            } catch (e: Exception) {
                android.util.Log.e("SongRepository", "Download error: ${e.message}", e)
                throw e
            }
        }
    }
        
    suspend fun getSongById(songId: Int): TopSong? {
        Log.d("MiniPlayer", "getSongById called with id=$songId")
        val response = songService.getTopSongs(songId)
        Log.d("MiniPlayer", "getSongById result: $response")
        return if (response.isSuccessful) response.body() else null
    }
}

// Update the factory function
fun createSongRepository(
    tokenManager: TokenManager,
    localSongDao: LocalSongDao? = null,
    context: Context? = null
): SongRepository {
    val retrofit = ApiClient.buildRetrofit(tokenManager)
    val songService = ApiClient.createSongService(retrofit)
    return SongRepository(songService, localSongDao, context)
}