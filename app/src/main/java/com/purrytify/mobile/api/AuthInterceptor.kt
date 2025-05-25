package com.purrytify.mobile.api

import android.util.Log
import com.purrytify.mobile.data.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.net.HttpURLConnection

class AuthInterceptor(
    private val tokenManager: TokenManager,
    private val authService: AuthService
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip auth header injection for auth-related endpoints
        if (isAuthEndpoint(originalRequest)) {
            return chain.proceed(originalRequest)
        }

        // Add auth header and proceed with request
        val initialResponse = chain.proceed(addAuthHeader(originalRequest))

        // If response is 401 and it's not an auth endpoint, try to refresh token
        if (initialResponse.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
            initialResponse.close()
            Log.d("AuthInterceptor", "Received 401, attempting token refresh")

            return runBlocking {
                val refreshToken = tokenManager.getRefreshToken()
                if (refreshToken == null) {
                    Log.d("AuthInterceptor", "No refresh token available")
                    return@runBlocking initialResponse
                }

                try {
                    Log.d("AuthInterceptor", "Refreshing token with refresh token: $refreshToken")
                    val refreshResponse =
                        authService.refreshToken(RefreshTokenRequest(refreshToken))
                    Log.d("AuthInterceptor", "Token refresh response: $refreshResponse")
                    if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                        val tokens = refreshResponse.body()!!
                        tokenManager.saveTokens(tokens.accessToken, tokens.refreshToken)
                        Log.d(
                            "AuthInterceptor",
                            "Token refresh successful, retrying original request"
                        )

                        // Retry original request with new token
                        chain.proceed(addAuthHeader(originalRequest))
                    } else {
                        Log.d("AuthInterceptor", "Token refresh failed")
                        initialResponse
                    }
                } catch (e: Exception) {
                    Log.e("AuthInterceptor", "Error during token refresh", e)
                    initialResponse
                }
            }
        }

        return initialResponse
    }

    private fun isAuthEndpoint(request: Request): Boolean {
        val path = request.url.encodedPath
        return path.contains("/login") ||
                path.contains("/refresh-token") ||
                path.contains("/verify-token")
    }

    private fun addAuthHeader(request: Request): Request {
        val token = runBlocking { tokenManager.getAccessToken() } ?: return request
        return request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
    }
}