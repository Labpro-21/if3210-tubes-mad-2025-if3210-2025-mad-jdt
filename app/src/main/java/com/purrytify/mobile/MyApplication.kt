package com.purrytify.mobile

import android.app.Application
import com.purrytify.mobile.data.room.AppDatabase
import com.purrytify.mobile.data.room.LocalSongRepository
import com.purrytify.mobile.utils.ListeningTracker

class MyApplication : Application() {
    // Database instance
    val database by lazy { AppDatabase.getDatabase(this) }
    
    // Repository instance
    val localSongRepository by lazy { LocalSongRepository(database.localSongDao()) }
    
    override fun onCreate() {
        super.onCreate()
        // Initialize ListeningTracker
        ListeningTracker.initialize(this)
    }
}
