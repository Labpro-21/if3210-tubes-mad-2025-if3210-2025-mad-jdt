package com.purrytify.mobile.data

import android.content.Context
import android.util.Log
import com.purrytify.mobile.api.ApiClient
import com.purrytify.mobile.api.CountrySongService
import com.purrytify.mobile.data.room.CountrySong
import com.purrytify.mobile.data.room.LocalSong
import com.purrytify.mobile.data.room.LocalSongDao
import retrofit2.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class CountrySongRepository(
    private val countrySongService: CountrySongService,
    private val localSongDao: LocalSongDao? = null,
    private val context: Context? = null
) {
    suspend fun getCountrySongs(): Result<List<CountrySong>> = withContext(Dispatchers.IO) {
        try {
            val response = countrySongService.getCountrySongs()
            Log.d("CountrySongRepository", "Response code: ${response.code()}")
            
            if (response.isSuccessful) {
                response.body()?.let {
                    Log.d("CountrySongRepository", "Fetched ${it.size} songs")
                    Result.success(it)
                } ?: Result.failure(Exception("Response body is empty"))
            } else {
                Log.e("CountrySongRepository", "Error: ${response.errorBody()?.string()}")
                Result.failure(Exception("Failed to fetch songs: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("CountrySongRepository", "Exception: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getCountrySongById(songId: Int): CountrySong? {
        val response = countrySongService.getCountrySongs(songId)
        return if (response.isSuccessful) response.body() else null
    }

    suspend fun downloadSong(
        song: CountrySong,
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

                val response = countrySongService.downloadFile(song.url)
                val body = response.body()

                if (body != null) {
                    // Create downloads directory in internal storage
                    val downloadsDir = File(context.filesDir, "downloads")
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs()
                    }

                    val fileName = "country_${song.id}.mp3"
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

                    Log.d("CountrySongRepository", "File downloaded to: ${file.absolutePath}")
                    Log.d("CountrySongRepository", "File exists: ${file.exists()}")

                    val downloadedSong = song.toLocalSong(downloadedFilePath = file.absolutePath)
                    localSongDao.insert(downloadedSong)

                    withContext(Dispatchers.Main) {
                        onComplete(downloadedSong)
                    }
                } else {
                    throw Exception("Response body is null")
                }
            } catch (e: Exception) {
                Log.e("CountrySongRepository", "Download error: ${e.message}", e)
                throw e
            }
        }
    }
}

// Update the factory function
fun createCountrySongRepository(
    tokenManager: TokenManager,
    localSongDao: LocalSongDao? = null,
    context: Context? = null
): CountrySongRepository {
    val retrofit = ApiClient.buildRetrofit(tokenManager)
    val countrySongService = ApiClient.createCountrySongService(retrofit)
    return CountrySongRepository(countrySongService, localSongDao, context)
}