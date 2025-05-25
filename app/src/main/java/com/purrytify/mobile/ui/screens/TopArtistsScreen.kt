package com.purrytify.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import com.purrytify.mobile.data.room.TopArtistData
import com.purrytify.mobile.viewmodel.TopArtistsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopArtistsScreen(navController: NavController) {
        val viewModel: TopArtistsViewModel = viewModel()
        val topArtists by viewModel.topArtists.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val currentMonth by viewModel.currentMonth.collectAsState()
        val totalArtists by viewModel.totalArtists.collectAsState()

        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .background(
                                        Brush.verticalGradient(
                                                colors =
                                                        listOf(
                                                                Color(0xFF1E3A8A),
                                                                Color(0xFF1E1B4B),
                                                                Color.Black
                                                        )
                                        )
                                )
        ) {
                Column(modifier = Modifier.fillMaxSize()) {
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
                                        text = "Top artists",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                )
                        }

                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Text(
                                        text = currentMonth,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 14.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                        text = "You listened to $totalArtists artists this month.",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium
                                )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Loading state
                        if (isLoading) {
                                Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                ) { CircularProgressIndicator(color = Color(0xFF1DB954)) }
                        } else {
                                // Artists list
                                LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                        itemsIndexed(topArtists) { index, artist ->
                                                TopArtistItem(
                                                        rank = index + 1,
                                                        artist = artist,
                                                        viewModel = viewModel
                                                )

                                                if (index < topArtists.size - 1) {
                                                        Spacer(modifier = Modifier.height(16.dp))
                                                }
                                        }

                                        item { Spacer(modifier = Modifier.height(100.dp)) }
                                }
                        }
                }
        }
}

@Composable
fun TopArtistItem(rank: Int, artist: TopArtistData, viewModel: TopArtistsViewModel) {
        val context = LocalContext.current
        val artistImageUrl by
                viewModel.getArtistImageUrl(artist.artist).collectAsState(initial = null)

        Row(
                modifier = Modifier.fillMaxWidth().clickable {}.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
                Text(
                        text = String.format("%02d", rank),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(40.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Artist image
                AsyncImage(
                        model =
                                ImageRequest.Builder(context)
                                        .data(artistImageUrl ?: R.drawable.profile_image)
                                        .error(R.drawable.profile_image)
                                        .placeholder(R.drawable.profile_image)
                                        .crossfade(true)
                                        .build(),
                        contentDescription = "Artist Image",
                        modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.Gray),
                        contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Artist name
                Text(
                        text = artist.artist,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                )
        }
}
