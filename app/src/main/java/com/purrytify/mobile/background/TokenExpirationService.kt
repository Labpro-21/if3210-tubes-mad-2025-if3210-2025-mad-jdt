package com.purrytify.mobile.background

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import com.purrytify.mobile.data.AuthRepository
import com.purrytify.mobile.data.TokenManager // Import TokenManager
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK

@AndroidEntryPoint
class TokenExpirationService : Service() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var tokenManager: TokenManager

    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + job)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startTokenCheck()
        return START_STICKY
    }

    private fun startTokenCheck() {
        serviceScope.launch {
            while (isActive) {
                delay(TimeUnit.MINUTES.toMillis(2)) // Check every 2 minutes

                val accessToken = tokenManager.getAccessToken()
                if (accessToken != null) {
                    if (!authRepository.verifyToken(accessToken)) {
                        refreshTokenOrLogout()
                    }
                } else {
                    // No access token, maybe user is logged out. Stop checking
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
                // Optionally:  Post a notification to inform the user that the token was refreshed
            } else {
                logout() // Refresh token failed, logout
            }
        } else {
            logout() // No refresh token, logout
        }
    }


    private fun logout() {
        // Implement your logout logic here:
        // 1. Clear tokens from DataStore
        // 2. Navigate user to login screen
        serviceScope.launch(Dispatchers.Main) {  // Switch to main thread for UI updates
            tokenManager.clearTokens()
            // Example navigation (replace with your actual navigation code)
            val intent = Intent(this@TokenExpirationService, LoginActivity::class.java)
            intent.flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
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
