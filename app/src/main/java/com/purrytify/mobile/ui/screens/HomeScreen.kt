package com.purrytify.mobile.ui.screens

import android.media.MediaPlayer
import android.net.Uri
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.Glide
import com.purrytify.mobile.LocalPoppinsFont
import com.purrytify.mobile.R
import com.purrytify.mobile.data.room.LocalSong
import com.purrytify.mobile.viewmodel.LocalSongViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.purrytify.mobile.ui.MiniPlayerState
import com.purrytify.mobile.ui.playSong

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val localSongViewModel: LocalSongViewModel = viewModel()
    val allSongs by localSongViewModel.allSongs.collectAsState()
    val recentlyPlayedSongs by remember { mutableStateOf(allSongs.take(5)) }
    val newSongs by remember { mutableStateOf(allSongs.takeLast(10)) }
    val isLoading by localSongViewModel.isLoading.collectAsState()
    val errorMessage by localSongViewModel.errorMessage.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Loading state
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF1DB954))
                }
            }

            // Error handling
            errorMessage?.let { error ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = error,
                        color = Color.Red,
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontFamily = LocalPoppinsFont.current
                        )
                    )
                }
            }

            // Main content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp)
                    .padding(bottom = if (MiniPlayerState.currentSong != null) 0.dp else 0.dp)
            ) {
                // New Songs Section First
                item {
                    Text(
                        text = "New Songs",
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = LocalPoppinsFont.current,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
                    )

                    if (newSongs.isEmpty() && !isLoading) {
                        EmptyStateMessage(message = "No new songs available")
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            items(newSongs) { song ->
                                HomeSongItem(
                                    song = song,
                                    onPlayClick = { playSong(song, context) },
                                    isPlaying = MiniPlayerState.isPlaying && MiniPlayerState.currentSong?.id == song.id
                                )
                            }
                        }
                    }
                }

                // Spacer between sections
                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }

                // Recently Played Section
                item {
                    Text(
                        text = "Recently Played",
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = LocalPoppinsFont.current,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (recentlyPlayedSongs.isEmpty() && !isLoading) {
                        EmptyStateMessage(message = "No recently played songs")
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            recentlyPlayedSongs.forEach { song ->
                                RecentSongCard(
                                    song = song,
                                    onPlayClick = { playSong(song, context) },
                                    isPlaying = MiniPlayerState.isPlaying && MiniPlayerState.currentSong?.id == song.id
                                )
                            }
                        }
                    }
                }

                // Add bottom padding
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun HomeSongItem(
    song: LocalSong,
    onPlayClick: () -> Unit,
    isPlaying: Boolean
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable { onPlayClick() }
    ) {
        // Album artwork
        Box(
            modifier = Modifier
                .size(130.dp)
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
                    modifier = Modifier.size(48.dp)
                )
            }
            
            // Show playing indicator
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x66000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.pause),
                        contentDescription = "Now Playing",
                        tint = Color(0xFF1DB954),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Song title
        Text(
            text = song.title,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = LocalPoppinsFont.current,
                color = Color.White
            ),
            maxLines = 1,
            textAlign = TextAlign.Start
        )

        // Artist
        Text(
            text = song.artist,
            style = TextStyle(
                fontSize = 12.sp,
                fontFamily = LocalPoppinsFont.current,
                color = Color.Gray
            ),
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RecentSongCard(
    song: LocalSong,
    onPlayClick: () -> Unit,
    isPlaying: Boolean
) {
    val context = LocalContext.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = LocalPoppinsFont.current,
                    color = Color.White
                ),
                maxLines = 1
            )

            Text(
                text = song.artist,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontFamily = LocalPoppinsFont.current,
                    color = Color.Gray
                ),
                maxLines = 1
            )
        }

        Icon(
            painter = painterResource(
                id = if (song.isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            ),
            contentDescription = "Like",
            tint = if (song.isLiked) Color(0xFF1DB954) else Color.Gray,
            modifier = Modifier
                .size(24.dp)
                .clickable { /* Handle like toggle */ }
                .padding(end = 8.dp)
        )

        Icon(
            painter = painterResource(
                id = if (isPlaying) R.drawable.pause else R.drawable.play_circle
            ),
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color(0xFF1DB954),
            modifier = Modifier
                .size(32.dp)
                .clickable { onPlayClick() }
        )
    }
}

@Composable
fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = TextStyle(
                fontSize = 16.sp,
                fontFamily = LocalPoppinsFont.current,
                color = Color.Gray
            )
        )
    }
}
