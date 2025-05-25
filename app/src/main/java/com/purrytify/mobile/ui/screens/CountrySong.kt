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
import com.purrytify.mobile.viewmodel.CountrySongUiState
import com.purrytify.mobile.viewmodel.CountrySongViewModel

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

      Image(
        painter = painterResource(id = R.drawable.country_top_50),
        contentDescription = "Top 50 Cover",
        modifier = Modifier
          .size(160.dp)
          .align(Alignment.CenterHorizontally),
        contentScale = ContentScale.Crop
      )

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text =
          "Your daily update of the most played tracks right now on your country.",
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
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        IconButton(
          onClick = { /* Bulk download */ },
          modifier = Modifier.size(56.dp)
        ) {
          Icon(
            painter = painterResource(id = R.drawable.download_for_offline_24),
            contentDescription = "Download All",
            tint = Color.White,
            modifier = Modifier.size(30.dp)
          )
        }
        IconButton(
          onClick = { /* Play All */ },
          modifier =
            Modifier
              .size(46.dp)
              .background(Color(0xFF1DB954), shape = CircleShape)
        ) {
          Icon(
            painter = painterResource(id = R.drawable.play_arrow),
            contentDescription = "Play All",
            tint = Color.Black,
            modifier = Modifier.size(30.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      when (uiState) {
        is CountrySongUiState.Loading -> {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            CircularProgressIndicator(color = Color.White)
          }
        }
        is CountrySongUiState.CountryNotSupported -> {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(16.dp),
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
              text =
                "Top songs are only available for these countries:",
              color = Color.White.copy(alpha = 0.8f),
              fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            val supported = viewModel.getSupportedCountries()
            val names =
              mapOf(
                "ID" to "Indonesia",
                "MY" to "Malaysia",
                "US" to "United States",
                "GB" to "United Kingdom",
                "CH" to "Switzerland",
                "DE" to "Germany",
                "BR" to "Brazil"
              )
            supported.forEach { code ->
              Text(
                text = "• ${names[code] ?: code}",
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
            modifier = Modifier
              .fillMaxSize()
              .padding(16.dp),
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
                Modifier
                  .background(
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
                Text(
                  text = "Retry",
                  color = Color.White,
                  fontSize = 14.sp
                )
              }
            }
          }
        }
        is CountrySongUiState.Success -> {
          val songs = (uiState as CountrySongUiState.Success).songs
          if (songs.isEmpty()) {
            Text(
              text = "No songs available",
              color = Color.White,
              modifier =
                Modifier
                  .padding(16.dp)
                  .align(Alignment.CenterHorizontally)
            )
          } else {
            LazyColumn {
              itemsIndexed(songs) { index, song ->
                Log.d("CountrySong", "Rendering song: ${song.title}")
                CountrySongItem(
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
}

@Composable
fun CountrySongItem(
  index: Int,
  song: CountrySong,
  viewModel: CountrySongViewModel
) {
  val context = LocalContext.current
  val countrySongs by viewModel.countrySongs.collectAsState()
  val downloadProgress by viewModel.downloadProgress.collectAsState()
  val downloadedSongs by viewModel.downloadedSongs.collectAsState()
  val isDownloaded = downloadedSongs.contains(song.id)

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable {
        Log.d("CountrySong", "Playing song: ${song.title}, URL: ${song.url}")
        MiniPlayerState.setQueue(countrySongs, index - 1, "country")
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
      model =
        ImageRequest.Builder(context)
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

    Column(modifier = Modifier.weight(1f)) {
      Text(text = song.title, fontSize = 16.sp, color = Color.White)
      Text(text = song.artist, fontSize = 14.sp, color = Color.Gray)
    }

    IconButton(onClick = {
      if (!isDownloaded) {
        viewModel.downloadSong(song, context)
      }
    }) {
      val progress = downloadProgress[song.id.toString()]
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

    IconButton(onClick = {
      Log.d("CountrySong", "Playing song: ${song.title}, URL: ${song.url}")
      MiniPlayerState.setQueue(countrySongs, index - 1, "country")
      playSong(song, context)
    }) {
      Icon(
        painter =
          painterResource(
            id =
              if (
                MiniPlayerState.currentUrl == song.url &&
                MiniPlayerState.isPlaying
              )
                R.drawable.pause
              else
                R.drawable.play_circle
          ),
        contentDescription = "Play",
        tint = Color(0xFF1DB954),
        modifier = Modifier.size(32.dp)
      )
    }
  }
}