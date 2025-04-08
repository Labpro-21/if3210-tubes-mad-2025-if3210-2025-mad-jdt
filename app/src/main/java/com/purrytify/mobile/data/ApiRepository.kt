package com.purrytify.mobile.data

import com.purrytify.mobile.api.ApiClient
import com.purrytify.mobile.api.LoginRequest

class AuthRepository(private val tokenManager: TokenManager) {
    private val authService = ApiClient.authService

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
}
