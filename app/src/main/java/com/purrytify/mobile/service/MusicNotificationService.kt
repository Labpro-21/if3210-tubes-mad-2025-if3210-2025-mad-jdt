package com.purrytify.mobile.service

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.purrytify.mobile.MainActivity
import com.purrytify.mobile.R
import com.purrytify.mobile.data.room.CountrySong
import com.purrytify.mobile.data.room.LocalSong
import com.purrytify.mobile.data.room.TopSong
import com.purrytify.mobile.ui.MiniPlayerState
import kotlinx.coroutines.*
import com.purrytify.mobile.utils.ListeningTracker

class MusicNotificationService : Service() {

    companion object {
        const val CHANNEL_ID = "music_player_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "ACTION_NEXT"
        const val ACTION_PREV = "ACTION_PREV"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_SEEK_TO = "ACTION_SEEK_TO"
        const val EXTRA_SEEK_POSITION = "EXTRA_SEEK_POSITION"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var notificationUpdateJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startNotificationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MusicNotificationService", "onStartCommand called with action: ${intent?.action}")
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> togglePlayPause()
            ACTION_NEXT -> playNext()
            ACTION_PREV -> playPrev()
            ACTION_STOP -> stopPlayback()
            ACTION_SEEK_TO -> {
                val position = intent.getIntExtra(EXTRA_SEEK_POSITION, 0)
                seekTo(position)
            }
            else -> showNotification()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startNotificationUpdates() {
        notificationUpdateJob = serviceScope.launch {
            while (isActive) {
                if (MiniPlayerState.isPlaying && MiniPlayerState.mediaPlayer != null) {
                    MiniPlayerState.currentPosition = MiniPlayerState.mediaPlayer?.currentPosition ?: 0
                    showNotification()
                }
                delay(1000)
            }
        }
    }

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
                    val localSong =
                        when (song) {
                            is LocalSong -> song
                            is TopSong -> song.toLocalSong()
                            is CountrySong -> song.toLocalSong()
                            else -> return@let
                        }
                    ListeningTracker.resumeListening(localSong)
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

    private fun stopPlayback() {
        MiniPlayerState.mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.reset()
        }
        MiniPlayerState.isPlaying = false
        MiniPlayerState.currentSong = null
        MiniPlayerState.currentPosition = 0
        MiniPlayerState.totalDuration = 0

        ListeningTracker.stopListening()
        stopForeground(true)
        stopSelf()
    }

    private fun seekTo(position: Int) {
        MiniPlayerState.mediaPlayer?.let { player ->
            if (position >= 0 && position <= player.duration) {
                player.seekTo(position)
                MiniPlayerState.currentPosition = position
            }
        }
        showNotification()
    }

    private fun showNotification() {
        Log.d("MusicNotificationService", "showNotification called")
        val song = MiniPlayerState.currentSong ?: return
        val isPlaying = MiniPlayerState.isPlaying
        val durationMs = MiniPlayerState.totalDuration
        val currentPositionMs = MiniPlayerState.currentPosition

        val playPauseIcon = if (isPlaying) R.drawable.pause else R.drawable.play_circle
        val playPauseLabel = if (isPlaying) "Pause" else "Play"

        val openPlayerIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("open_player", true)
            when (song) {
                is TopSong -> {
                    putExtra("song_type", "top")
                    putExtra("song_id", song.id)
                }
                is CountrySong -> {
                    putExtra("song_type", "country")
                    putExtra("song_id", song.id)
                }
            }
        }
        val openPlayerPendingIntent = PendingIntent.getActivity(
            this, 0, openPlayerIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = PendingIntent.getService(
            this, 1, Intent(this, javaClass).setAction(ACTION_PLAY_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nextIntent = PendingIntent.getService(
            this, 2, Intent(this, javaClass).setAction(ACTION_NEXT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val prevIntent = PendingIntent.getService(
            this, 3, Intent(this, javaClass).setAction(ACTION_PREV),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 4, Intent(this, javaClass).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (title, artist, artworkPath) = when (song) {
            is LocalSong -> Triple(song.title, song.artist, song.artworkPath)
            is TopSong -> Triple(song.title, song.artist, song.artwork)
            is CountrySong -> Triple(song.title, song.artist, song.artwork)
            else -> Triple("No Song", "Unknown Artist", null)
        }

        fun formatTime(ms: Int): String {
            val totalSec = ms / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            return "%d:%02d".format(min, sec)
        }

        val progressText = "${formatTime(currentPositionMs)} / ${formatTime(durationMs)}"
        val progressPercentage = if (durationMs > 0) (currentPositionMs * 100 / durationMs) else 0

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(R.drawable.splash_logo)
            .setContentIntent(openPlayerPendingIntent)
            .addAction(R.drawable.skip_previous, "Previous", prevIntent)
            .addAction(playPauseIcon, playPauseLabel, playPauseIntent)
            .addAction(R.drawable.skip_next, "Next", nextIntent)
            .addAction(R.drawable.ic_close, "Stop", stopIntent)
            .setDeleteIntent(stopIntent)
            .setStyle(
                MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            .setSubText(progressText)
            .setProgress(100, progressPercentage, false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setColor(0xFF1DB954.toInt())
            .setPriority(NotificationCompat.PRIORITY_LOW)

        loadArtworkAndUpdateNotification(artworkPath, builder)
    }

    private fun loadArtworkAndUpdateNotification(
        artworkPath: String?,
        builder: NotificationCompat.Builder
    ) {
        if (artworkPath != null) {
            Glide.with(this)
                .asBitmap()
                .load(artworkPath)
                .placeholder(R.drawable.placeholder_album)
                .error(R.drawable.placeholder_album)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        builder.setLargeIcon(resource)
                        startForegroundNotification(builder)
                    }
                    override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                        val defaultIcon = BitmapFactory.decodeResource(resources, R.drawable.placeholder_album)
                        builder.setLargeIcon(defaultIcon)
                        startForegroundNotification(builder)
                    }
                })
        } else {
            val defaultIcon = BitmapFactory.decodeResource(resources, R.drawable.placeholder_album)
            builder.setLargeIcon(defaultIcon)
            startForegroundNotification(builder)
        }
    }

    private fun startForegroundNotification(builder: NotificationCompat.Builder) {
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
                CHANNEL_ID,
                "Purrytify Music Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music player controls and information"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        notificationUpdateJob?.cancel()
        serviceScope.cancel()
        stopForeground(true)
        MiniPlayerState.mediaPlayer?.stop()
        MiniPlayerState.isPlaying = false
        ListeningTracker.stopListening()
        super.onDestroy()
    }
}