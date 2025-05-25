package com.purrytify.mobile.ui.screens

import android.net.Uri
import android.util.Log
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
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
import com.purrytify.mobile.data.UserRepository
import com.purrytify.mobile.ui.components.MapLocationPicker
import com.purrytify.mobile.utils.LocationService
import com.purrytify.mobile.utils.NetworkConnectivityObserver
import com.purrytify.mobile.viewmodel.EditProfileState
import com.purrytify.mobile.viewmodel.ProfileUiState
import com.purrytify.mobile.viewmodel.ProfileViewModel
import com.purrytify.mobile.viewmodel.ProfileViewModelFactory
import com.purrytify.mobile.viewmodel.SoundCapsuleViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authRepository: AuthRepository,
    userRepository: UserRepository,
    networkConnectivityObserver: NetworkConnectivityObserver,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val locationService = remember { LocationService(context) }
    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(
            authRepository,
            userRepository,
            networkConnectivityObserver,
            locationService
        )
    )

    val profileState by profileViewModel.profileUiState.collectAsState()
    val editProfileState by profileViewModel.editProfileState.collectAsState()
    var showEditSheet by remember { mutableStateOf(false) }
    var showImagePicker by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var currentLocation by remember { mutableStateOf<String?>(null) }
    var showLocationPermissionDialog by remember { mutableStateOf(false) }
    var showMapLocationPicker by remember { mutableStateOf(false) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }


    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted =
            permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted =
            permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            // Permission granted, proceed with location detection
            selectedImageUri?.let { uri ->
                val file = File(context.cacheDir, "profile_photo")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                profileViewModel.editProfileWithAutoLocation(file)
            } ?: run {
                profileViewModel.editProfileWithAutoLocation(null)
            }
        } else {
            Toast.makeText(
                context,
                "Location permission is required to detect your country",
                Toast.LENGTH_LONG
            ).show()
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
                            .padding(top = 60.dp, start = 16.dp, end = 16.dp)
                            .verticalScroll(rememberScrollState()),
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

                                // Sound Capsule Section
                                SoundCapsuleSection()

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
                containerColor = Color(0xFF282828),
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

                    // Location options
                    Column {
                        Text(
                            text = "Location Settings",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Current location display
                        currentLocation?.let {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Color(0xFF1DB954).copy(alpha = 0.1f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Current Location",
                                    tint = Color(0xFF1DB954),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Current: $it",
                                    color = Color(0xFF1DB954),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Auto-detect option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(0xFF404040),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    if (profileViewModel.hasLocationPermission()) {
                                        selectedImageUri?.let { uri ->
                                            val file = File(context.cacheDir, "profile_photo")
                                            context.contentResolver.openInputStream(uri)
                                                ?.use { input ->
                                                    file.outputStream().use { output ->
                                                        input.copyTo(output)
                                                    }
                                                }
                                            profileViewModel.editProfileWithAutoLocation(file)
                                        } ?: run {
                                            profileViewModel.editProfileWithAutoLocation(null)
                                        }
                                    } else {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Auto-detect Location",
                                tint = Color(0xFF1DB954),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Auto-detect Location",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Automatically detect your country",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Embedded Map option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(0xFF404040),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    showMapLocationPicker = true
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Select on Map",
                                tint = Color(0xFF1DB954),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Select on Map",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Choose location from interactive map",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

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
                                // Save only profile photo if changed, location is handled separately
                                selectedImageUri?.let { uri ->
                                    val file = File(context.cacheDir, "profile_photo")
                                    context.contentResolver.openInputStream(uri)?.use { input ->
                                        file.outputStream().use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    profileViewModel.editProfile(null, file)
                                } ?: run {
                                    // No changes to save
                                    showEditSheet = false
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
                                Text("Save Photo")
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

        if (showLocationPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showLocationPermissionDialog = false },
                title = { Text("Location Permission Required", color = Color.White) },
                containerColor = Color(0xFF282828),
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLocationPermissionDialog = false
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    ) {
                        Text("Grant Permission", color = Color(0xFF1DB954))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLocationPermissionDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                },
                text = {
                    Text(
                        "This app needs location permission to automatically detect your country code (ISO 3166-1 alpha-2 format) for your profile.",
                        color = Color.White
                    )
                }
            )
        }

        if (showMapLocationPicker) {
            MapLocationPicker(
                onLocationSelected = { countryCode ->
                    // Save the selected location
                    selectedImageUri?.let { uri ->
                        val file = File(context.cacheDir, "profile_photo")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            file.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        profileViewModel.editProfile(countryCode, file)
                    } ?: run {
                        profileViewModel.editProfile(countryCode, null)
                    }

                    showMapLocationPicker = false
                },
                onDismiss = {
                    showMapLocationPicker = false
                }
            )
        }
    }
}

@Composable
fun SoundCapsuleSection() {
    val soundCapsuleViewModel: SoundCapsuleViewModel = viewModel()
    val monthlyData by soundCapsuleViewModel.monthlyData.collectAsState()
    val isLoading by soundCapsuleViewModel.isLoading.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh data every time this composable is displayed
    LaunchedEffect(Unit) {
        soundCapsuleViewModel.refreshData()
    }

    // Also refresh when the screen becomes visible (lifecycle aware)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                soundCapsuleViewModel.refreshData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Your Sound Capsule",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { soundCapsuleViewModel.refreshData() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF1DB954))
            }
        } else if (monthlyData.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No listening data yet",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Start playing some music to see your stats!",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            monthlyData.forEachIndexed { index, data ->
                SoundCapsuleMonthSection(
                    month = data.month,
                    minutes = data.formattedTime,
                    topArtist = data.topArtist ?: "No data",
                    topArtistImageUrl = data.topArtistImageUrl,
                    topSong = data.topSong ?: "No data",
                    topSongImageUrl = data.topSongImageUrl,
                    achievementText = data.achievementText,
                    achievementSubtext = data.achievementSubtext,
                    achievementDate = data.achievementDate
                )

                if (index < monthlyData.size - 1) {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun SoundCapsuleMonthSection(
    month: String,
    minutes: String,
    topArtist: String,
    topArtistImageUrl: String?,
    topSong: String,
    topSongImageUrl: String?,
    achievementText: String?,
    achievementSubtext: String?,
    achievementDate: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFF1A1A1A),
                RoundedCornerShape(16.dp)
            )
            .padding(20.dp)
    ) {
        // Month header with share icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = month,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Share",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Time listened - prominent display
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Time listened",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "View Details",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = minutes,
                color = Color(0xFF1DB954),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Top artist and song cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top artist card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        Color(0xFF2A2A2A),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Top artist",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "View Artist Details",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Artist image - larger and centered
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(topArtistImageUrl?.let { 
                                if (it.startsWith("http")) it else Uri.parse(it)
                            } ?: R.drawable.profile_image)
                            .error(R.drawable.profile_image)
                            .placeholder(R.drawable.profile_image)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Artist Image",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.Gray),
                        contentScale = ContentScale.Crop
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = topArtist,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                }
            }

            // Top song card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        Color(0xFF2A2A2A),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Top song",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "View Song Details",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Log.d("topSongImageUrl", topSongImageUrl.toString())
                    // Song image - larger and centered
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(topSongImageUrl?.let { 
                                if (it.startsWith("http")) it else Uri.parse(it)
                            } ?: R.drawable.profile_image)
                            .error(R.drawable.profile_image)
                            .placeholder(R.drawable.profile_image)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Song Image",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Gray),
                        contentScale = ContentScale.Crop
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = topSong,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )
                }
            }
        }

        // Achievement card (only for April)
        achievementText?.let {
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF8B5CF6),
                                Color(0xFF06B6D4)
                            )
                        ),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = achievementText,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    achievementSubtext?.let { subtext ->
                        Text(
                            text = subtext,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    achievementDate?.let { date ->
                        Text(
                            text = date,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
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