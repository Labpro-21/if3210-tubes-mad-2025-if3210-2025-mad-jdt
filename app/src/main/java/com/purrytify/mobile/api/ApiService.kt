package com.purrytify.mobile.api

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Response

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val access: String,
    val refresh: String
)

interface AuthService {
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}
