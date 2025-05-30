package com.purrytify.mobile.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.purrytify.mobile.R
import com.purrytify.mobile.data.SongRepository
import com.purrytify.mobile.data.room.TopSong
import com.purrytify.mobile.ui.MiniPlayerState
import com.purrytify.mobile.ui.playSong
import com.purrytify.mobile.viewmodel.GlobalSongViewModel

@Composable
fun GlobalSong(navController: NavController, repository: SongRepository) {
    val viewModel =
            androidx.lifecycle.viewmodel.compose.viewModel<GlobalSongViewModel>(
                    factory = GlobalSongViewModel.provideFactory(repository)
            )

    val topSongs by viewModel.topSongs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val gradient =
            Brush.verticalGradient(
                    colors = listOf(Color(0xFF1C8075), Color(0xFF1D4569), Color(0xFF121212)),
                    startY = 0f,
                    endY = 1000f
            )

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
        Box(modifier = Modifier.fillMaxWidth().height(600.dp).background(gradient))

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            IconButton(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier.size(48.dp).padding(8.dp)
            ) {
                Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cover dan judul
            Image(
                    painter = painterResource(id = R.drawable.top_50),
                    contentDescription = "Top 50 Cover",
                    modifier = Modifier.size(160.dp).align(Alignment.CenterHorizontally),
                    contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                    text = "Your daily update of the most played tracks right now – Global.",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                    text = "Puritify • Apr 2025 • 2h 55min",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                itemsIndexed(topSongs) { index, song ->
                    SongItem(
                            index = index + 1,
                            song = song,
                            viewModel = viewModel,
                            navController = navController
                    )
                }
            }
        }
    }
}

@Composable
fun SongItem(
        index: Int,
        song: TopSong,
        viewModel: GlobalSongViewModel,
        navController: NavController
) {
    val context = LocalContext.current
    val topSongs by viewModel.topSongs.collectAsState()
    val downloadProgress: Map<String, Float> = viewModel.downloadProgress.collectAsState().value

    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .clickable {
                                Log.d("GlobalSong", "Playing song: ${song.title}, URL: ${song.url}")
                                MiniPlayerState.setQueue(topSongs, index - 1, "global")
                                playSong(song, context)
                            }
                            .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
                text = index.toString(),
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.width(24.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        AsyncImage(
                model = ImageRequest.Builder(context).data(song.artwork).crossfade(true).build(),
                contentDescription = "Song artwork",
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = song.title, fontSize = 16.sp, color = Color.White)
            Text(text = song.artist, fontSize = 14.sp, color = Color.Gray)
        }

        // Download button
        IconButton(
                onClick = {
                    val downloadedSongs = viewModel.downloadedSongs.value
                    if (!downloadedSongs.contains(song.id)) {
                        viewModel.downloadSong(song, context)
                    }
                }
        ) {
            val progress = downloadProgress[song.id.toString()]
            val downloadedSongs by viewModel.downloadedSongs.collectAsState()
            val isDownloaded = downloadedSongs.contains(song.id)

            when {
                progress != null -> {
                    CircularProgressIndicator(
                            progress = progress,
                            color = Color(0xFF1DB954),
                            modifier = Modifier.size(24.dp)
                    )
                }
                isDownloaded -> {
                    Icon(
                            painter = painterResource(id = R.drawable.download_done),
                            contentDescription = "Downloaded",
                            tint = Color(0xFF1DB954),
                            modifier = Modifier.size(24.dp)
                    )
                }
                else -> {
                    Icon(
                            painter = painterResource(id = R.drawable.download_for_offline_24),
                            contentDescription = "Download",
                            tint = Color(0xFF1DB954),
                            modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Play button
        IconButton(
                onClick = {
                    Log.d("GlobalSong", "Playing song: ${song.title}, URL: ${song.url}")
                    MiniPlayerState.setQueue(topSongs, index - 1, "global")
                    playSong(song, context)
                }
        ) {
            Icon(
                    painter =
                            painterResource(
                                    id =
                                            if (MiniPlayerState.currentUrl == song.url &&
                                                            MiniPlayerState.isPlaying
                                            )
                                                    R.drawable.pause
                                            else R.drawable.play_circle
                            ),
                    contentDescription = "Play",
                    tint = Color(0xFF1DB954),
                    modifier = Modifier.size(32.dp)
            )
        }
    }
}
