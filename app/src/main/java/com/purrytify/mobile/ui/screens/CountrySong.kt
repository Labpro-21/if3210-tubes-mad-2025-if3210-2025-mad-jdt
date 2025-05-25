package com.purrytify.mobile.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.purrytify.mobile.R
import com.purrytify.mobile.data.CountrySongRepository
import com.purrytify.mobile.data.room.CountrySong
import com.purrytify.mobile.ui.MiniPlayerState
import com.purrytify.mobile.ui.playSong

@Composable
fun CountrySong(navController: NavController, repository: CountrySongRepository) {
    val viewModel =
            androidx.lifecycle.viewmodel.compose.viewModel<CountrySongViewModel>(
                    factory = CountrySongViewModel.provideFactory(repository)
            )

    val uiState by viewModel.uiState.collectAsState()

    val gradient =
            Brush.verticalGradient(
                    colors = listOf(Color(0xFFf36a78), Color(0xFFEF2D40), Color(0xFF121212)),
                    startY = 0f,
                    endY = 1000f
            )

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(600.dp) // Adjust this value to control gradient height
                                .background(gradient)
        )

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
                    painter = painterResource(id = R.drawable.country_top_50),
                    contentDescription = "Top 50 Cover",
                    modifier = Modifier.size(160.dp).align(Alignment.CenterHorizontally),
                    contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                    text = "Your daily update of the most played tracks right now on your country.",
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

            Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Download button on the left
                IconButton(
                        onClick = { /* Download functionality */},
                        modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                            painter = painterResource(id = R.drawable.download_for_offline_24),
                            contentDescription = "Download",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                    )
                }

                // Play button on the right
                IconButton(
                        onClick = { /* Play functionality */},
                        modifier =
                                Modifier.size(46.dp)
                                        .background(Color(0xFF1DB954), shape = CircleShape)
                ) {
                    Icon(
                            painter = painterResource(id = R.drawable.play_arrow),
                            contentDescription = "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (uiState) {
                is CountrySongUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
                is CountrySongUiState.CountryNotSupported -> {
                    Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = "Country not supported",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(64.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                                text = "Country Not Supported",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                                text = "Top songs are only available for these countries:",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val supportedCountries = viewModel.getSupportedCountries()
                        val countryNames =
                                mapOf(
                                        "ID" to "Indonesia",
                                        "MY" to "Malaysia",
                                        "US" to "United States",
                                        "GB" to "United Kingdom",
                                        "CH" to "Switzerland",
                                        "DE" to "Germany",
                                        "BR" to "Brazil"
                                )

                        supportedCountries.forEach { countryCode ->
                            Text(
                                    text = "• ${countryNames[countryCode] ?: countryCode}",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                                text =
                                        "Please update your location in your profile to one of the supported countries.",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                        )
                    }
                }
                is CountrySongUiState.Error -> {
                    Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                                text = "Error",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                                text = (uiState as CountrySongUiState.Error).message,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        IconButton(
                                onClick = { viewModel.retry() },
                                modifier =
                                        Modifier.background(
                                                        Color(0xFF1DB954),
                                                        shape = RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                        painter = painterResource(id = R.drawable.play_arrow),
                                        contentDescription = "Retry",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Retry", color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                }
                is CountrySongUiState.Success -> {
                    if ((uiState as CountrySongUiState.Success).songs.isEmpty()) {
                        Text(
                                text = "No songs available",
                                color = Color.White,
                                modifier =
                                        Modifier.padding(16.dp).align(Alignment.CenterHorizontally)
                        )
                    } else {
                        LazyColumn {
                            itemsIndexed((uiState as CountrySongUiState.Success).songs) {
                                    index,
                                    song ->
                                Log.d("CountrySong", "Rendering song: ${song.title}")
                                CountrySongItem(index = index + 1, song = song)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CountrySongItem(index: Int, song: CountrySong) {
    val context = LocalContext.current

    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .clickable {
                                Log.d("GlobalSong", "Playing song: ${song.title}, URL: ${song.url}")
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

        // Add song artwork
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

        // Add play button
        IconButton(
                onClick = {
                    Log.d("GlobalSong", "Playing song: ${song.title}, URL: ${song.url}")
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
