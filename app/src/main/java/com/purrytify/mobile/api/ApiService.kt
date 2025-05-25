package com.purrytify.mobile.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.Date

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String
)

data class RefreshTokenRequest(val refreshToken: String)

data class RefreshTokenResponse(val accessToken: String, val refreshToken: String)

data class EditProfileResponse(
    val message: String
)

data class ProfileResponse(
    val id: Int, // Or String, depending on your API
    val username: String,
    val email: String,
    val profilePhoto: String?, // Make nullable if it can be absent
    val location: String,
    val createdAt: Date, // Or String if you prefer to handle parsing later
    val updatedAt: Date  // Or String
)

interface AuthService {
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/verify-token")
    suspend fun verifyToken(@Header("Authorization") authorization: String): Response<Unit>

    @POST("api/refresh-token")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<RefreshTokenResponse>
}

interface UserService {
    @GET("api/profile")
    suspend fun getProfile(@Header("Authorization") authorization: String): Response<ProfileResponse>

    @Multipart
    @PATCH("api/profile")
    suspend fun editProfile(
        @Header("Authorization") authorization: String,
        @Part("location") location: RequestBody?,
        @Part profilePhoto: MultipartBody.Part?
    ): Response<EditProfileResponse>
}