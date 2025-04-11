package com.purrytify.mobile.data

import android.util.Log
import com.purrytify.mobile.api.AuthService
import com.purrytify.mobile.api.LoginRequest
import com.purrytify.mobile.api.RefreshTokenRequest
import com.purrytify.mobile.api.RefreshTokenResponse
import java.lang.Exception

class AuthRepository (  // Remove @Inject
    private val tokenManager: TokenManager,
    private val authService: AuthService
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
                    tokenManager.saveTokens(tokens.access, tokens.refresh)
                    true
                } else {
                    Log.e("AuthRepository", "Login failed: Response body is null")
                    false
                }
            } else {
                Log.e("AuthRepository", "Login failed: Response not successful, code: ${response.code()}, message: ${response.message()}")
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
}
