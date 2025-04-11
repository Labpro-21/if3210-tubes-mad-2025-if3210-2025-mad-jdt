package com.purrytify.mobile.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purrytify.mobile.R

@Composable
fun ProfileScreen(
    onLogout: () -> Unit // <<< ADDED: Callback function parameter
) {
    var showEditModal by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Main profile content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight() // Make it full height to start from the top
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
                    .padding(top = 60.dp), // Adjusted to start from top
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF64DFDF))
                    )
                    Image(
                        painter = painterResource(id = R.drawable.profile_image),
                        contentDescription = "Your Image",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "13522xxx",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Indonesia",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showEditModal = true }, // Open edit modal on click
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.DarkGray.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(36.dp)
                ) {
                    Text("Edit Profile", color = Color.White)
                }

                Spacer(modifier = Modifier.height(30.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("135", "SONGS")
                    StatItem("32", "LIKED")
                    StatItem("50", "LISTENED")
                }

                // --- ADDED: Logout Button ---
                Spacer(modifier = Modifier.height(30.dp)) // Space before logout
                Button(
                    onClick = onLogout, // Call the passed-in lambda
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red.copy(alpha = 0.6f) // Example color
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(36.dp)
                ) {
                    Text("Log Out", color = Color.White)
                }
                // --- End Logout Button ---

            }
        }

        // Edit Profile Modal
        AnimatedVisibility(
            visible = showEditModal,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize()
        ) {
            EditProfileModal(
                onDismiss = { showEditModal = false },
                onSave = {
                    // Handle save logic here
                    showEditModal = false
                }
            )
        }
    }
}

// --- EditProfileModal (Unchanged from your version) ---
@Composable
fun EditProfileModal(onDismiss: () -> Unit, onSave: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(
                        color = Color(0xFF1A1A1A),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
            )

            // Edit Profile Header Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A))
                    .padding(vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        Text(
                            "Cancel",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }

                    Text(
                        text = "Edit Profile",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(
                        onClick = onSave,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(
                            "Save",
                            color = Color(0xFF1E88E5),
                            fontSize = 16.sp
                        )
                    }
                }
            }


            // Profile editing content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile picture with edit button
                Box(contentAlignment = Alignment.BottomEnd) {
                    // Profile image
                    Image(
                        painter = painterResource(id = R.drawable.profile_image),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    // Edit icon button
                    IconButton(
                        onClick = { /* Handle edit profile picture */ },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_edit),
                            contentDescription = "Edit Picture",
                            tint = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Name field
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Name",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(80.dp)
                    )

                    TextField(
                        value = "13522xxx",
                        onValueChange = { /* Handle name change */ },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color(0xFF1E88E5),
                            unfocusedIndicatorColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Country",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(80.dp)
                    )

                    TextField(
                        value = "Indonesia",
                        onValueChange = { /* Handle name change */ },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color(0xFF1E88E5),
                            unfocusedIndicatorColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// --- StatItem (Unchanged from your version) ---
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
