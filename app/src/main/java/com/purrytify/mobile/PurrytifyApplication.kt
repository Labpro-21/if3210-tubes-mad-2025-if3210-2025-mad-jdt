package com.purrytify.mobile

import android.app.Application
import com.purrytify.mobile.data.room.AppDatabase
import com.purrytify.mobile.data.room.LocalSongRepository
import com.purrytify.mobile.data.room.ListeningSessionRepository

class PurrytifyApplication : Application() {
    // Database instance
    val database by lazy { AppDatabase.getDatabase(this) }
    
    // Repository instances
    val localSongRepository by lazy { LocalSongRepository(database.localSongDao()) }
    val listeningSessionRepository by lazy { ListeningSessionRepository(database.listeningSessionDao()) }
}