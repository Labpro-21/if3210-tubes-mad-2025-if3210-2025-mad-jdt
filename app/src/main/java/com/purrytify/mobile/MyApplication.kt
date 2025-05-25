package com.purrytify.mobile

import android.app.Application
import com.purrytify.mobile.data.room.AppDatabase
import com.purrytify.mobile.data.room.LocalSongRepository
import com.purrytify.mobile.utils.ListeningTracker

class MyApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val localSongRepository by lazy { LocalSongRepository(database.localSongDao()) }

    override fun onCreate() {
        super.onCreate()
        ListeningTracker.initialize(this)
    }
}
