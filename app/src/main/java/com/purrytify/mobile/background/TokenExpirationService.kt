package com.purrytify.mobile.background

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import com.purrytify.mobile.data.AuthRepository
import com.purrytify.mobile.data.TokenManager // Import TokenManager
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.util.Log
import com.purrytify.mobile.MainActivity
import com.purrytify.mobile.api.ApiClient

class TokenExpirationService : Service() {

    private lateinit var authRepository: AuthRepository //Remove inject
    private lateinit var tokenManager: TokenManager //Remove inject

    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + job)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("TokenService", "Service started") // Add log here
        // Create Retrofit instance
        val retrofit = ApiClient.buildRetrofit()

        // Create AuthService
        val authService = ApiClient.createAuthService(retrofit)

        // Create TokenManager, needs context
        tokenManager = TokenManager(this)

        authRepository = AuthRepository(tokenManager, authService)

        startTokenCheck()
        return START_STICKY
    }

    private fun startTokenCheck() {
        serviceScope.launch {
            while (isActive) {
                Log.d("TokenService", "Checking token...") // Add log here
                delay(TimeUnit.MINUTES.toMillis(3)) // Check every 3 minutes

                val accessToken = tokenManager.getAccessToken()
                if (accessToken != null) {
                    if (!authRepository.verifyToken(accessToken)) {
                        Log.d("TokenService", "Token expired, refreshing...") // Add log here
                        refreshTokenOrLogout()
                    } else {
                        Log.d("TokenService", "Token is valid")
                    }
                } else {
                    Log.d("TokenService", "No access token found, stopping service")
                    stopSelf()
                    break
                }
            }
        }
    }

    private suspend fun refreshTokenOrLogout() {
        val refreshToken = tokenManager.getRefreshToken()
        if (refreshToken != null) {
            val refreshResult = authRepository.refreshToken(refreshToken)
            if (refreshResult != null) {
                tokenManager.saveTokens(refreshResult.access, refreshResult.refresh)
                Log.d("TokenService", "Token refreshed successfully") // Add log here
                // Optionally:  Post a notification to inform the user that the token was refreshed
            } else {
                Log.d("TokenService", "Token refresh failed, logging out") // Add log here
                logout() // Refresh token failed, logout
            }
        } else {
            Log.d("TokenService", "No refresh token, logout") // Add log here
            logout() // No refresh token, logout
        }
    }


    private fun logout() {
        Log.d("TokenService", "Logging out...")
        // Implement your logout logic here:
        // 1. Clear tokens from DataStore
        // 2. Navigate user to login screen
        serviceScope.launch(Dispatchers.Main) {  // Switch to main thread for UI updates
            tokenManager.clearTokens()
            // Example navigation (replace with your actual navigation code)
            val intent = Intent(this@TokenExpirationService, MainActivity::class.java)
            intent.flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
            intent.putExtra("isLogout", true)
            startActivity(intent)
            stopSelf() // Stop the service after logout
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel() // Cancel coroutines when service is destroyed
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Not a bound service
    }
}
