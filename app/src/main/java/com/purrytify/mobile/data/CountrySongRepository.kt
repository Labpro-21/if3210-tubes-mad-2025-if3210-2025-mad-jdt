package com.purrytify.mobile.data

import android.util.Log
import com.purrytify.mobile.api.ApiClient
import com.purrytify.mobile.api.CountrySongService
import com.purrytify.mobile.data.room.CountrySong
import retrofit2.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CountrySongRepository(private val countrySongService: CountrySongService) {
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
}

// Example of how to create the repository:
fun createCountrySongRepository(tokenManager: TokenManager): CountrySongRepository {
    val retrofit = ApiClient.buildRetrofit(tokenManager)
    val countrySongService = ApiClient.createCountrySongService(retrofit)
    return CountrySongRepository(countrySongService)
}