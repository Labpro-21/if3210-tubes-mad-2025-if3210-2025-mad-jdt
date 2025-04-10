package com.purrytify.mobile.data

import com.purrytify.mobile.api.AuthService
import com.purrytify.mobile.api.LoginRequest
import com.purrytify.mobile.api.RefreshTokenRequest
import com.purrytify.mobile.api.RefreshTokenResponse
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val tokenManager: TokenManager,
    private val authService: AuthService
) {

    suspend fun login(email: String, password: String): Boolean {
        val response = authService.login(LoginRequest(email, password))
        return if (response.isSuccessful && response.body() != null) {
            val tokens = response.body()!!
            tokenManager.saveTokens(tokens.access, tokens.refresh)
            true
        } else {
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
