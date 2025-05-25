package com.purrytify.mobile.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.purrytify.mobile.LocalPoppinsFont
import com.purrytify.mobile.R
import com.purrytify.mobile.data.room.CountrySong
import com.purrytify.mobile.data.room.LocalSong
import com.purrytify.mobile.data.room.TopSong
import com.purrytify.mobile.service.MusicNotificationService
import com.purrytify.mobile.utils.ListeningTracker
import com.purrytify.mobile.utils.generateQrCode
import com.purrytify.mobile.utils.getAvailableOutputDevices
import com.purrytify.mobile.utils.setAudioOutput
import com.purrytify.mobile.utils.shareSong
import com.purrytify.mobile.viewmodel.LocalSongViewModel
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object MiniPlayerState {
    var mediaPlayer: MediaPlayer? = null
    var currentSong: Any? by mutableStateOf(null)
    var isPlaying by mutableStateOf(false)
    var currentPosition by mutableStateOf(0)
    var totalDuration by mutableStateOf(0)
    var isExpanded by mutableStateOf(false)
    var currentUrl: String? by mutableStateOf(null)
    var fromNotification: Boolean = false
    var isUserSeeking: Boolean = false
    var currentQueue: List<Any> = emptyList()
    var currentIndex: Int = -1
    var queueType: String = ""
    
    fun setQueue(songs: List<Any>, startIndex: Int = 0, type: String) {
        currentQueue = songs
        currentIndex = startIndex.coerceIn(0, songs.size - 1)
        queueType = type
        if (songs.isNotEmpty() && currentIndex >= 0) {
            currentSong = songs[currentIndex]
        }
        Log.d("MiniPlayerState", "Queue set: type=$type, size=${songs.size}, index=$currentIndex")
    }
    
    fun getCurrentSongId(): Int? {
        return when (val song = currentSong) {
            is LocalSong -> song.id.toInt()
            is TopSong -> song.id
            is CountrySong -> song.id
            else -> null
        }
    }
    
    fun getNextSong(): Any? {
        return if (currentIndex + 1 < currentQueue.size) {
            val nextSong = currentQueue[currentIndex + 1]
            Log.d("MiniPlayerState", "Next song found: ${getSongTitle(nextSong)} (queue: $queueType)")
            nextSong
        } else {
            Log.d("MiniPlayerState", "No next song in queue (queue: $queueType)")
            null
        }
    }
    
    fun getPrevSong(): Any? {
        return if (currentIndex > 0) {
            val prevSong = currentQueue[currentIndex - 1]
            Log.d("MiniPlayerState", "Prev song found: ${getSongTitle(prevSong)} (queue: $queueType)")
            prevSong
        } else {
            Log.d("MiniPlayerState", "No prev song in queue (queue: $queueType)")
            null
        }
    }
    
    fun moveToNext(): Boolean {
        val next = getNextSong()
        return if (next != null) {
            currentIndex++
            currentSong = next
            Log.d("MiniPlayerState", "Moved to next: index=$currentIndex, song=${getSongTitle(next)}")
            true
        } else false
    }
    
    fun moveToPrev(): Boolean {
        val prev = getPrevSong()
        return if (prev != null) {
            currentIndex--
            currentSong = prev
            Log.d("MiniPlayerState", "Moved to prev: index=$currentIndex, song=${getSongTitle(prev)}")
            true
        } else false
    }
    
    // Helper function
    private fun getSongTitle(song: Any?): String {
        return when (song) {
            is LocalSong -> song.title
            is TopSong -> song.title
            is CountrySong -> song.title
            else -> "Unknown"
        }
    }
}

