package com.purrytify.mobile.api

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val access: String,
    val refresh: String
)

data class RefreshTokenRequest(val refreshToken: String)

data class RefreshTokenResponse(val access: String, val refresh: String)

interface AuthService {
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/verify-token")
    suspend fun verifyToken(@Header("Authorization") authorization: String): Response<Unit>

    @POST("api/refresh-token")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<RefreshTokenResponse>
}
