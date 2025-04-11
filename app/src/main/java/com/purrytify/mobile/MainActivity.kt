package com.purrytify.mobile

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.purrytify.mobile.api.ApiClient
import com.purrytify.mobile.data.AuthRepository
import com.purrytify.mobile.data.TokenManager
import com.purrytify.mobile.ui.BottomNavItem
import com.purrytify.mobile.ui.BottomNavigationBar
import com.purrytify.mobile.ui.screens.HomeScreen
import com.purrytify.mobile.ui.screens.LoginScreen
import com.purrytify.mobile.ui.screens.ProfileScreen
import com.purrytify.mobile.ui.screens.SplashScreen
import com.purrytify.mobile.ui.screens.YourLibraryScreen
import com.purrytify.mobile.ui.theme.PurrytifyTheme

// Composition Local for Poppins Font
val LocalPoppinsFont = staticCompositionLocalOf<FontFamily> {
    error("Poppins font family not provided")
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate: MainActivity is starting!")
        enableEdgeToEdge()

        // --- Dependencies ---
        // Create these once. Consider Dependency Injection (Hilt) for a real app.
        val tokenManager = TokenManager(applicationContext)
        val retrofit = ApiClient.buildRetrofit()
        val authService = ApiClient.createAuthService(retrofit)
        val authRepository = AuthRepository(tokenManager, authService)
        // --- End Dependencies ---

        setContent {
            Log.d("MainActivity", "setContent: Setting up Main UI")
            PurrytifyTheme {
                val poppinsFontFamily: FontFamily = rememberPoppinsFontFamily()
                CompositionLocalProvider(LocalPoppinsFont provides poppinsFontFamily) {

                    val navController = rememberNavController()

                    // Determine initial state (use ViewModel for complex/async checks)
                    val startDestination = remember {
                        if (checkInitialAuthState(tokenManager)) "main" else "auth"
                    }
                    Log.d("MainActivity", "Initial start destination: $startDestination")

                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {

                        // Authentication Flow Graph
                        navigation(startDestination = "splash", route = "auth") {
                            composable("splash") {
                                SplashScreen(
                                    onSplashScreenFinish = {
                                        Log.d(
                                            "MainActivity",
                                            "Splash finished, navigating to login"
                                        )
                                        // Navigate to login, clearing splash
                                        navController.navigate("login") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("login") {
                                Log.d("MainActivity", "Navigating to LoginScreen")
                                // Pass dependencies and navController
                                LoginScreen(
                                    authRepository = authRepository,
                                    navController = navController
                                )
                            }
                        }

                        // Main Application Flow Graph
                        composable("main") {
                            Log.d("MainActivity", "Navigating to MainContent (main graph)")
                            // Pass dependencies and navController for logout
                            MainContent(
                                navController = navController,
                                tokenManager = tokenManager // Example dependency needed for logout
                            )
                        }
                    }
                }
            }
            Log.d("MainActivity", "onCreate: MainActivity setup finished.")
        }
    }

    // Simple synchronous check (replace with ViewModel/async logic if needed)
    private fun checkInitialAuthState(tokenManager: TokenManager): Boolean {
        val hasToken = tokenManager.getAccessToken() != null
        Log.d("MainActivity", "checkInitialAuthState: hasToken = $hasToken")
        // Add more robust validation if required (e.g., check expiry)
        return hasToken
    }

    @Composable
    fun rememberPoppinsFontFamily(): FontFamily {
        val context = LocalContext.current
        val provider = GoogleFont.Provider(
            providerAuthority = "com.google.android.gms.fonts",
            providerPackage = "com.google.android.gms",
            certificates = R.array.com_google_android_gms_fonts_certs
        )
        val fontName = GoogleFont("Poppins")

        return remember {
            FontFamily(
                Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Light),
                Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Normal),
                Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Medium),
                Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.SemiBold),
                Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Bold),
                Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.ExtraBold),
                Font(googleFont = fontName, fontProvider = provider, weight = FontWeight.Black)
            )
        }
    }
}

// --- Main Authenticated Content Composable ---
@Composable
fun MainContent(
    navController: NavHostController, // Top-level controller for logout
    tokenManager: TokenManager // Pass needed dependencies
    // Add other dependencies like AuthRepository if screens need them
) {
    val nestedNavController = rememberNavController() // Controller for bottom nav sections

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black, // Or your theme background
        bottomBar = {
            BottomNavigationBar(navController = nestedNavController)
        }
    ) { innerPadding ->
        NavHost(
            navController = nestedNavController, // Use nested controller here
            startDestination = BottomNavItem.Home.route, // Default screen after login
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) { HomeScreen(/* Pass dependencies */) }
            composable(BottomNavItem.Library.route) { YourLibraryScreen(/* Pass dependencies */) }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen()
            }
        }
    }
}
