package com.purrytify.mobile.data

import android.content.Context
import android.util.Log
import com.purrytify.mobile.api.ApiClient
import com.purrytify.mobile.api.CountrySongService
import com.purrytify.mobile.data.room.CountrySong
import com.purrytify.mobile.data.room.LocalSong
import com.purrytify.mobile.data.room.LocalSongDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class CountrySongRepository(
    private val countrySongService: CountrySongService,
    private val userRepository: UserRepository,
    private val localSongDao: LocalSongDao? = null,
    private val context: Context? = null
) {
    private val supportedCountries = setOf(
        "ID", "MY", "US", "GB", "CH", "DE", "BR"
    )

    suspend fun getCountrySongs(): Result<List<CountrySong>> =
        withContext(Dispatchers.IO) {
            try {
                val profileResult = userRepository.getProfile()
                if (profileResult.isFailure) {
                    Log.e(
                        "CountrySongRepository",
                        "Failed to get user profile: " +
                            "${profileResult.exceptionOrNull()?.message}"
                    )
                    return@withContext Result.failure(
                        Exception("Failed to get user profile")
                    )
                }

                val profile = profileResult.getOrNull()
                val countryCode = profile?.location
                if (countryCode == null) {
                    Log.e(
                        "CountrySongRepository",
                        "User location is null"
                    )
                    return@withContext Result.failure(
                        Exception("User location not set")
                    )
                }

                if (!supportedCountries.contains(countryCode)) {
                    Log.e(
                        "CountrySongRepository",
                        "Country $countryCode is not supported"
                    )
                    return@withContext Result.failure(
                        Exception(
                            "Country $countryCode is not supported. " +
                                "Supported countries: " +
                                "${supportedCountries.joinToString(", ")}"
                        )
                    )
                }

                Log.d(
                    "CountrySongRepository",
                    "Fetching songs for country: $countryCode"
                )
                val response =
                    countrySongService.getCountrySongs(countryCode)
                Log.d(
                    "CountrySongRepository",
                    "Response code: ${response.code()}"
                )

                if (response.isSuccessful) {
                    response.body()?.let {
                        Log.d(
                            "CountrySongRepository",
                            "Fetched ${it.size} songs for country $countryCode"
                        )
                        Result.success(it)
                    } ?: Result.failure(
                        Exception("Response body is empty")
                    )
                } else {
                    Log.e(
                        "CountrySongRepository",
                        "Error: ${response.errorBody()?.string()}"
                    )
                    Result.failure(
                        Exception("Failed to fetch songs: ${response.code()}")
                    )
                }
            } catch (e: Exception) {
                Log.e(
                    "CountrySongRepository",
                    "Exception: ${e.message}"
                )
                Result.failure(e)
            }
        }

    suspend fun getCountrySongById(songId: Int): CountrySong? {
        val response =
            countrySongService.getCountrySongById(songId)
        return if (response.isSuccessful) response.body() else null
    }

    suspend fun isCountrySupported(): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val profileResult = userRepository.getProfile()
                if (profileResult.isFailure) {
                    return@withContext Result.failure(
                        Exception("Failed to get user profile")
                    )
                }

                val profile = profileResult.getOrNull()
                val countryCode = profile?.location
                if (countryCode == null) {
                    return@withContext Result.success(false)
                }

                return@withContext Result.success(
                    supportedCountries.contains(countryCode)
                )
            } catch (e: Exception) {
                Log.e(
                    "CountrySongRepository",
                    "Exception checking country support: ${e.message}"
                )
                Result.failure(e)
            }
        }

    fun getSupportedCountries(): Set<String> = supportedCountries

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
                val existingSong =
                    localSongDao.getDownloadedSongByTopSongId(song.id)
                if (existingSong != null) {
                    withContext(Dispatchers.Main) {
                        onComplete(existingSong)
                    }
                    return@withContext
                }

                val response =
                    countrySongService.downloadFile(song.url)
                val body = response.body()
                if (body != null) {
                    val downloadsDir = File(context.filesDir, "downloads")
                    if (!downloadsDir.exists()) downloadsDir.mkdirs()

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
                                    onProgress(
                                        currentBytes.toFloat() /
                                            totalBytes.toFloat()
                                    )
                                }
                                bytes = inputStream.read(buffer)
                            }
                        }
                    }

                    Log.d(
                        "CountrySongRepository",
                        "File downloaded to: ${file.absolutePath}"
                    )
                    Log.d(
                        "CountrySongRepository",
                        "File exists: ${file.exists()}"
                    )

                    val downloadedSong = song.toLocalSong(
                        downloadedFilePath = file.absolutePath
                    )
                    localSongDao.insert(downloadedSong)

                    withContext(Dispatchers.Main) {
                        onComplete(downloadedSong)
                    }
                } else {
                    throw Exception("Response body is null")
                }
            } catch (e: Exception) {
                Log.e(
                    "CountrySongRepository",
                    "Download error: ${e.message}", e
                )
                throw e
            }
        }
    }
}

fun createCountrySongRepository(
    tokenManager: TokenManager,
    localSongDao: LocalSongDao? = null,
    context: Context? = null
): CountrySongRepository {
    val retrofit = ApiClient.buildRetrofit(tokenManager)
    val countrySongService =
        ApiClient.createCountrySongService(retrofit)
    val userService = ApiClient.createUserService(retrofit)
    val userRepository =
        UserRepository(tokenManager, userService)
    return CountrySongRepository(
        countrySongService,
        userRepository,
        localSongDao,
        context
    )
}