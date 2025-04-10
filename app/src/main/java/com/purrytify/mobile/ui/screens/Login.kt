package com.purrytify.mobile.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purrytify.mobile.LocalPoppinsFont
import com.purrytify.mobile.R

@Composable
fun LoginScreen() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val backgroundColor = Color(0xFF121212)
    val loginbuttonColor = Color(0xFF1DB955)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 500.dp)
                .zIndex(0f)
        ) {
            Image(
                painter = painterResource(id = R.drawable.background_login),
                contentDescription = "Album covers background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(vertical = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(240.dp))

            Box(
                modifier = Modifier
                    .zIndex(1f)
                    .size(130.dp, 130.dp)
                    .background(backgroundColor)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.splash_logo), // Replace with your logo
                    contentDescription = "Purritify Logo",
                    modifier = Modifier
                        .size(130.dp)
                )
            }

            Text(
                text = "Millions of Songs.\nOnly on Purritify.",
                style = TextStyle(
                    fontSize = 24.sp,
                    color = Color.White,
                    fontFamily = LocalPoppinsFont.current,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(vertical = 5.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Email",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color.White,
                        fontFamily = LocalPoppinsFont.current
                    ),
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 8.dp)
                )

                TextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("Email") },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.DarkGray.copy(alpha = 0.3f),
                        focusedContainerColor = Color.DarkGray.copy(alpha = 0.5f),
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        cursorColor = Color.White,
                        unfocusedPlaceholderColor = Color.LightGray,
                        focusedPlaceholderColor = Color.LightGray,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Password",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color.White,
                        fontFamily = LocalPoppinsFont.current
                    ),
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 8.dp)
                )

                TextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Password") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.DarkGray.copy(alpha = 0.3f),
                        focusedContainerColor = Color.DarkGray.copy(alpha = 0.5f),
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        cursorColor = Color.White,
                        unfocusedPlaceholderColor = Color.LightGray,
                        focusedPlaceholderColor = Color.LightGray,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent
                    ),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = Color.LightGray
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { /* Handle login */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = loginbuttonColor,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(50.dp))
            ) {
                Text(
                    text = "Log In",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontFamily = LocalPoppinsFont.current,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}