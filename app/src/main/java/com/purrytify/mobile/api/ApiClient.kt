package com.purrytify.mobile.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    fun buildRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://34.101.226.132:3000")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun createAuthService(retrofit: Retrofit): AuthService {
        return retrofit.create(AuthService::class.java)
    }
}
