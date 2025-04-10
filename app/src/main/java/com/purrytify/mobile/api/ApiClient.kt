package com.purrytify.mobile.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // The BASE_URL is now handled in the Hilt module
    //private const val BASE_URL = "http://34.101.226.132:3000"

    // This is no longer used directly
    //val authService: AuthService by lazy {
    //    Retrofit.Builder()
    //        .baseUrl(BASE_URL)
    //        .addConverterFactory(GsonConverterFactory.create())
    //        .build()
    //        .create(AuthService::class.java)
    //}
}
