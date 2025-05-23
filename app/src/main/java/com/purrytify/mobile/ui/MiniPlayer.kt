package com.purrytify.mobile.ui

import android.media.MediaPlayer
import android.net.Uri
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.Share

object MiniPlayerState {
    var mediaPlayer: MediaPlayer? = null
    var currentSong: LocalSong? by mutableStateOf(null)
    var isPlaying by mutableStateOf(false)
    var currentPosition by mutableStateOf(0)
    var totalDuration by mutableStateOf(0)
    var isExpanded by mutableStateOf(false)
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

    AnimatedVisibility(
        visible = MiniPlayerState.isExpanded,
        enter = slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        ),
        exit = slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        )
    ) {
        ExpandedPlayer(onDismiss = { MiniPlayerState.isExpanded = false })
    }

    MiniPlayerState.currentSong?.let { song ->
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(Color(0xFF282828))
                .clickable { MiniPlayerState.isExpanded = true }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedPlayer(
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF282828))
    ) {
        val offsetY = remember { mutableStateOf(0f) }
        val scope = rememberCoroutineScope()
        var showOptions by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .offset(y = offsetY.value.dp)
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        val newOffset = offsetY.value + delta
                        if (newOffset >= 0) { // Only allow dragging down
                            offsetY.value = newOffset
                        }
                    },
                    onDragStopped = { velocity ->
                        if (offsetY.value > 300 || velocity > 300f) {
                            // Dismiss if dragged far enough or with enough velocity
                            onDismiss()
                        } else {
                            // Spring back to original position
                            scope.launch {
                                animate(
                                    initialValue = offsetY.value,
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ) { value, _ ->
                                    offsetY.value = value
                                }
                            }
                        }
                    }
                )
        ) {
            // Top bar with close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close button on the left
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_down),
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Options menu on the right
                Box {
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
                            text = { Text("Share", color = Color.White) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Edit",
                                    tint = Color.White
                                )
                            },
                            onClick = {
                                showOptions = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit", color = Color.White) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = Color.White
                                )
                            },
                            onClick = {
                                showOptions = false
                            }
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
                            }
                        )
                    }
                }
            }

            MiniPlayerState.currentSong?.let { song ->
                // Album artwork
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentSize(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .size(320.dp)
                            .background(Color.DarkGray, RoundedCornerShape(8.dp)),
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
                                        .load(Uri.parse(song.artworkPath))
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
                    // Song title and artist (aligned start)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = song.title,
                            style = TextStyle(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = LocalPoppinsFont.current,
                                color = Color.White
                            ),
                            textAlign = TextAlign.Start
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = song.artist,
                            style = TextStyle(
                                fontSize = 20.sp,
                                fontFamily = LocalPoppinsFont.current,
                                color = Color.Gray
                            ),
                            textAlign = TextAlign.Start
                        )
                    }

                    // Like button
                    IconButton(
                        onClick = {},
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (song.isLiked) 
                                    R.drawable.ic_heart_filled 
                                else R.drawable.ic_heart_outline
                            ),
                            contentDescription = if (song.isLiked) 
                                "Unlike" 
                            else "Like",
                            tint = if (song.isLiked) 
                                Color(0xFF1DB954) 
                            else Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp) // Increased touch target
                            .padding(vertical = 10.dp)
                    ) {
                        // Background track
                        LinearProgressIndicator(
                            progress = 1f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = Color.DarkGray,
                        )

                        // Progress track
                        LinearProgressIndicator(
                            progress = if (isDragging) 
                                dragPosition 
                            else if (MiniPlayerState.totalDuration > 0)
                                MiniPlayerState.currentPosition.toFloat() / MiniPlayerState.totalDuration
                            else 0f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { 
                                            isDragging = true 
                                        },
                                        onDragEnd = {
                                            isDragging = false
                                            seekToPosition(dragPosition)
                                        },
                                        onDragCancel = { 
                                            isDragging = false 
                                        },
                                        onHorizontalDrag = { change, dragAmount ->
                                            change.consume()
                                            val newPosition = dragPosition + (dragAmount / size.width)
                                            dragPosition = newPosition.coerceIn(0f, 1f)
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTimeMs(
                                if (isDragging) 
                                    (dragPosition * MiniPlayerState.totalDuration).toInt()
                                else 
                                    MiniPlayerState.currentPosition
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
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { /* Previous song */ }) {
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
                                } else {
                                    MiniPlayerState.mediaPlayer?.start()
                                    MiniPlayerState.isPlaying = true
                                }
                            },
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (MiniPlayerState.isPlaying) 
                                        R.drawable.pause 
                                    else R.drawable.play_circle
                                ),
                                contentDescription = if (MiniPlayerState.isPlaying) 
                                    "Pause" 
                                else "Play",
                                tint = Color(0xFF1DB954),
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        IconButton(onClick = { /* Next song */ }) {
                            Icon(
                                painter = painterResource(id = R.drawable.skip_next),
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}