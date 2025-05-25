package com.purrytify.mobile.background

import android.app.Service
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.IBinder
import android.util.Log
import com.purrytify.mobile.MainActivity
import com.purrytify.mobile.api.ApiClient
import com.purrytify.mobile.data.AuthRepository
import com.purrytify.mobile.data.TokenManager
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TokenExpirationService : Service() {

    private lateinit var authRepository: AuthRepository
    private lateinit var tokenManager: TokenManager

    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + job)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("TokenService", "Service started")
        val retrofit = ApiClient.buildRetrofit()

        val authService = ApiClient.createAuthService(retrofit)

        tokenManager = TokenManager(this)

        authRepository = AuthRepository(tokenManager, authService)

        startTokenCheck()
        return START_STICKY
    }

    private fun startTokenCheck() {
        serviceScope.launch {
            while (isActive) {
                Log.d("TokenService", "1 Minute started")
                delay(TimeUnit.MINUTES.toMillis(1)) // 1 Minute

                val accessToken = tokenManager.getAccessToken()
                if (accessToken != null) {
                    if (!authRepository.verifyToken(accessToken)) {
                        Log.d("TokenService", "Token expired, refreshing...")
                        refreshTokenOrLogout()
                    } else {
                        Log.d("TokenService", "Token is valid")
                    }
                } else {
                    Log.d("TokenService", "No access token found, logout")
                    logout()
                    stopSelf()
                    break
                }
            }
        }
    }

    private suspend fun refreshTokenOrLogout() {
        val refreshToken = tokenManager.getRefreshToken()
        Log.d("TokenService", "Refresh token: $refreshToken")
        if (refreshToken != null) {
            val refreshResult = authRepository.refreshToken(refreshToken)
            if (refreshResult != null) {
                Log.d("TokenService", "Refresh result: ${refreshResult.toString()}")
                tokenManager.clearTokens()
                Log.d("TokenService", "Old token cleared")
                tokenManager.saveTokens(refreshResult.accessToken, refreshResult.refreshToken)
                Log.d("TokenService", "Token refreshed successfully")
            } else {
                Log.d("TokenService", "Token refresh failed, logging out")
                logout()
            }
        } else {
            Log.d("TokenService", "No refresh token, logout")
            logout()
        }
    }

    private fun logout() {
        Log.d("TokenService", "Logging out...")
        runBlocking(Dispatchers.Main) {
            try {
                tokenManager.clearTokensSync()
                Log.d("TokenService", "Tokens cleared successfully")

                val intent =
                        Intent(this@TokenExpirationService, MainActivity::class.java).apply {
                            flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
                            putExtra("isLogout", true)
                        }
                startActivity(intent)
                Log.d("TokenService", "Started MainActivity with logout flag")
            } catch (e: Exception) {
                Log.e("TokenService", "Error during logout", e)
            }
        }

        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
