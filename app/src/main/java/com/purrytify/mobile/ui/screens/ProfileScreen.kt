package com.purrytify.mobile.ui.screens

// Remove unused imports related to editing if any remain
// import androidx.compose.animation.AnimatedVisibility
// import androidx.compose.animation.slideInVertically
// import androidx.compose.animation.slideOutVertically
// import androidx.compose.material3.Icon
// import androidx.compose.material3.IconButton
// import androidx.compose.material3.TextButton
// import androidx.compose.material3.TextField
// import androidx.compose.material3.TextFieldDefaults
// import androidx.compose.runtime.setValue
// import androidx.compose.runtime.getValue
// import androidx.compose.runtime.mutableStateOf
// import androidx.compose.runtime.remember
// import androidx.compose.ui.res.painterResource

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
import androidx.compose.foundation.layout.width // Keep width if StatItem uses it
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
import com.purrytify.mobile.api.ProfileResponse // Keep ProfileResponse if StatItem uses it
import com.purrytify.mobile.data.AuthRepository
import com.purrytify.mobile.viewmodel.ProfileUiState
import com.purrytify.mobile.viewmodel.ProfileViewModel
import com.purrytify.mobile.viewmodel.ProfileViewModelFactory
// import com.purrytify.mobile.api.ApiClient // Remove if baseUrl is hardcoded and not needed elsewhere

@Composable
fun ProfileScreen(
    authRepository: AuthRepository,
    onLogout: () -> Unit
) {
    // Remove edit modal state
    // var showEditModal by remember { mutableStateOf(false) }

    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(authRepository)
    )

    val profileState by profileViewModel.profileUiState.collectAsState()

    LaunchedEffect(Unit) {
        profileViewModel.fetchProfile()
    }

    val baseUrl = "http://34.101.226.132:3000" // Keep or get from a config

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
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

                        // Profile Picture (No edit icon)
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

                        // Remove Edit Profile Button
                        // Spacer(modifier = Modifier.height(16.dp))
                        // Button(
                        //     onClick = { showEditModal = true },
                        //     ...
                        // ) {
                        //     Text("Edit Profile", color = Color.White)
                        // }

                        Spacer(modifier = Modifier.height(30.dp)) // Adjust spacing if needed after removing button
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
                        Text(
                            text = "Error: ${(profileState as ProfileUiState.Error).message}",
                            color = Color.Red,
                            modifier = Modifier.padding(top = 50.dp)
                        )
                    }
                }
            }
        }

        // Remove AnimatedVisibility and EditProfileModal call
        // AnimatedVisibility(...) { ... }
    }
}

// Remove EditProfileModal composable function entirely
// @Composable
// fun EditProfileModal(...) { ... }


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