package com.purrytify.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.purrytify.mobile.R
import com.purrytify.mobile.data.AuthRepository
import com.purrytify.mobile.viewmodel.ProfileUiState
import com.purrytify.mobile.viewmodel.ProfileViewModel
import com.purrytify.mobile.viewmodel.ProfileViewModelFactory
import com.purrytify.mobile.utils.NetworkConnectivityObserver

@Composable
fun ProfileScreen(
    authRepository: AuthRepository,
    networkConnectivityObserver: NetworkConnectivityObserver,
    onLogout: () -> Unit
) {
    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(authRepository, networkConnectivityObserver)
    )

    val profileState by profileViewModel.profileUiState.collectAsState()
    val networkStatus by profileViewModel.networkStatus.collectAsState()

    LaunchedEffect(Unit) {
        profileViewModel.fetchProfile()
    }

    val baseUrl = "http://34.101.226.132:3000"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (profileState) {
            is ProfileUiState.NetworkError -> {
                NoNetworkScreen(onRetry = {
                    profileViewModel.fetchProfile()
                })
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF00667B),
                                    Color(0xFF002F38),
                                    Color.Black
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 60.dp, start = 16.dp, end = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (profileState) {
                            is ProfileUiState.Loading, ProfileUiState.Initial -> {
                                CircularProgressIndicator(modifier = Modifier.padding(top = 50.dp))
                            }
                            is ProfileUiState.Success -> {
                                val profile = (profileState as ProfileUiState.Success).profile
                                val profileImageUrl = profile.profilePhoto?.let { "$baseUrl/uploads/profile-picture/$it" }

                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(profileImageUrl ?: R.drawable.profile_image)
                                        .error(R.drawable.profile_image)
                                        .placeholder(R.drawable.profile_image)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(CircleShape)
                                        .background(Color.Gray),
                                    contentScale = ContentScale.Crop
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = profile.username,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = profile.location,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 14.sp
                                )

                                Spacer(modifier = Modifier.height(30.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    StatItem("135", "SONGS")
                                    StatItem("32", "LIKED")
                                    StatItem("50", "LISTENED")
                                }

                                Spacer(modifier = Modifier.height(30.dp))
                                Button(
                                    onClick = onLogout,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Red.copy(alpha = 0.6f)
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .fillMaxWidth(0.5f)
                                        .height(36.dp)
                                ) {
                                    Text("Log Out", color = Color.White)
                                }
                            }
                            is ProfileUiState.Error -> {
                                val errorMessage = (profileState as ProfileUiState.Error).message
                                Text(
                                    text = "Error: $errorMessage",
                                    color = Color.White,
                                    modifier = Modifier.padding(top = 50.dp)
                                )
                                Button(
                                    onClick = { profileViewModel.fetchProfile() },
                                    modifier = Modifier.padding(top = 16.dp)
                                ) {
                                    Text("Retry")
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
    }
}