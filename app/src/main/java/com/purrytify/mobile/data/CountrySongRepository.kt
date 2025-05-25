package com.purrytify.mobile.data

import android.util.Log
import com.purrytify.mobile.api.ApiClient
import com.purrytify.mobile.api.CountrySongService
import com.purrytify.mobile.data.room.CountrySong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CountrySongRepository(
        private val countrySongService: CountrySongService,
        private val userRepository: UserRepository
) {
    // List of supported countries
    private val supportedCountries = setOf("ID", "MY", "US", "GB", "CH", "DE", "BR")

    suspend fun getCountrySongs(): Result<List<CountrySong>> =
            withContext(Dispatchers.IO) {
                try {
                    // Get user profile to determine country
                    val profileResult = userRepository.getProfile()
                    if (profileResult.isFailure) {
                        Log.e(
                                "CountrySongRepository",
                                "Failed to get user profile: ${profileResult.exceptionOrNull()?.message}"
                        )
                        return@withContext Result.failure(Exception("Failed to get user profile"))
                    }

                    val profile = profileResult.getOrNull()
                    val countryCode = profile?.location

                    if (countryCode == null) {
                        Log.e("CountrySongRepository", "User location is null")
                        return@withContext Result.failure(Exception("User location not set"))
                    }

                    if (!supportedCountries.contains(countryCode)) {
                        Log.e("CountrySongRepository", "Country $countryCode is not supported")
                        return@withContext Result.failure(
                                Exception(
                                        "Country $countryCode is not supported. Supported countries: ${supportedCountries.joinToString(", ")}"
                                )
                        )
                    }

                    Log.d("CountrySongRepository", "Fetching songs for country: $countryCode")
                    val response = countrySongService.getCountrySongs(countryCode)
                    Log.d("CountrySongRepository", "Response code: ${response.code()}")

                    if (response.isSuccessful) {
                        response.body()?.let {
                            Log.d(
                                    "CountrySongRepository",
                                    "Fetched ${it.size} songs for country $countryCode"
                            )
                            Result.success(it)
                        }
                                ?: Result.failure(Exception("Response body is empty"))
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
        val response = countrySongService.getCountrySongById(songId)
        return if (response.isSuccessful) response.body() else null
    }

    suspend fun isCountrySupported(): Result<Boolean> =
            withContext(Dispatchers.IO) {
                try {
                    val profileResult = userRepository.getProfile()
                    if (profileResult.isFailure) {
                        return@withContext Result.failure(Exception("Failed to get user profile"))
                    }

                    val profile = profileResult.getOrNull()
                    val countryCode = profile?.location

                    if (countryCode == null) {
                        return@withContext Result.success(false)
                    }

                    return@withContext Result.success(supportedCountries.contains(countryCode))
                } catch (e: Exception) {
                    Log.e(
                            "CountrySongRepository",
                            "Exception checking country support: ${e.message}"
                    )
                    Result.failure(e)
                }
            }

    fun getSupportedCountries(): Set<String> = supportedCountries
}

// Example of how to create the repository:
fun createCountrySongRepository(tokenManager: TokenManager): CountrySongRepository {
    val retrofit = ApiClient.buildRetrofit(tokenManager)
    val countrySongService = ApiClient.createCountrySongService(retrofit)
    val userService = ApiClient.createUserService(retrofit)
    val userRepository = UserRepository(tokenManager, userService)
    return CountrySongRepository(countrySongService, userRepository)
}
