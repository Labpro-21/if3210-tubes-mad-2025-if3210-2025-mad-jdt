package com.purrytify.mobile

import android.app.Application
import com.purrytify.mobile.data.room.AppDatabase
import com.purrytify.mobile.data.room.ListeningSessionRepository
import com.purrytify.mobile.data.room.LocalSongRepository

class PurrytifyApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val localSongRepository by lazy { LocalSongRepository(database.localSongDao()) }
    val listeningSessionRepository by lazy {
        ListeningSessionRepository(database.listeningSessionDao())
    }
}
