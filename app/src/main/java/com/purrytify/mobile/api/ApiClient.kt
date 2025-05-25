package com.purrytify.mobile.api

import com.purrytify.mobile.data.TokenManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "http://34.101.226.132:3000"
    private var authInterceptor: AuthInterceptor? = null

    fun buildRetrofit(tokenManager: TokenManager? = null, onLogoutRequired: (() -> Unit)? = null): Retrofit {
        val httpClientBuilder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        // Add auth interceptor if tokenManager is provided
        if (tokenManager != null) {
            // If logout callback is provided, create a new interceptor instance
            // Otherwise, use cached interceptor for backward compatibility
            val interceptor = if (onLogoutRequired != null) {
                // Create a basic Retrofit instance first to get AuthService
                val basicRetrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(OkHttpClient.Builder().build())
                    .build()

                val authService = basicRetrofit.create(AuthService::class.java)
                AuthInterceptor(tokenManager, authService, onLogoutRequired)
            } else {
                if (authInterceptor == null) {
                    // Create a basic Retrofit instance first to get AuthService
                    val basicRetrofit = Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .client(OkHttpClient.Builder().build())
                        .build()

                    val authService = basicRetrofit.create(AuthService::class.java)
                    authInterceptor = AuthInterceptor(tokenManager, authService)
                }
                authInterceptor!!
            }
            httpClientBuilder.addInterceptor(interceptor)
        }

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClientBuilder.build())
            .build()
    }

    fun createAuthService(retrofit: Retrofit): AuthService {
        return retrofit.create(AuthService::class.java)
    }

    fun createUserService(retrofit: Retrofit): UserService {
        return retrofit.create(UserService::class.java)
    }
}
