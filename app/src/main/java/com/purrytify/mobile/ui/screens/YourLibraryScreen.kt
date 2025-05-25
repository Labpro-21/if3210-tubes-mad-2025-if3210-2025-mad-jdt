package com.purrytify.mobile.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.Glide
import com.purrytify.mobile.LocalPoppinsFont
import com.purrytify.mobile.R
import com.purrytify.mobile.data.room.LocalSong
import com.purrytify.mobile.viewmodel.LocalSongViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import com.purrytify.mobile.ui.MiniPlayerState
import com.purrytify.mobile.ui.playSong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YourLibraryScreen() {
    // Create a MediaPlayer instance and track state
    val context = LocalContext.current

    val localSongViewModel: LocalSongViewModel = viewModel()
    val localSongs by localSongViewModel.allSongs.collectAsState()
    val likedLocalSongs by localSongViewModel.likedSongs.collectAsState()
    val isLoading by localSongViewModel.isLoading.collectAsState()
    val errorMessage by localSongViewModel.errorMessage.collectAsState()

    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    var permissionsGranted by remember {
        mutableStateOf(
            permissionsToRequest.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions.values.all { it }
    }

    LaunchedEffect(Unit) {
        if (!permissionsGranted) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedLocalSong by remember { mutableStateOf<LocalSong?>(null) }

    var coverImageUri by remember { mutableStateOf<Uri?>(null) }
    var audioFileUri by remember { mutableStateOf<Uri?>(null) }
    var songTitle by remember { mutableStateOf("") }
    var artistName by remember { mutableStateOf("") }
    var songDuration by remember { mutableStateOf("--:--") }
    var isEditMode by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableStateOf(0) }

    val scope = rememberCoroutineScope()

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { coverImageUri = it }
    }

    val pickAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            audioFileUri = it
            if (!isEditMode) {
                scope.launch {
                    try {
                        val retriever = MediaMetadataRetriever()
                        retriever.setDataSource(context, uri)

                        val extractedTitle =
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                        if (!extractedTitle.isNullOrBlank()) {
                            songTitle = extractedTitle
                        } else {
                            val fileName = uri.lastPathSegment?.substringAfterLast('/')
                            if (!fileName.isNullOrBlank()) {
                                songTitle = File(fileName).nameWithoutExtension
                            }
                        }

                        val extractedArtist =
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        if (!extractedArtist.isNullOrBlank()) {
                            artistName = extractedArtist
                        } else {
                            artistName = "Unknown Artist"
                        }

                        val durationString =
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        val duration = durationString?.toLongOrNull() ?: 0
                        songDuration = localSongViewModel.formatDuration(duration)

                        retriever.release()
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Error reading metadata: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    val tabs = listOf("All", "Liked", "Downloaded")
    var showBottomSheet by remember { mutableStateOf(false) }
    val downloadedSongs by localSongViewModel.downloadedSongs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Your Library",
                style = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = LocalPoppinsFont.current,
                    color = Color.White
                )
            )

            IconButton(onClick = {
                if (!permissionsGranted) {
                    permissionLauncher.launch(permissionsToRequest)
                    return@IconButton
                }
                isEditMode = false
                songTitle = ""
                artistName = ""
                songDuration = "--:--"
                coverImageUri = null
                audioFileUri = null
                showBottomSheet = true
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = "Add Song",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { selectedTabIndex = 0 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTabIndex == 0) Color(0xFF1DB954) else Color.DarkGray,
                    contentColor = if (selectedTabIndex == 0) Color.Black else Color.White
                ),
                shape = RoundedCornerShape(45.dp),
                modifier = Modifier.size(64.dp, 32.dp)
            ) {
                Text(
                    text = "All",
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = { selectedTabIndex = 1 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTabIndex == 1) Color(0xFF1DB954) else Color.DarkGray,
                    contentColor = if (selectedTabIndex == 1) Color.Black else Color.White
                ),
                shape = RoundedCornerShape(45.dp),
                modifier = Modifier.size(80.dp, 32.dp)
            ) {
                Text(
                    text = "Liked",
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = { selectedTabIndex = 2 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTabIndex == 2) Color(0xFF1DB954) else Color.DarkGray,
                    contentColor = if (selectedTabIndex == 2) Color.Black else Color.White
                ),
                shape = RoundedCornerShape(45.dp),
                modifier = Modifier.size(100.dp, 32.dp)
            ) {
                Text(
                    text = "Downloaded",
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(color = Color.DarkGray)
        )

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

        val songsToShow = when (selectedTabIndex) {
            0 -> localSongs
            1 -> likedLocalSongs
            2 -> downloadedSongs
            else -> emptyList()
        }

        if (songsToShow.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = when (selectedTabIndex) {
                            0 -> "No songs yet"
                            1 -> "No liked songs yet" 
                            2 -> "No downloaded songs yet"
                            else -> ""
                        },
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = LocalPoppinsFont.current,
                            color = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (selectedTabIndex == 0) 
                            "Add your first song by tapping the + icon" 
                        else 
                            "Like songs to see them here",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontFamily = LocalPoppinsFont.current,
                            color = Color.Gray
                        ),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (selectedTabIndex == 0) {
                        Button(
                            onClick = {
                                isEditMode = false
                                songTitle = ""
                                artistName = ""
                                songDuration = "--:--"
                                coverImageUri = null
                                audioFileUri = null
                                showBottomSheet = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1DB954),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(45.dp)
                        ) {
                            Text(
                                text = "Add Song",
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 18.dp, 
                        end = 18.dp, 
                        top = 16.dp,
                        bottom = if (MiniPlayerState.currentSong != null) 80.dp else 16.dp
                    )
            ) {
                itemsIndexed(
                    items = songsToShow,
                    key = { _, song -> song.id }
                ) { index, song ->
                    LocalSongItem(
                        song = song,
                        onPlayClick = { playSong(song, context) },
                        isPlaying = MiniPlayerState.isPlaying && isCurrentSongLocalSongId(song.id),
                        onEditClick = {
                            selectedLocalSong = song
                            // Prefill form fields for editing
                            songTitle = song.title
                            artistName = song.artist
                            songDuration = localSongViewModel.formatDuration(song.duration)
                            coverImageUri = song.artworkPath?.let { Uri.parse(it) }
                            audioFileUri = Uri.parse(song.filePath)
                            isEditMode = true
                            showBottomSheet = true
                        },
                        onDeleteClick = {
                            selectedLocalSong = song
                            showDeleteDialog = true
                        },
                        onLikeToggle = {
                            localSongViewModel.toggleLikeStatus(song)
                        }
                    )

                    if (index < songsToShow.size - 1) {
                        Divider(
                            color = Color.DarkGray,
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }


    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Song") },
            text = { Text("Are you sure you want to delete \"${selectedLocalSong?.title}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        selectedLocalSong?.let {
                            localSongViewModel.deleteSong(it)
                            Toast.makeText(context, "Song deleted", Toast.LENGTH_SHORT).show()
                        }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF282828),
            textContentColor = Color.White,
            titleContentColor = Color.White
        )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                if (!isEditMode) {
                    coverImageUri = null
                    audioFileUri = null
                    songTitle = ""
                    artistName = ""
                    songDuration = "--:--"
                }
                showBottomSheet = false
            },
            containerColor = Color(0xFF121212),
            contentColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) },
            modifier = Modifier.fillMaxHeight(0.7f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = if (isEditMode) "Edit Song" else "Add Song",
                    style = TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalPoppinsFont.current,
                        color = Color.White,
                    ),
                    modifier = Modifier
                        .padding(bottom = 24.dp)
                        .align(Alignment.CenterHorizontally)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(105.dp)
                                .background(Color.DarkGray, RoundedCornerShape(8.dp))
                                .clickable {
                                    pickImageLauncher.launch("image/*")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (coverImageUri != null) {
                                AndroidView(
                                    factory = { ctx ->
                                        ImageView(ctx).apply {
                                            scaleType = ImageView.ScaleType.CENTER_CROP
                                        }
                                    },
                                    update = { imageView ->
                                        Glide.with(imageView)
                                            .load(coverImageUri)
                                            .centerCrop()
                                            .into(imageView)
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_add),
                                    contentDescription = "Upload Cover",
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (coverImageUri != null) "Change Cover" else "Upload Cover",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontFamily = LocalPoppinsFont.current,
                                color = Color.White
                            )
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(105.dp)
                                .background(Color.DarkGray, RoundedCornerShape(8.dp))
                                .clickable {
                                    pickAudioLauncher.launch("audio/*")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_add),
                                contentDescription = "Upload Song File",
                                tint = if (audioFileUri != null) Color(0xFF1DB954) else Color.White,
                                modifier = Modifier.size(48.dp)
                            )

                            if (audioFileUri != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 8.dp)
                                        .background(Color(0x661DB954)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Duration: $songDuration",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (audioFileUri != null) "Change File" else "Upload File",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontFamily = LocalPoppinsFont.current,
                                color = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = songTitle,
                    onValueChange = { songTitle = it },
                    label = { Text("Song Title") },
                    placeholder = { Text("Enter song title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1DB954),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFF1DB954),
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = Color(0xFF1DB954),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = artistName,
                    onValueChange = { artistName = it },
                    label = { Text("Artist Name") },
                    placeholder = { Text("Enter artist name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1DB954),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFF1DB954),
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = Color(0xFF1DB954),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            if (!isEditMode) {
                                coverImageUri = null
                                audioFileUri = null
                            }
                            showBottomSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.DarkGray,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(45.dp),
                        modifier = Modifier.size(152.dp, 40.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (isEditMode) {
                                selectedLocalSong?.let { song ->
                                    if (songTitle.isBlank() || artistName.isBlank() || audioFileUri == null) {
                                        Toast.makeText(
                                            context,
                                            "Please fill all required fields",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@Button
                                    }

                                    val updatedSong = song.copy(
                                        title = songTitle,
                                        artist = artistName,
                                        artworkPath = coverImageUri?.toString()
                                    )

                                    localSongViewModel.updateSong(updatedSong)
                                    Toast.makeText(
                                        context,
                                        "Song updated successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    showBottomSheet = false
                                }
                            } else {
                                if (songTitle.isBlank() || artistName.isBlank() || audioFileUri == null) {
                                    Toast.makeText(
                                        context,
                                        "Please fill all required fields",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }

                                localSongViewModel.addSong(
                                    audioFileUri = audioFileUri!!,
                                    coverImageUri = coverImageUri,
                                    title = songTitle,
                                    artist = artistName
                                )

                                Toast.makeText(context, "Adding song...", Toast.LENGTH_SHORT).show()
                                showBottomSheet = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1DB954),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(45.dp),
                        modifier = Modifier.size(152.dp, 40.dp)
                    ) {
                        Text(
                            text = if (isEditMode) "Save Changes" else "Add Song",
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LocalSongItem(
    song: LocalSong,
    onPlayClick: () -> Unit,
    isPlaying: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onLikeToggle: () -> Unit
) {
    val context = LocalContext.current
    val localSongViewModel: LocalSongViewModel = viewModel()
    val allSongs by localSongViewModel.allSongs.collectAsState()
    var showOptions by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                val currentIndex = allSongs.indexOfFirst { it.id == song.id }
                MiniPlayerState.setQueue(allSongs, currentIndex, "local")
                onPlayClick() 
            }
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
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = song.title,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = LocalPoppinsFont.current,
                        color = Color.White
                    ),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = song.artist,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontFamily = LocalPoppinsFont.current,
                    color = Color.Gray
                ),
                maxLines = 1
            )
            
            Text(
                text = song.duration.toString(),
                style = TextStyle(
                    fontSize = 12.sp,
                    fontFamily = LocalPoppinsFont.current,
                    color = Color.Gray
                )
            )
        }

        Box {
            IconButton(onClick = { showOptions = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More Options",
                    tint = Color.Gray
                )
            }

            DropdownMenu(
                expanded = showOptions,
                onDismissRequest = { showOptions = false },
                modifier = Modifier.background(Color(0xFF282828))
            ) {
                DropdownMenuItem(
                    text = { Text("Edit", color = Color.White) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color.White
                        )
                    },
                    onClick = {
                        showOptions = false
                        onEditClick()
                    }
                )

                DropdownMenuItem(
                    text = { Text("Delete", color = Color.Red) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red
                        )
                    },
                    onClick = {
                        showOptions = false
                        onDeleteClick()
                    }
                )
            }
        }

        IconButton(onClick = {
            val currentIndex = allSongs.indexOfFirst { it.id == song.id }
            MiniPlayerState.setQueue(allSongs, currentIndex, "local")
            onPlayClick()
        }) {
            Icon(
                painter = painterResource(
                    id = if (isPlaying) R.drawable.pause else R.drawable.play_circle
                ),
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color(0xFF1DB954),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

fun isCurrentSongLocalSongId(id: Long): Boolean {
    val song = MiniPlayerState.currentSong
    return song is LocalSong && song.id == id
}
