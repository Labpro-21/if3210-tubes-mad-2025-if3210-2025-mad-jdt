package com.purrytify.mobile.ui.screens

import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import com.purrytify.mobile.R

@Composable
fun TrackView() {
    var isPlaying by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableStateOf(0.4f) }

    var backgroundColor by remember { mutableStateOf(Color(0xFF140022)) }

    // Extract colors from the album art
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        try {
            val drawable = context.resources.getDrawable(R.drawable.song1, context.theme)
            val bitmap = (drawable as BitmapDrawable).bitmap

            Palette.from(bitmap).generate { palette ->
                palette?.let {
                    val darkColor = it.darkMutedSwatch?.rgb ?: it.darkVibrantSwatch?.rgb

                    backgroundColor = if (darkColor != null) {
                        Color(darkColor)
                    } else {
                        Color(0xFF140022)
                    }
                }
            }
        } catch (e: Exception) {
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* Handle back click */ }) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                IconButton(onClick = { /* Handle menu click */ }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.song1),
                    contentDescription = "Album Cover",
                    modifier = Modifier
                        .aspectRatio(1f)
                        .fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Starboy",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "The Weeknd, Daft Punk",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = { isFavorite = !isFavorite }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column {
                    // Assuming the total duration of the track is 3:50 (230 seconds)
                    val totalDurationInSeconds = 230

                    // Calculate current position in seconds based on slider value
                    val currentPositionInSeconds = (sliderPosition * totalDurationInSeconds).toInt()

                    // Format the time strings
                    val currentTimeFormatted = formatTime(currentPositionInSeconds)
                    val totalTimeFormatted = formatTime(totalDurationInSeconds)

                    Slider(
                        value = sliderPosition,
                        onValueChange = { newValue ->
                            sliderPosition = newValue
                            // The time text will update automatically on recomposition
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = currentTimeFormatted,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )

                        Text(
                            text = totalTimeFormatted,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Skip to previous */ }) {
                        Image(
                            painter = painterResource(R.drawable.skip_previous),
                            contentDescription = "Previous",
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    FloatingActionButton(
                        onClick = { isPlaying = !isPlaying },
                        containerColor = Color.Transparent,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Image(
                            painter = painterResource(
                                id = if (isPlaying) R.drawable.pause else R.drawable.play_circle
                            ),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    IconButton(onClick = { /* Skip to next */ }) {
                        Image(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = "Next",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(0.05f))
        }
    }
}

private fun formatTime(timeInSeconds: Int): String {
    val minutes = timeInSeconds / 60
    val seconds = timeInSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}