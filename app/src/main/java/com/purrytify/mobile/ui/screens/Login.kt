package com.purrytify.mobile.ui.screens

import android.util.Log
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.purrytify.mobile.LocalPoppinsFont
import com.purrytify.mobile.R
import com.purrytify.mobile.data.AuthRepository
import com.purrytify.mobile.viewmodel.LoginUiState // Import from viewmodel package
import com.purrytify.mobile.viewmodel.LoginViewModel // Import from viewmodel package
import com.purrytify.mobile.viewmodel.LoginViewModelFactory // Import from viewmodel package

@Composable
fun LoginScreen(
    authRepository: AuthRepository, // Pass repository for factory
    navController: NavHostController // Pass NavController for navigation
) {
    Log.d("LoginScreen", "LoginScreen Composable is rendered")

    // Use viewModel() with the factory to get a stable ViewModel instance
    val loginViewModel: LoginViewModel = viewModel(
        factory = LoginViewModelFactory(authRepository)
    )

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val backgroundColor = Color(0xFF121212)
    val loginbuttonColor = Color(0xFF1DB955)

    // Observe the state from the stable ViewModel instance
    val loginUiState by loginViewModel.loginUiState.collectAsState()

    // Navigation effect
    LaunchedEffect(key1 = loginUiState) {
        Log.d("LoginScreen", "LaunchedEffect running. loginUiState: $loginUiState")
        if (loginUiState is LoginUiState.Success) {
            val successState = loginUiState as LoginUiState.Success
            if (successState.isLoggedIn) {
                Log.d("LoginScreen", "Login Success, navigating to main graph")
                // Navigate to main graph, clearing the auth stack
                navController.navigate("main") {
                    popUpTo("auth") { inclusive = true }
                    launchSingleTop = true
                }
                // Optional: Reset ViewModel state after successful navigation
                // loginViewModel.resetState()
            } else {
                // Handle case where Success is false (if possible in your logic)
                Log.w("LoginScreen", "LoginUiState is Success(false), login failed but no exception. Not navigating.")
            }
        }
    }

    // Reset state when LoginScreen leaves composition (optional but good practice)
    DisposableEffect(Unit) {
        onDispose {
            // loginViewModel.resetState() // Uncomment if you want state reset on leaving
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Background Image Box
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 500.dp) // Adjust as needed

        ) {
            Image(
                painter = painterResource(id = R.drawable.background_login),
                contentDescription = "Album covers background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Main Content Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(vertical = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom // Arrange from bottom up
        ) {
            // Logo and Title Section
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(100.dp)) // Adjust spacing

                Box(
                    modifier = Modifier
                        .size(130.dp, 130.dp)
                        .background(backgroundColor)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.splash_logo),
                        contentDescription = "Purritify Logo",
                        modifier = Modifier.size(130.dp)
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
                    modifier = Modifier.padding(vertical = 15.dp)
                )
            }


            // Input Fields Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
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
                    placeholder = { Text("Enter your email") },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.DarkGray.copy(alpha = 0.3f),
                        focusedContainerColor = Color.DarkGray.copy(alpha = 0.5f),
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        cursorColor = Color.White,
                        unfocusedPlaceholderColor = Color.LightGray,
                        focusedPlaceholderColor = Color.LightGray,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = LocalPoppinsFont.current)
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
                    placeholder = { Text("Enter your password") },
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
                        focusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent
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
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = LocalPoppinsFont.current)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login Button and Status Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Display Loading or Error Message
                Box(modifier = Modifier.height(40.dp), contentAlignment = Alignment.Center) {
                    when (val uiState = loginUiState) {
                        is LoginUiState.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = loginbuttonColor,
                                strokeWidth = 3.dp
                            )
                        }
                        is LoginUiState.Error -> {
                            Text(
                                text = uiState.message,
                                color = Color.Red,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        // Initial and Success states don't need specific UI here
                        else -> {}
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        Log.d("LoginScreen", "Login button clicked")
                        if (email.isNotBlank() && password.isNotBlank()) {
                            loginViewModel.login(email, password)
                        } else {
                            Log.w("LoginScreen", "Email or password is blank")
                            // Optionally show a snackbar or message
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = loginbuttonColor,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(50.dp)),
                    enabled = loginUiState !is LoginUiState.Loading // Disable while loading
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
            Spacer(modifier = Modifier.height(50.dp)) // Bottom padding
        }
    }
}
