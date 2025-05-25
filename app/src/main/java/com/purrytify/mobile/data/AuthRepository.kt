package com.purrytify.mobile.data

import android.util.Log
import com.purrytify.mobile.api.AuthService
import com.purrytify.mobile.api.UserService
import com.purrytify.mobile.api.LoginRequest
import com.purrytify.mobile.api.ProfileResponse
import com.purrytify.mobile.api.RefreshTokenRequest
import com.purrytify.mobile.api.RefreshTokenResponse
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class AuthRepository(
    private val tokenManager: TokenManager,
    private val authService: AuthService,
    private val userService: UserService
) {

    suspend fun login(email: String, password: String): Boolean {
        Log.d("AuthRepository", "Login called with email: $email")
        return try {
            val response = authService.login(LoginRequest(email, password))
            Log.d("AuthRepository", "Login response: $response")
            if (response.isSuccessful) {
                val tokens = response.body()
                if (tokens != null) {
                    Log.d("AuthRepository", "Login successful, saving tokens")
                    tokenManager.saveTokens(tokens.accessToken, tokens.refreshToken)
                    true
                } else {
                    Log.e("AuthRepository", "Login failed: Response body is null")
                    false
                }
            } else {
                Log.e(
                    "AuthRepository",
                    "Login failed: Response not successful, code: ${response.code()}, message: ${response.message()}"
                )
                false
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Exception during login: ${e.message}", e)
            false
        }
    }

    suspend fun logout() {
        tokenManager.clearTokens()
    }

    suspend fun verifyToken(accessToken: String): Boolean {
        val response = authService.verifyToken("Bearer $accessToken")
        return response.isSuccessful
    }

    suspend fun refreshToken(refreshToken: String): RefreshTokenResponse? {
        val response = authService.refreshToken(RefreshTokenRequest(refreshToken))
        return if (response.isSuccessful) {
            response.body()
        } else {
            null
        }
    }

    suspend fun getProfile(): Result<ProfileResponse> {
        val token = tokenManager.accessToken.first()
        if (token == null) {
            Log.w("AuthRepository", "Cannot get profile, access token is null.")
            return Result.failure(Exception("User not logged in"))
        }
        Log.d("AuthRepository", "Fetching profile with token: Bearer $token")
        return try {
            val response = userService.getProfile("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                Log.d("AuthRepository", "Profile fetched successfully: ${response.body()}")
                Result.success(response.body()!!)
            } else {
                Log.e(
                    "AuthRepository",
                    "Get profile failed: Code: ${response.code()}, Message: ${response.message()}, Body: ${
                        response.errorBody()?.string()
                    }"
                )
                Result.failure(Exception("Failed to fetch profile: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Exception during getProfile: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun editProfile(location: String?, profilePhoto: File?): Result<ProfileResponse> {
        val token = tokenManager.accessToken.first()
        if (token == null) {
            Log.w("AuthRepository", "Cannot edit profile, access token is null.")
            return Result.failure(Exception("User not logged in"))
        }
        Log.d("AuthRepository", "Editing profile with token: Bearer $token")

        return try {
            // Convert location to RequestBody
            val locationBody = location?.toRequestBody("text/plain".toMediaTypeOrNull())

            // Convert profile photo to MultipartBody.Part
            val profilePhotoBody = profilePhoto?.let {
                val requestFile = it.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("profilePhoto", it.name, requestFile)
            }

            val response = userService.editProfile(
                "Bearer $token",
                locationBody,
                profilePhotoBody
            )

            if (response.isSuccessful && response.body() != null) {
                Log.d("AuthRepository", "Profile edited successfully: ${response.body()}")
                val profileResponse = userService.getProfile("Bearer $token");
                if (profileResponse.isSuccessful && profileResponse.body() != null) {
                    Result.success(profileResponse.body()!!)
                } else {
                    Result.failure(Exception("Failed to fetch profile after edit: ${profileResponse.message()}"))
                }
            } else {
                Log.e(
                    "AuthRepository",
                    "Edit profile failed: Code: ${response.code()}, Message: ${response.message()}, Body: ${
                        response.errorBody()?.string()
                    }"
                )
                Result.failure(Exception("Failed to edit profile: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Exception during editProfile: ${e.message}", e)
            Result.failure(e)
        }
    }
}
