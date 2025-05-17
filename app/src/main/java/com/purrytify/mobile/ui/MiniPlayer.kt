package com.purrytify.mobile.ui

import android.media.MediaPlayer
import android.net.Uri
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.purrytify.mobile.LocalPoppinsFont
import com.purrytify.mobile.R
import com.purrytify.mobile.data.room.LocalSong
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.core.net.toUri
import java.io.File

object MiniPlayerState {
    var mediaPlayer: MediaPlayer? = null
    var currentSong: LocalSong? by mutableStateOf(null)
    var isPlaying by mutableStateOf(false)
    var currentPosition by mutableStateOf(0)
    var totalDuration by mutableStateOf(0)
}

@Composable
fun MiniPlayer(
    bottomPadding: Int = 0,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(MiniPlayerState.isPlaying) {
        while (isActive && MiniPlayerState.isPlaying) {
            MiniPlayerState.currentPosition = MiniPlayerState.mediaPlayer?.currentPosition ?: 0
            delay(100)
        }
    }

    MiniPlayerState.currentSong?.let { song ->
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(Color(0xFF282828))
                .padding(bottom = bottomPadding.dp)
        ) {
            LinearProgressIndicator(
                progress = if (MiniPlayerState.totalDuration > 0) 
                    MiniPlayerState.currentPosition.toFloat() / MiniPlayerState.totalDuration 
                else 0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = Color(0xFF1DB954),
                trackColor = Color.DarkGray
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.DarkGray, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (song.artworkPath != null) {
                        AndroidView(
                            factory = { ctx ->
                                ImageView(ctx).apply {
                                    scaleType = ImageView.ScaleType.CENTER_CROP
                                }
                            },
                            update = { imageView ->
                                Glide.with(imageView)
                                    .load(song.artworkPath.toUri())
                                    .centerCrop()
                                    .placeholder(R.drawable.placeholder_album)
                                    .into(imageView)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.play_circle),
                            contentDescription = "Music Icon",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = song.title,
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = LocalPoppinsFont.current,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = song.artist,
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontFamily = LocalPoppinsFont.current,
                                color = Color.Gray
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "${formatTimeMs(MiniPlayerState.currentPosition)} / ${formatTimeMs(MiniPlayerState.totalDuration)}",
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontFamily = LocalPoppinsFont.current,
                                color = Color.Gray
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    painter = painterResource(
                        id = if (MiniPlayerState.isPlaying) R.drawable.pause else R.drawable.play_circle
                    ),
                    contentDescription = if (MiniPlayerState.isPlaying) "Pause" else "Play",
                    tint = Color(0xFF1DB954),
                    modifier = Modifier
                        .size(32.dp)
                        .clickable {
                            if (MiniPlayerState.isPlaying) {
                                MiniPlayerState.mediaPlayer?.pause()
                                MiniPlayerState.isPlaying = false
                            } else {
                                MiniPlayerState.mediaPlayer?.start()
                                MiniPlayerState.isPlaying = true
                            }
                        }
                )
            }
        }
    }
}

fun formatTimeMs(timeMs: Int): String {
    if (timeMs <= 0) return "0:00"
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val remainingSeconds = totalSeconds % 60
    return "$minutes:${remainingSeconds.toString().padStart(2, '0')}"
}

fun initializeMediaPlayer(context: android.content.Context) {
    try {
        if (MiniPlayerState.mediaPlayer == null) {
            MiniPlayerState.mediaPlayer = MediaPlayer().apply {
                setOnCompletionListener {
                    MiniPlayerState.isPlaying = false
                    MiniPlayerState.currentPosition = 0
                }
                setOnErrorListener { mp, what, extra ->
                    android.util.Log.e("MiniPlayer", "MediaPlayer error: what=$what extra=$extra")
                    false
                }
            }
            android.util.Log.d("MiniPlayer", "MediaPlayer initialized successfully")
        }
    } catch (e: Exception) {
        android.util.Log.e("MiniPlayer", "Error initializing MediaPlayer: ${e.message}", e)
    }
}

fun playSong(song: LocalSong, context: android.content.Context) {
    try {
        android.util.Log.d("MiniPlayer", "Attempting to play song: ${song.title}")
        android.util.Log.d("MiniPlayer", "File path: ${song.filePath}")
        
        if (MiniPlayerState.mediaPlayer == null) {
            initializeMediaPlayer(context)
        }
        
        if (MiniPlayerState.currentSong?.id == song.id) {
            if (MiniPlayerState.mediaPlayer?.isPlaying == true) {
                MiniPlayerState.mediaPlayer?.pause()
                MiniPlayerState.isPlaying = false
            } else {
                MiniPlayerState.mediaPlayer?.start()
                MiniPlayerState.isPlaying = true
            }
        } else {
            MiniPlayerState.currentSong = song
            MiniPlayerState.mediaPlayer?.apply {
                reset()
                try {
                    // Try using direct file path first
                    if (File(song.filePath).exists()) {
                        setDataSource(song.filePath)
                    } else {
                        // Fallback to using URI if file path doesn't exist
                        setDataSource(context, Uri.parse(song.filePath))
                    }
                    
                    prepareAsync()
                    setOnPreparedListener {
                        MiniPlayerState.totalDuration = duration
                        start()
                        MiniPlayerState.isPlaying = true
                    }
                    setOnErrorListener { mp, what, extra ->
                        android.util.Log.e("MiniPlayer", "MediaPlayer error: what=$what extra=$extra")
                        android.widget.Toast.makeText(
                            context,
                            "Error playing song: Error code $what",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        false
                    }
                } catch (e: Exception) {
                    throw Exception("Could not load audio file: ${e.message}")
                }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("MiniPlayer", "Error playing song: ${e.message}", e)
        android.widget.Toast.makeText(
            context,
            "Error playing song: ${e.message}",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}