package com.purrytify.mobile.ui.screens

import android.app.Activity
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.navigation.NavHostController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.IconButton
import com.google.zxing.integration.android.IntentIntegrator

@Composable
fun HomeScreen(
    navController: NavHostController, // Main navController
    nestedNavController: NavHostController, // Nested navController  
    onQrResult: (Int, String?) -> Unit // Callback untuk QR result
) {
    val context = LocalContext.current
    val localSongViewModel: LocalSongViewModel = viewModel()
    val allSongs by localSongViewModel.allSongs.collectAsState()
    val recentlyPlayedSongs by remember { mutableStateOf(allSongs.take(5)) }
    val newSongs by remember { mutableStateOf(allSongs.takeLast(10)) }
    val isLoading by localSongViewModel.isLoading.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            Log.d("ScanQR", "Launcher result received")
            val intent = result.data
            val contents = IntentIntegrator.parseActivityResult(result.resultCode, intent)?.contents
            Log.d("ScanQR", "Scanned contents: $contents")
            
            if (contents != null && contents.startsWith("purrytify://song/")) {
                Log.d("ScanQR", "Valid QR detected")
                
                val songIdString = contents.removePrefix("purrytify://song/").split("?")[0]
                val songId = songIdString.toIntOrNull()
                val type = if (contents.contains("type=country")) "country" else null
                
                Log.d("ScanQR", "Parsed songId: $songId, type: $type")
                
                if (songId != null) {
                    Log.d("ScanQR", "Calling onQrResult callback")
                    onQrResult(songId, type)
                } else {
                    Log.d("ScanQR", "Invalid songId")
                    errorMessage = "QR tidak valid"
                }
            } else {
                Log.d("ScanQR", "Invalid QR content")
                errorMessage = "QR tidak valid"
            }
        } catch (e: Exception) {
            Log.e("ScanQR", "Error processing scan result: ${e.message}")
            errorMessage = "Error: ${e.message}"
        }
    }

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
                     Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Charts text on the left
                        Text(
                            text = "Charts",
                            style = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = LocalPoppinsFont.current,
                                color = Color.White
                            )
                        )
                        
                        // QR Scanner button on the right
                        IconButton(
                            onClick = {
                                Log.d("ScanQR", "Button clicked")
                                try {
                                    val activity = context as? Activity
                                    if (activity == null) {
                                        Log.e("ScanQR", "Context is not Activity")
                                        errorMessage = "Error: Context bukan Activity"
                                        return@IconButton
                                    }
                                    
                                    Log.d("ScanQR", "Creating IntentIntegrator")
                                    val integrator = IntentIntegrator(activity)
                                    integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                                    integrator.setPrompt("Scan QR lagu Purrytify")
                                    integrator.setOrientationLocked(true)
                                    
                                    Log.d("ScanQR", "Launching scanner")
                                    launcher.launch(integrator.createScanIntent())
                                    Log.d("ScanQR", "Scanner launched")
                                } catch (e: Exception) {
                                    Log.e("ScanQR", "Error launching scanner: ${e.message}")
                                    errorMessage = "Error: ${e.message}"
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCodeScanner,
                                contentDescription = "Scan QR",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .clickable { 
                                        nestedNavController.navigate("global_song")  // Nested navigation
                                    }
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.top_50),
                                    contentDescription = "Top 50 Cover",
                                    modifier = Modifier
                                        .size(150.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        item {
                            Box(
                                modifier = Modifier
                                    .clickable {
                                        nestedNavController.navigate("country_song")  // Nested navigation
                                    }
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.country_top_50),
                                    contentDescription = "Country Top 50 Cover",
                                    modifier = Modifier
                                        .size(150.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        item {
                            Box(
                                modifier = Modifier
                                    .clickable {
                                        nestedNavController.navigate("recommendation_song")
                                    }
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.recommendation_song),
                                    contentDescription = "Recommendation Cover",
                                    modifier = Modifier
                                        .size(150.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

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
                                    isPlaying = isCurrentSongLocalSongId(song.id)
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
                                    isPlaying = isCurrentSongLocalSongId(song.id)
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
