package com.purrytify.mobile.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [LocalSong::class, ListeningSession::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun localSongDao(): LocalSongDao
    abstract fun listeningSessionDao(): ListeningSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `listening_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `songId` INTEGER NOT NULL,
                        `songTitle` TEXT NOT NULL,
                        `artist` TEXT NOT NULL,
                        `startTime` INTEGER NOT NULL,
                        `endTime` INTEGER NOT NULL,
                        `duration` INTEGER NOT NULL,
                        `month` INTEGER NOT NULL,
                        `year` INTEGER NOT NULL
                    )
                """)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "purrytify_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}