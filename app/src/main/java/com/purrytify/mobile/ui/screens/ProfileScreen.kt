package com.purrytify.mobile.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.purrytify.mobile.utils.NetworkConnectivityObserver
import com.purrytify.mobile.viewmodel.EditProfileState
import com.purrytify.mobile.viewmodel.ProfileUiState
import com.purrytify.mobile.viewmodel.ProfileViewModel
import com.purrytify.mobile.viewmodel.ProfileViewModelFactory
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authRepository: AuthRepository,
    networkConnectivityObserver: NetworkConnectivityObserver,
    onLogout: () -> Unit
) {
    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(authRepository, networkConnectivityObserver)
    )

    val context = LocalContext.current
    val profileState by profileViewModel.profileUiState.collectAsState()
    val editProfileState by profileViewModel.editProfileState.collectAsState()
    val networkStatus by profileViewModel.networkStatus.collectAsState()

    var showEditSheet by remember { mutableStateOf(false) }
    var showImagePicker by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var currentLocation by remember { mutableStateOf<String?>(null) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            // The image has been saved to the Uri we provided
            showImagePicker = false
        }
    }

    LaunchedEffect(Unit) {
        profileViewModel.fetchProfile()
    }

    LaunchedEffect(editProfileState) {
        when (editProfileState) {
            is EditProfileState.Success -> {
                showEditSheet = false
                selectedImageUri = null
            }

            is EditProfileState.Error -> {
                Toast.makeText(
                    context,
                    (editProfileState as EditProfileState.Error).message,
                    Toast.LENGTH_SHORT
                ).show()
            }

            else -> {}
        }
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
                                val profileImageUrl =
                                    profile.profilePhoto?.let { "$baseUrl/uploads/profile-picture/$it" }
                                currentLocation = profile.location

                                Box(
                                    contentAlignment = Alignment.TopEnd
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(
                                                selectedImageUri ?: (profileImageUrl
                                                    ?: R.drawable.profile_image)
                                            )
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

                                    IconButton(
                                        onClick = { showEditSheet = true },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Color(0xFF1DB954), CircleShape)
                                            .padding(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Profile",
                                            tint = Color.White
                                        )
                                    }
                                }

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

        if (showEditSheet) {
            ModalBottomSheet(
                onDismissRequest = { showEditSheet = false },
                containerColor = Color(0xFF282828)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Edit Profile",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Current/Preview Image
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(
                                selectedImageUri ?: (when (profileState) {
                                    is ProfileUiState.Success -> {
                                        val profile =
                                            (profileState as ProfileUiState.Success).profile
                                        profile.profilePhoto?.let { "$baseUrl/uploads/profile-picture/$it" }
                                    }

                                    else -> R.drawable.profile_image
                                }))
                            .error(R.drawable.profile_image)
                            .placeholder(R.drawable.profile_image)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile Picture Preview",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.Gray)
                            .clickable { showImagePicker = true },
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Tap to change profile picture",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = currentLocation ?: "",
                        onValueChange = { currentLocation = it },
                        label = { Text("Location") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1DB954),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFF1DB954),
                            unfocusedLabelColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { showEditSheet = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.DarkGray
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Button(
                            onClick = {
                                selectedImageUri?.let { uri ->
                                    val file = File(context.cacheDir, "profile_photo")
                                    context.contentResolver.openInputStream(uri)?.use { input ->
                                        file.outputStream().use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    profileViewModel.editProfile(currentLocation, file)
                                } ?: run {
                                    profileViewModel.editProfile(currentLocation, null)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1DB954)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (editProfileState is EditProfileState.Loading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text("Save")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        if (showImagePicker) {
            AlertDialog(
                onDismissRequest = { showImagePicker = false },
                title = { Text("Choose Image Source", color = Color.White) },
                containerColor = Color(0xFF282828),
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showImagePicker = false }) {
                        Text("Cancel", color = Color(0xFF1DB954))
                    }
                },
                text = {
                    Column {
                        TextButton(
                            onClick = {
                                pickImageLauncher.launch("image/*")
                                showImagePicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Choose from Gallery", color = Color.White)
                        }
                        TextButton(
                            onClick = {
                                // Implementation for camera capture would go here
                                // This requires additional setup for camera permissions and file provider
                                showImagePicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Take Photo", color = Color.White)
                        }
                    }
                }
            )
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