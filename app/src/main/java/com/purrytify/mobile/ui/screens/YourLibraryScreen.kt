package com.purrytify.mobile.ui.screens

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.purrytify.mobile.LocalPoppinsFont
import com.purrytify.mobile.R
import com.purrytify.mobile.models.Song
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.MediaStore
import android.widget.Toast
import coil.compose.rememberAsyncImagePainter
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YourLibraryScreen() {
    val allSongs = remember {
        mutableStateListOf(
            Song(
                "1",
                "Starboy",
                "The Weeknd, Daft Punk",
                "https://example.com/cover1.jpg",
                "https://example.com/song1.mp3",
                false
            ),
            Song(
                "2",
                "Here Comes The Sun",
                "The Beatles",
                "https://example.com/cover2.jpg",
                "https://example.com/song2.mp3",
                true
            ),
            Song(
                "3",
                "Midnight Pretenders",
                "Tomoko Aran",
                "https://example.com/cover3.jpg",
                "https://example.com/song3.mp3",
                false
            ),
            Song(
                "4",
                "Violent Crimes",
                "Kanye West",
                "https://example.com/cover4.jpg",
                "https://example.com/song4.mp3",
                true
            ),
            Song(
                "5",
                "DENIAL IS A RIVER",
                "Doechii",
                "https://example.com/cover5.jpg",
                "https://example.com/song5.mp3",
                false
            )
        )
    }

    // Remember states for temporary uploads
    var coverImageUri by remember { mutableStateOf<Uri?>(null) }
    var audioFileUri by remember { mutableStateOf<Uri?>(null) }

    // Photo picker launcher
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { coverImageUri = it }
    }

    // Audio file picker launcher
    val pickAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { audioFileUri = it }
    }

    val likedSongs = remember { allSongs.filter { it.isLiked } }
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("All", "Liked")
    val tabIndex = selectedTabIndex
    val tabPositions = remember { mutableStateListOf<TabPosition>() }
    val context = LocalContext.current
    var showBottomSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Header Section
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

            IconButton(onClick = { showBottomSheet = true }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = "Add Song",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Tab Row
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
                    fontSize = 12.sp,
                    modifier = Modifier.align(alignment = Alignment.CenterVertically)
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
        }

        // line
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(color = Color.DarkGray)
        )

        // Content using RecyclerView
        AndroidView(
            factory = { context ->
                RecyclerView(context).apply {
                    layoutManager = LinearLayoutManager(context)
                    clipToPadding = false
                }
            },
            update = { recyclerView ->
                val songsToShow = if (selectedTabIndex == 0) allSongs else likedSongs
                recyclerView.adapter = SongsAdapter(songsToShow) { song ->
                    // Handle song click
                }
            },
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp)
        )
    }

    // Bottom Sheet for Add Song
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                // Clear temporary states when closing
                coverImageUri = null
                audioFileUri = null
                showBottomSheet = false
            },
            containerColor = Color(0xFF121212),
            contentColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) },
            modifier = Modifier.fillMaxHeight(0.7f)
        ) {
            var songTitle by remember { mutableStateOf("") }
            var artistName by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Upload Song",
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

                // Album Cover Upload Section with preview if selected
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
                                        text = "File Selected",
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

                // Rest of form fields...
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

                // Artist Name Input
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

                // Upload Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            coverImageUri = null
                            audioFileUri = null
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
                            fontSize = 14.sp,
                            modifier = Modifier.align(alignment = Alignment.CenterVertically)
                        )
                    }

                    Button(
                        onClick = {
                            // Validate and add new song if input is valid
                            if (songTitle.isNotBlank() && artistName.isNotBlank() && coverImageUri != null && audioFileUri != null) {
                                // Create a new song with the provided details
                                val newSong = Song(
                                    id = (allSongs.size + 1).toString(),
                                    title = songTitle,
                                    artist = artistName,
                                    imageUrl = coverImageUri.toString(),
                                    audioUrl = audioFileUri.toString(),
                                    isLiked = false
                                )

                                // Add to our songs list
                                allSongs.add(newSong)

                                // Show success message
                                Toast.makeText(context, "Song uploaded successfully", Toast.LENGTH_SHORT).show()

                                // Close bottom sheet and reset values
                                showBottomSheet = false
                            } else {
                                // Show error message for missing fields
                                Toast.makeText(context, "Please fill all fields and select files", Toast.LENGTH_SHORT).show()
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
                            text = "Upload",
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddSongOption(
    icon: Int,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = title,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = LocalPoppinsFont.current,
                    color = Color.White
                )
            )

            Text(
                text = description,
                style = TextStyle(
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontFamily = LocalPoppinsFont.current
                )
            )
        }
    }
}

// RecyclerView Adapter for songs
class SongsAdapter(
    private val songs: List<Song>,
    private val onSongClick: (Song) -> Unit
) : RecyclerView.Adapter<SongsAdapter.SongViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view, songs as MutableList<Song>)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.bind(song)
        holder.itemView.setOnClickListener { onSongClick(song) }
    }

    override fun getItemCount() = songs.size

    class SongViewHolder(
        itemView: View,
        private val songs: MutableList<Song>
    ) : RecyclerView.ViewHolder(itemView) {
        private val imageAlbumArt: ImageView = itemView.findViewById(R.id.imageAlbumArt)
        private val textTitle: TextView = itemView.findViewById(R.id.textTitle)
        private val textArtist: TextView = itemView.findViewById(R.id.textArtist)
        private val imageLike: ImageView = itemView.findViewById(R.id.imageLike)

        fun bind(song: Song) {
            textTitle.text = song.title
            textArtist.text = song.artist

            // Load image with Glide
            Glide.with(itemView.context)
                .load(song.imageUrl)
                .placeholder(R.drawable.placeholder_album)
                .into(imageAlbumArt)

            // Set heart icon based on liked status
            imageLike.setImageResource(
                if (song.isLiked) R.drawable.ic_heart_filled
                else R.drawable.ic_heart_outline
            )

            // Add click listener for the like button
            imageLike.setOnClickListener {
                // Toggle like status
                song.isLiked = !song.isLiked

                // Update icon
                imageLike.setImageResource(
                    if (song.isLiked) R.drawable.ic_heart_filled
                    else R.drawable.ic_heart_outline
                )

                // Optional: Show toast for like/unlike
                Toast.makeText(
                    itemView.context,
                    if (song.isLiked) "Added to Liked Songs" else "Removed from Liked Songs",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
