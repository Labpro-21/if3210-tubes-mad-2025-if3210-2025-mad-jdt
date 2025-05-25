package com.purrytify.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.purrytify.mobile.R
import com.purrytify.mobile.data.room.TopSongData
import com.purrytify.mobile.viewmodel.TopSongsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopSongsScreen(navController: NavController) {
    val viewModel: TopSongsViewModel = viewModel()
    val topSongs by viewModel.topSongs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val totalSongs by viewModel.totalSongs.collectAsState()

    Box(
            modifier =
                    Modifier.fillMaxSize()
                            .background(
                                    Brush.verticalGradient(
                                            colors =
                                                    listOf(
                                                            Color(0xFF8B5CF6),
                                                            Color(0xFF7C3AED),
                                                            Color.Black
                                                    )
                                    )
                            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                        text = "Top songs",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                )
            }

            // Month and stats
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(text = currentMonth, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                        text = "You played $totalSongs different songs this month.",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Loading state
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF1DB954))
                }
            } else {
                // Songs list
                LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    itemsIndexed(topSongs) { index, song ->
                        TopSongItem(rank = index + 1, song = song, viewModel = viewModel)

                        if (index < topSongs.size - 1) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Bottom padding for mini player
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }
    }
}

@Composable
fun TopSongItem(rank: Int, song: TopSongData, viewModel: TopSongsViewModel) {
    val context = LocalContext.current
    val songImageUrl by
            viewModel.getSongImageUrl(song.songTitle, song.artist).collectAsState(initial = null)
    val playCount = song.playCount

    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .clickable { /* Handle song click if needed */}
                            .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank number
        Text(
                text = String.format("%02d", rank),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(40.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Song image
        AsyncImage(
                model =
                        ImageRequest.Builder(context)
                                .data(songImageUrl ?: R.drawable.profile_image)
                                .error(R.drawable.profile_image)
                                .placeholder(R.drawable.profile_image)
                                .crossfade(true)
                                .build(),
                contentDescription = "Song Image",
                modifier =
                        Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray),
                contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Song info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                    text = song.songTitle,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                    text = song.artist,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Play count
        Text(text = "$playCount plays", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
    }
}
