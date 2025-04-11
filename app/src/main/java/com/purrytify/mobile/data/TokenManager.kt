package com.purrytify.mobile.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore("auth_prefs")

class TokenManager(private val context: Context) {
    companion object {
        val ACCESS_TOKEN_KEY = stringPreferencesKey("ACCESS_TOKEN")
        val REFRESH_TOKEN_KEY = stringPreferencesKey("REFRESH_TOKEN")
    }

    suspend fun saveTokens(access: String, refresh: String) {
        context.dataStore.edit {
            it[ACCESS_TOKEN_KEY] = access
            it[REFRESH_TOKEN_KEY] = refresh
        }
    }

    val accessToken: Flow<String?> = context.dataStore.data
        .map { it[ACCESS_TOKEN_KEY] }

    val refreshToken: Flow<String?> = context.dataStore.data
        .map { it[REFRESH_TOKEN_KEY] }

    suspend fun clearTokens() {
        context.dataStore.edit {
            it.remove(ACCESS_TOKEN_KEY)
            it.remove(REFRESH_TOKEN_KEY)
        }
    }

    fun getAccessToken(): String? {
        return runBlocking {
            context.dataStore.data.map { preferences ->
                preferences[ACCESS_TOKEN_KEY]
            }.first()
        }
    }

    fun getRefreshToken(): String? {
        return runBlocking {
            context.dataStore.data.map { preferences ->
                preferences[REFRESH_TOKEN_KEY]
            }.first()
        }
    }
}
