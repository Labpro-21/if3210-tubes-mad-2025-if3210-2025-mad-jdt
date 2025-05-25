package com.purrytify.mobile.service

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.purrytify.mobile.R
import com.purrytify.mobile.data.room.LocalSong
import com.purrytify.mobile.ui.MiniPlayerState
import com.purrytify.mobile.utils.ListeningTracker

class MusicNotificationService : Service() {

    companion object {
        const val CHANNEL_ID = "music_player_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "ACTION_NEXT"
        const val ACTION_PREV = "ACTION_PREV"
        const val ACTION_STOP = "ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MusicNotificationService", "onStartCommand called with action: ${intent?.action}")
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> togglePlayPause()
            ACTION_NEXT -> playNext()
            ACTION_PREV -> playPrev()
            ACTION_STOP -> stopSelf()
            else -> showNotification()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun togglePlayPause() {
        MiniPlayerState.mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                MiniPlayerState.isPlaying = false
                ListeningTracker.pauseListening()
            } else {
                player.start()
                MiniPlayerState.isPlaying = true
                MiniPlayerState.currentSong?.let { song ->
                    ListeningTracker.resumeListening(song)
                }
            }
        }
        showNotification()
    }

    private fun playNext() {
        // belum implement queue
        showNotification()
    }

    private fun playPrev() {
        // belum implement queue
        showNotification()
    }

    private fun showNotification() {
        Log.d("MusicNotificationService", "showNotification called")
        val song = MiniPlayerState.currentSong
        val isPlaying = MiniPlayerState.isPlaying
        val durationMs = MiniPlayerState.totalDuration
        val currentPositionMs = MiniPlayerState.mediaPlayer?.currentPosition ?: 0

        val playPauseIcon = if (isPlaying) R.drawable.pause else R.drawable.play_circle
        val playPauseLabel = if (isPlaying) "Pause" else "Play"

        val playPauseIntent = PendingIntent.getService(
            this, 0, Intent(this, javaClass).setAction(ACTION_PLAY_PAUSE), PendingIntent.FLAG_IMMUTABLE
        )
        val nextIntent = PendingIntent.getService(
            this, 1, Intent(this, javaClass).setAction(ACTION_NEXT), PendingIntent.FLAG_IMMUTABLE
        )
        val prevIntent = PendingIntent.getService(
            this, 2, Intent(this, javaClass).setAction(ACTION_PREV), PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 3, Intent(this, javaClass).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE
        )

        val openPlayerIntent = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 4, it, PendingIntent.FLAG_IMMUTABLE)
        }

        val largeIcon = song?.artworkPath?.let {
            try {
                BitmapFactory.decodeFile(it)
            } catch (e: Exception) {
                null
            }
        } ?: BitmapFactory.decodeResource(resources, R.drawable.placeholder_album)

        fun formatTime(ms: Int): String {
            val totalSec = ms / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            return "%d:%02d".format(min, sec)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(song?.title ?: "No Song")
            .setContentText(song?.artist ?: "Unknown Artist")
            .setSmallIcon(R.drawable.splash_logo)
            .setLargeIcon(largeIcon)
            .setContentIntent(openPlayerIntent)
            .addAction(R.drawable.skip_previous, "Prev", prevIntent)
            .addAction(playPauseIcon, playPauseLabel, playPauseIntent)
            .addAction(R.drawable.skip_next, "Next", nextIntent)
            .addAction(R.drawable.ic_close, "Stop", stopIntent)
            .setDeleteIntent(stopIntent)
            .setStyle(
                MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2, 3)
            )
            .setSubText("${formatTime(currentPositionMs)} / ${formatTime(durationMs)}")
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setColor(0xFF181818.toInt())

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                builder.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, builder.build())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Music Player", NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Purrytify Music Player Controls"
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopForeground(true)
        MiniPlayerState.mediaPlayer?.stop()
        MiniPlayerState.isPlaying = false
        ListeningTracker.stopListening()
        super.onDestroy()
    }
}