@Composable
fun MiniPlayer(
        bottomPadding: Int = 0,
        modifier: Modifier = Modifier,
        onDeleteClick: (LocalSong) -> Unit,
        viewModel: LocalSongViewModel
) {
    val context = LocalContext.current

    LaunchedEffect(MiniPlayerState.isPlaying) {
        while (isActive && MiniPlayerState.isPlaying) {
            MiniPlayerState.currentPosition = MiniPlayerState.mediaPlayer?.currentPosition ?: 0
            delay(100)
        }
    }

    AnimatedVisibility(
            visible = MiniPlayerState.isExpanded,
            enter =
                    slideInVertically(
                            initialOffsetY = { fullHeight -> fullHeight },
                            animationSpec =
                                    tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    ),
            exit =
                    slideOutVertically(
                            targetOffsetY = { fullHeight -> fullHeight },
                            animationSpec =
                                    tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    )
    ) {
        ExpandedPlayer(
                onDismiss = { MiniPlayerState.isExpanded = false },
                onDeleteClick = onDeleteClick,
                viewModel = viewModel
        )
    }

    MiniPlayerState.currentSong?.let { song ->
        val title =
                when (song) {
                    is LocalSong -> song.title
                    is TopSong -> song.title
                    is CountrySong -> song.title
                    else -> ""
                }
        val artist =
                when (song) {
                    is LocalSong -> song.artist
                    is TopSong -> song.artist
                    is CountrySong -> song.artist
                    else -> ""
                }
        val artworkPath =
                when (song) {
                    is LocalSong -> song.artworkPath
                    is TopSong -> song.artwork
                    is CountrySong -> song.artwork
                    else -> null
                }

        Column(
                modifier =
                        modifier.fillMaxWidth()
                                .background(Color(0xFF282828))
                                .clickable { MiniPlayerState.isExpanded = true }
                                .padding(bottom = bottomPadding.dp)
        ) {
            LinearProgressIndicator(
                    progress =
                            if (MiniPlayerState.totalDuration > 0)
                                    MiniPlayerState.currentPosition.toFloat() /
                                            MiniPlayerState.totalDuration
                            else 0f,
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = Color(0xFF1DB954),
                    trackColor = Color.DarkGray
            )

            Row(
                    modifier = Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                        modifier =
                                Modifier.size(40.dp)
                                        .background(Color.DarkGray, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                ) {
                    if (artworkPath != null) {
                        AndroidView(
                                factory = { ctx ->
                                    ImageView(ctx).apply {
                                        scaleType = ImageView.ScaleType.CENTER_CROP
                                    }
                                },
                                update = { imageView ->
                                    Glide.with(imageView)
                                            .load(artworkPath.toUri())
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

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                            text = title,
                            style =
                                    TextStyle(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = LocalPoppinsFont.current,
                                            color = Color.White
                                    ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                                text = artist,
                                style =
                                        TextStyle(
                                                fontSize = 12.sp,
                                                fontFamily = LocalPoppinsFont.current,
                                                color = Color.Gray
                                        ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                        )

                        Text(
                                text =
                                        "${formatTimeMs(MiniPlayerState.currentPosition)} / ${formatTimeMs(MiniPlayerState.totalDuration)}",
                                style =
                                        TextStyle(
                                                fontSize = 12.sp,
                                                fontFamily = LocalPoppinsFont.current,
                                                color = Color.Gray
                                        )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (MiniPlayerState.currentSong is TopSong ||
                                MiniPlayerState.currentSong is CountrySong
                ) {
                    val onlineSong = MiniPlayerState.currentSong
                    val songId =
                            when (onlineSong) {
                                is TopSong -> onlineSong.id
                                is CountrySong -> onlineSong.id
                                else -> return
                            }
                    IconButton(onClick = { shareSong(context, songId) }) {
                        Icon(
                                Icons.Default.Share,
                                contentDescription = "share song",
                                tint = Color.White,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                        painter =
                                painterResource(
                                        id =
                                                if (MiniPlayerState.isPlaying) R.drawable.pause
                                                else R.drawable.play_circle
                                ),
                        contentDescription = if (MiniPlayerState.isPlaying) "Pause" else "Play",
                        tint = Color(0xFF1DB954),
                        modifier =
                                Modifier.size(32.dp).clickable {
                                    if (MiniPlayerState.isPlaying) {
                                        MiniPlayerState.mediaPlayer?.pause()
                                        MiniPlayerState.isPlaying = false
                                        ListeningTracker.pauseListening()
                                    } else {
                                        MiniPlayerState.mediaPlayer?.start()
                                        MiniPlayerState.isPlaying = true
                                        MiniPlayerState.currentSong?.let { song ->
                                            // Convert to LocalSong for ListeningTracker
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
                )
            }
        }
    }
}

fun playNext(context: Context) {
    Log.d("MiniPlayer", "playNext called - Current queue type: ${MiniPlayerState.queueType}")
    Log.d("MiniPlayer", "Current queue size: ${MiniPlayerState.currentQueue.size}")
    Log.d("MiniPlayer", "Current index: ${MiniPlayerState.currentIndex}")
    
    if (MiniPlayerState.moveToNext()) {
        val nextSong = MiniPlayerState.currentSong
        when (nextSong) {
            is LocalSong -> playSong(nextSong, context)
            is TopSong -> playSong(nextSong, context)
            is CountrySong -> playSong(nextSong, context)
        }
        Log.d("MiniPlayer", "Playing next song: ${getSongTitle(nextSong)} from ${MiniPlayerState.queueType} queue")
    } else {
        MiniPlayerState.mediaPlayer?.let { player ->
            val duration = player.duration
            if (duration > 0) {
                player.seekTo(duration)
                MiniPlayerState.currentPosition = duration
                Log.d("MiniPlayer", "No next song, seeking to end")
                
                val serviceIntent = Intent(context, MusicNotificationService::class.java)
                context.startForegroundService(serviceIntent)
            }
        }
    }
}

fun playPrev(context: Context) {
    Log.d("MiniPlayer", "playPrev called - Current queue type: ${MiniPlayerState.queueType}")
    
    if (MiniPlayerState.moveToPrev()) {
        val prevSong = MiniPlayerState.currentSong
        when (prevSong) {
            is LocalSong -> playSong(prevSong, context)
            is TopSong -> playSong(prevSong, context)
            is CountrySong -> playSong(prevSong, context)
        }
        Log.d("MiniPlayer", "Playing prev song: ${getSongTitle(prevSong)} from ${MiniPlayerState.queueType} queue")
    } else {
        MiniPlayerState.mediaPlayer?.let { player ->
            player.seekTo(0)
            MiniPlayerState.currentPosition = 0
            Log.d("MiniPlayer", "No previous song, seeking to start")
            
            val serviceIntent = Intent(context, MusicNotificationService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}

private fun getSongTitle(song: Any?): String {
    return when (song) {
        is LocalSong -> song.title
        is TopSong -> song.title
        is CountrySong -> song.title
        else -> "Unknown"
    }
}

fun formatTimeMs(timeMs: Int): String {
    if (timeMs <= 0) return "0:00"
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val remainingSeconds = totalSeconds % 60
    return "$minutes:${remainingSeconds.toString().padStart(2, '0')}"
}

private fun seekToPosition(position: Float) {
    MiniPlayerState.mediaPlayer?.let { player ->
        val newPosition = (position * MiniPlayerState.totalDuration).toInt()
        player.seekTo(newPosition)
        MiniPlayerState.currentPosition = newPosition
    }
}

fun initializeMediaPlayer(context: android.content.Context) {
    try {
        if (MiniPlayerState.mediaPlayer == null) {
            MiniPlayerState.mediaPlayer =
                    MediaPlayer().apply {
                        setOnCompletionListener {
                            MiniPlayerState.isPlaying = false
                            MiniPlayerState.currentPosition = 0
                            ListeningTracker.stopListening()
                        }
                        val stopIntent =
                                Intent(context, MusicNotificationService::class.java).apply {
                                    action = MusicNotificationService.ACTION_STOP
                                }
                        context.startService(stopIntent)
                        setOnErrorListener { mp, what, extra ->
                            android.util.Log.e(
                                    "MiniPlayer",
                                    "MediaPlayer error: what=$what extra=$extra"
                            )
                            ListeningTracker.stopListening()
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
        android.util.Log.d("MiniPlayer", "Is downloaded: ${song.isDownloaded}")

        // Initialize ListeningTracker
        ListeningTracker.initialize(context)

        // If switching songs, stop tracking the previous song
        val currentSongId =
                when (val currentSong = MiniPlayerState.currentSong) {
                    is LocalSong -> currentSong.id
                    is TopSong -> currentSong.id
                    is CountrySong -> currentSong.id
                    else -> null
                }

        if (currentSongId != null && currentSongId != song.id) {
            ListeningTracker.switchSong(song)
        } else {
            ListeningTracker.startListening(song)
        }

        if (MiniPlayerState.mediaPlayer == null) {
            initializeMediaPlayer(context)
        }

        MiniPlayerState.mediaPlayer?.apply {
            reset()
            try {
                when {
                    // For downloaded songs (internal storage files)
                    song.isDownloaded && song.filePath.startsWith("/") -> {
                        val file = File(song.filePath)
                        android.util.Log.d(
                                "MiniPlayer",
                                "Playing downloaded file: ${file.absolutePath}"
                        )
                        android.util.Log.d("MiniPlayer", "File exists: ${file.exists()}")

                        if (file.exists()) {
                            // For internal storage files, use direct file path
                            setDataSource(file.absolutePath)
                        } else {
                            throw Exception("Downloaded file not found: ${file.absolutePath}")
                        }
                    }
                    // For content URIs (user-added songs)
                    song.filePath.startsWith("content://") -> {
                        android.util.Log.d("MiniPlayer", "Playing content URI: ${song.filePath}")
                        setDataSource(context, Uri.parse(song.filePath))
                    }
                    // For regular file paths (user-added songs)
                    song.filePath.startsWith("/") -> {
                        val file = File(song.filePath)
                        android.util.Log.d("MiniPlayer", "Playing file path: ${file.absolutePath}")

                        if (file.exists()) {
                            // Try direct file access first
                            setDataSource(file.absolutePath)
                        } else {
                            throw Exception("File not found: ${file.absolutePath}")
                        }
                    }
                    // For URLs (streaming)
                    song.filePath.startsWith("http") -> {
                        android.util.Log.d("MiniPlayer", "Playing URL: ${song.filePath}")
                        setDataSource(song.filePath)
                    }
                    else -> {
                        // Fallback: try parsing as URI
                        android.util.Log.d("MiniPlayer", "Trying to parse as URI: ${song.filePath}")
                        setDataSource(context, Uri.parse(song.filePath))
                    }
                }

                MiniPlayerState.currentSong = song
                MiniPlayerState.currentUrl = song.filePath
                MiniPlayerState.isPlaying = true

                // Start the notification service
                val intent = Intent(context, MusicNotificationService::class.java)
                context.startForegroundService(intent)

                setOnPreparedListener { mp ->
                    android.util.Log.d("MiniPlayer", "MediaPlayer prepared successfully")
                    MiniPlayerState.totalDuration = mp.duration
                    mp.start()
                    MiniPlayerState.isPlaying = true
                }

                setOnErrorListener { mp, what, extra ->
                    android.util.Log.e("MiniPlayer", "MediaPlayer error: what=$what extra=$extra")
                    ListeningTracker.stopListening()
                    false
                }

                prepareAsync()
            } catch (e: Exception) {
                android.util.Log.e("MiniPlayer", "Error setting data source: ${e.message}", e)
                throw Exception("Could not access audio file: ${e.message}")
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("MiniPlayer", "Error playing song", e)
        ListeningTracker.stopListening()
        android.widget.Toast.makeText(
                        context,
                        "Error playing song: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                )
                .show()
    }
}

fun playSong(song: TopSong, context: android.content.Context) {
    Log.d("MiniPlayer", "playSong TopSong: ${song.title}")
    MiniPlayerState.currentSong = song
    playSong(song.toLocalSong(), context)
    MiniPlayerState.currentSong = song
}

fun playSong(song: CountrySong, context: android.content.Context) {
    Log.d("MiniPlayer", "playSong CountrySong: ${song.title}")
    MiniPlayerState.currentSong = song
    playSong(song.toLocalSong(), context)
    MiniPlayerState.currentSong = song
}

@Composable
fun ShareQrDialog(
        songId: Int,
        title: String,
        artist: String,
        onDismiss: () -> Unit,
        context: android.content.Context
) {
    val qrBitmap = remember(songId) { generateQrCode("purrytify://song/$songId") }
    Dialog(onDismissRequest = onDismiss) {
        Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                        Modifier.background(Color.White, RoundedCornerShape(16.dp)).padding(24.dp)
        ) {
            Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier.size(220.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(title, color = Color.Black, fontWeight = FontWeight.Bold)
            Text(artist, color = Color.Gray, fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))
            Button(
                    onClick = {
                        // Share QR as image
                        val file = File(context.cacheDir, "qr_${songId}.png")
                        FileOutputStream(file).use { out ->
                            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        val uri =
                                FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        file
                                )
                        val shareIntent =
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                        context.startActivity(Intent.createChooser(shareIntent, "Share QR via"))
                    }
            ) { Text("Share QR") }
        }
    }
}

@Composable
fun ExpandedPlayer(
        onDismiss: () -> Unit,
        onDeleteClick: (LocalSong) -> Unit,
        viewModel: LocalSongViewModel
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showOptions by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showAudioSheet by remember { mutableStateOf(false) }
    var activeDeviceId by remember { mutableStateOf<Int?>(null) }
    var showQrDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        val song = MiniPlayerState.currentSong
        if (song is LocalSong) {
            AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Delete Song", color = Color.White) },
                    text = {
                        Text(
                                "Are you sure you want to delete \"${song.title}\"?",
                                color = Color.White
                        )
                    },
                    confirmButton = {
                        Button(
                                onClick = {
                                    onDeleteClick(song)
                                    MiniPlayerState.isExpanded = false
                                    showDeleteDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) { Text("Delete") }
                    },
                    dismissButton = {
                        Button(
                                onClick = { showDeleteDialog = false },
                                colors =
                                        ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) { Text("Cancel") }
                    },
                    containerColor = Color(0xFF282828),
                    titleContentColor = Color.White,
                    textContentColor = Color.White
            )
        } else {
            showDeleteDialog = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF282828))) {
        val offsetY = remember { mutableStateOf(0f) }
        val scope = rememberCoroutineScope()
        var showOptions by remember { mutableStateOf(false) }

        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 32.dp)
                                .offset(y = offsetY.value.dp)
                                .draggable(
                                        orientation = Orientation.Vertical,
                                        state =
                                                rememberDraggableState { delta ->
                                                    val newOffset = offsetY.value + delta
                                                    if (newOffset >= 0
                                                    ) { // Only allow dragging down
                                                        offsetY.value = newOffset
                                                    }
                                                },
                                        onDragStopped = { velocity ->
                                            if (offsetY.value > 300 || velocity > 300f) {
                                                onDismiss()
                                            } else {
                                                scope.launch {
                                                    animate(
                                                            initialValue = offsetY.value,
                                                            targetValue = 0f,
                                                            animationSpec =
                                                                    spring(
                                                                            dampingRatio =
                                                                                    Spring.DampingRatioMediumBouncy,
                                                                            stiffness =
                                                                                    Spring.StiffnessLow
                                                                    )
                                                    ) { value, _ -> offsetY.value = value }
                                                }
                                            }
                                        }
                                )
        ) {
            Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_down),
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                    )
                }

                Box {
                    if (MiniPlayerState.currentSong is LocalSong) {
                        IconButton(onClick = { showOptions = true }) {
                            Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "More Options",
                                    tint = Color.Gray
                            )
                        }

                        DropdownMenu(
                                expanded = showOptions,
                                onDismissRequest = { showOptions = false },
                                modifier = Modifier.background(Color(0xFF282828))
                        ) {
                            DropdownMenuItem(
                                    text = { Text("Edit", color = Color.White) },
                                    leadingIcon = {
                                        Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = Color.White
                                        )
                                    },
                                    onClick = { showOptions = false }
                            )

                            DropdownMenuItem(
                                    text = { Text("Delete", color = Color.Red) },
                                    leadingIcon = {
                                        Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color.Red
                                        )
                                    },
                                    onClick = {
                                        showOptions = false
                                        showDeleteDialog = true
                                    }
                            )
                        }
                    }
                }
            }

            MiniPlayerState.currentSong?.let { song ->
                val title =
                        when (song) {
                            is LocalSong -> song.title
                            is TopSong -> song.title
                            is CountrySong -> song.title
                            else -> ""
                        }
                val artist =
                        when (song) {
                            is LocalSong -> song.artist
                            is TopSong -> song.artist
                            is CountrySong -> song.artist
                            else -> ""
                        }
                val artworkPath =
                        when (song) {
                            is LocalSong -> song.artworkPath
                            is TopSong -> song.artwork
                            is CountrySong -> song.artwork
                            else -> null
                        }
                val isLiked = (song as? LocalSong)?.isLiked == true

                // Album artwork
                Box(modifier = Modifier.fillMaxWidth().wrapContentSize(Alignment.Center)) {
                    Box(
                            modifier =
                                    Modifier.size(320.dp)
                                            .background(Color.DarkGray, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                    ) {
                        if (artworkPath != null) {
                            AndroidView(
                                    factory = { ctx ->
                                        ImageView(ctx).apply {
                                            scaleType = ImageView.ScaleType.CENTER_CROP
                                        }
                                    },
                                    update = { imageView ->
                                        Glide.with(imageView)
                                                .load(Uri.parse(artworkPath))
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
                                    modifier = Modifier.size(100.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Song info
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                        Text(
                                text = title,
                                style =
                                        TextStyle(
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = LocalPoppinsFont.current,
                                                color = Color.White
                                        ),
                                textAlign = TextAlign.Start
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                                text = artist,
                                style =
                                        TextStyle(
                                                fontSize = 20.sp,
                                                fontFamily = LocalPoppinsFont.current,
                                                color = Color.Gray
                                        ),
                                textAlign = TextAlign.Start
                        )
                    }

                    // Like button
                    if (song is LocalSong) {
                        IconButton(
                                onClick = { viewModel.toggleLikeStatus(song) },
                                modifier = Modifier.size(48.dp)
                        ) {
                            val isLiked = song.isLiked
                            Icon(
                                    painter =
                                            painterResource(
                                                    id =
                                                            if (isLiked) R.drawable.ic_heart_filled
                                                            else R.drawable.ic_heart_outline
                                            ),
                                    contentDescription = if (isLiked) "Unlike" else "Like",
                                    tint = if (isLiked) Color(0xFF1DB954) else Color.Gray,
                                    modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Player controls
                Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    var isDragging by remember { mutableStateOf(false) }
                    var dragPosition by remember { mutableStateOf(0f) }

                    // Progress bar
                    Box(
                            modifier =
                                    Modifier.fillMaxWidth().height(24.dp).padding(vertical = 10.dp)
                    ) {
                        // Background track
                        LinearProgressIndicator(
                                progress = 1f,
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = Color.DarkGray,
                        )

                        // Progress track
                        LinearProgressIndicator(
                                progress =
                                        if (isDragging) dragPosition
                                        else if (MiniPlayerState.totalDuration > 0)
                                                MiniPlayerState.currentPosition.toFloat() /
                                                        MiniPlayerState.totalDuration
                                        else 0f,
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .height(4.dp)
                                                .pointerInput(Unit) {
                                                    detectHorizontalDragGestures(
                                                            onDragStart = { isDragging = true },
                                                            onDragEnd = {
                                                                isDragging = false
                                                                seekToPosition(dragPosition)
                                                            },
                                                            onDragCancel = { isDragging = false },
                                                            onHorizontalDrag = { change, dragAmount
                                                                ->
                                                                change.consume()
                                                                val newPosition =
                                                                        dragPosition +
                                                                                (dragAmount /
                                                                                        size.width)
                                                                dragPosition =
                                                                        newPosition.coerceIn(0f, 1f)
                                                            }
                                                    )
                                                }
                                                .pointerInput(Unit) {
                                                    detectTapGestures { offset ->
                                                        val position = offset.x / size.width
                                                        seekToPosition(position)
                                                    }
                                                },
                                color = Color(0xFF1DB954)
                        )
                    }

                    // Time stamps
                    Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                                text =
                                        formatTimeMs(
                                                if (isDragging)
                                                        (dragPosition *
                                                                        MiniPlayerState
                                                                                .totalDuration)
                                                                .toInt()
                                                else MiniPlayerState.currentPosition
                                        ),
                                color = Color.Gray,
                                fontSize = 14.sp
                        )
                        Text(
                                text = formatTimeMs(MiniPlayerState.totalDuration),
                                color = Color.Gray,
                                fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))

                    // Playback controls
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showAudioSheet = true }) {
                            Icon(
                                    Icons.Default.Speaker,
                                    contentDescription = "Pilih Output",
                                    modifier = Modifier.size(36.dp),
                                    tint = Color.White,
                            )
                        }

                        if (showAudioSheet) {
                            AudioOutputBottomSheet(
                                    context = context,
                                    activeDeviceId = activeDeviceId,
                                    onDismiss = { showAudioSheet = false },
                                    onDeviceSelected = { device ->
                                        if (setAudioOutput(context, device)) {
                                            activeDeviceId = device.id
                                        }
                                        showAudioSheet = false
                                    }
                            )
                        }

                        val devices = getAvailableOutputDevices(context)
                        val activeDevice = devices.find { it.id == activeDeviceId }

                        IconButton(onClick = { playPrev(context) }) {
                            Icon(
                                    painter = painterResource(id = R.drawable.skip_previous),
                                    contentDescription = "Previous",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(
                                onClick = {
                                    if (MiniPlayerState.isPlaying) {
                                        MiniPlayerState.mediaPlayer?.pause()
                                        MiniPlayerState.isPlaying = false
                                        ListeningTracker.pauseListening()
                                    } else {
                                        MiniPlayerState.mediaPlayer?.start()
                                        MiniPlayerState.isPlaying = true
                                        MiniPlayerState.currentSong?.let { song ->
                                            // Convert to LocalSong for ListeningTracker
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
                                },
                                modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                    painter =
                                            painterResource(
                                                    id =
                                                            if (MiniPlayerState.isPlaying)
                                                                    R.drawable.pause
                                                            else R.drawable.play_circle
                                            ),
                                    contentDescription =
                                            if (MiniPlayerState.isPlaying) "Pause" else "Play",
                                    tint = Color(0xFF1DB954),
                                    modifier = Modifier.fillMaxSize()
                            )
                        }

                        IconButton(onClick = { playNext(context) }) {
                            Icon(
                                    painter = painterResource(id = R.drawable.skip_next),
                                    contentDescription = "Next",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                            )
                        }

                        if (MiniPlayerState.currentSong is TopSong ||
                                        MiniPlayerState.currentSong is CountrySong
                        ) {
                            val onlineSong = MiniPlayerState.currentSong
                            val songId =
                                    when (onlineSong) {
                                        is TopSong -> onlineSong.id
                                        is CountrySong -> onlineSong.id
                                        else -> return
                                    }
                            IconButton(onClick = { shareSong(context, songId) }) {
                                Icon(
                                        Icons.Default.Share,
                                        contentDescription = "share song",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        // QR BUTTON
                        if (MiniPlayerState.currentSong is TopSong ||
                                        MiniPlayerState.currentSong is CountrySong
                        ) {
                            val onlineSong = MiniPlayerState.currentSong
                            val songId =
                                    when (onlineSong) {
                                        is TopSong -> onlineSong.id
                                        is CountrySong -> onlineSong.id
                                        else -> return
                                    }
                            IconButton(onClick = { showQrDialog = true }) {
                                Icon(
                                    Icons.Default.QrCode,
                                    contentDescription = "Share QR",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            if (showQrDialog) {
                                when (onlineSong) {
                                    is TopSong ->
                                            ShareQrDialog(
                                                    songId = onlineSong.id,
                                                    title = onlineSong.title,
                                                    artist = onlineSong.artist,
                                                    onDismiss = { showQrDialog = false },
                                                    context = context
                                            )
                                    is CountrySong ->
                                            ShareQrDialog(
                                                    songId = onlineSong.id,
                                                    title = onlineSong.title,
                                                    artist = onlineSong.artist,
                                                    onDismiss = { showQrDialog = false },
                                                    context = context
                                            )
                                }
                            }
                        }
                    }

                    val devices = getAvailableOutputDevices(context)
                    val activeDevice = devices.find { it.id == activeDeviceId }

                    Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start,
                    ) {
                        if (activeDevice != null) {
                            Text(
                                    text = "output device: ${activeDevice.name}",
                                    color = Color(0xFF1DB954),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                                    textAlign = TextAlign.Start,
                            )
                        }
                    }
                }
            }
        }
    }
}
