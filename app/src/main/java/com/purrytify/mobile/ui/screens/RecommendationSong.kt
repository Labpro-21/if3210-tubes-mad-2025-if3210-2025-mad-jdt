package com.purrytify.mobile.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.purrytify.mobile.R
import com.purrytify.mobile.data.CountrySongRepository
import com.purrytify.mobile.data.SongRepository
import com.purrytify.mobile.ui.MiniPlayerState
import com.purrytify.mobile.ui.playSong
import com.purrytify.mobile.viewmodel.RecommendationSong
import com.purrytify.mobile.viewmodel.RecommendationSongViewModel

@Composable
fun RecommendationSong(
    navController: NavController,
    globalRepository: SongRepository,
    countryRepository: CountrySongRepository
) {
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<RecommendationSongViewModel>(
        factory = RecommendationSongViewModel.provideFactory(globalRepository, countryRepository)
    )
    
    val recommendationSongs by viewModel.recommendationSongs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF1C8075), Color(0xFF1D4569), Color(0xFF121212)),
        startY = 0f, 
        endY = 1000f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp)
                .background(gradient)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            IconButton(
                onClick = { navController.navigateUp() },
                modifier = Modifier
                    .size(48.dp)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Cover and title
            Image(
                painter = painterResource(id = R.drawable.recommendation_song),
                contentDescription = "Recommendation Song Cover",
                modifier = Modifier
                    .size(160.dp)
                    .align(Alignment.CenterHorizontally),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your personalized mix of global hits and local favorites.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Purrytify • Mixed Playlist • ${recommendationSongs.size} songs", 
                fontSize = 12.sp, 
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
                recommendationSongs.isEmpty() -> {
                    Text(
                        text = "No recommendation songs available",
                        color = Color.White,
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }
                else -> {
                    LazyColumn {
                        itemsIndexed(recommendationSongs) { index, song ->
                            RecommendSongItem(
                                index = index + 1,
                                song = song,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecommendSongItem(
    index: Int,
    song: RecommendationSong,
    viewModel: RecommendationSongViewModel
) {
    val context = LocalContext.current
    val downloadProgress: Map<String, Float> = viewModel.downloadProgress.collectAsState().value
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                Log.d("RecommendationSong", "Playing song: ${song.title}, URL: ${song.url}, Type: ${song.type}")
                // Convert to appropriate type for playSong function
                when (song.type) {
                    "global" -> playSong(song.toTopSong(), context)
                    "country" -> playSong(song.toCountrySong(), context)
                }
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
            model = ImageRequest.Builder(context)
                .data(song.artwork)
                .crossfade(true)
                .build(),
            contentDescription = "Song artwork",
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = song.title, fontSize = 16.sp, color = Color.White)
            Row {
                Text(text = song.artist, fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "• ${song.type}",
                    fontSize = 12.sp,
                    color = Color(0xFF1DB954)
                )
            }
        }

        // Download button
        IconButton(
            onClick = {
                viewModel.downloadSong(song, context)
            }
        ) {
            val progress = downloadProgress[song.id.toString()]
            if (progress != null) {
                CircularProgressIndicator(
                    progress = progress,
                    color = Color(0xFF1DB954),
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.download_for_offline_24),
                    contentDescription = "Download",
                    tint = Color(0xFF1DB954),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Play button
        IconButton(
            onClick = {
                Log.d("RecommendationSong", "Playing song: ${song.title}, URL: ${song.url}, Type: ${song.type}")
                when (song.type) {
                    "global" -> playSong(song.toTopSong(), context)
                    "country" -> playSong(song.toCountrySong(), context)
                }
            }
        ) {
            Icon(
                painter = painterResource(
                    id = if (MiniPlayerState.currentUrl == song.url && MiniPlayerState.isPlaying)
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