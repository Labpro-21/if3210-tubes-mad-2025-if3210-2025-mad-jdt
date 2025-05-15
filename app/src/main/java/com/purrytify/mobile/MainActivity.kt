package com.purrytify.mobile

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.lifecycle.lifecycleScope
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
import com.purrytify.mobile.utils.NetworkConnectivityObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
        val tokenManager = TokenManager(applicationContext)
        val retrofit = ApiClient.buildRetrofit()
        val authService = ApiClient.createAuthService(retrofit)
        val authRepository = AuthRepository(tokenManager, authService)
        val networkConnectivityObserver = NetworkConnectivityObserver(applicationContext) // Add this line
        // --- End Dependencies ---

        setContent {
            Log.d("MainActivity", "setContent: Setting up Main UI")
            PurrytifyTheme {
                val poppinsFontFamily: FontFamily = rememberPoppinsFontFamily()
                CompositionLocalProvider(LocalPoppinsFont provides poppinsFontFamily) {

                    val navController = rememberNavController()

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
                                        navController.navigate("login") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("login") {
                                Log.d("MainActivity", "Navigating to LoginScreen")
                                LoginScreen(
                                    authRepository = authRepository,
                                    navController = navController
                                )
                            }
                        }

                        // Main Application Flow Graph
                        composable("main") {
                            Log.d("MainActivity", "Navigating to MainContent (main graph)")
                            MainContent(
                                navController = navController,
                                tokenManager = tokenManager,
                                authRepository = authRepository,
                                networkConnectivityObserver = networkConnectivityObserver // Add this parameter
                            )
                        }
                    }
                }
            }
            Log.d("MainActivity", "onCreate: MainActivity setup finished.")
        }
    }

    // Simple synchronous check using the blocking getAccessToken
    private fun checkInitialAuthState(tokenManager: TokenManager): Boolean {
        val hasToken = tokenManager.getAccessToken() != null // Use the blocking version here
        Log.d("MainActivity", "checkInitialAuthState: hasToken = $hasToken")
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
    tokenManager: TokenManager, // Pass needed dependencies
    authRepository: AuthRepository, // Pass the repository instance
    networkConnectivityObserver: NetworkConnectivityObserver
) {
    val nestedNavController = rememberNavController() // Controller for bottom nav sections
    val scope = rememberCoroutineScope() // Get a coroutine scope tied to this composable's lifecycle
    val snackbarHostState = remember { SnackbarHostState() }
    val networkStatus = networkConnectivityObserver.observe().collectAsState(initial = NetworkConnectivityObserver.Status.AVAILABLE).value

    // Show network status changes
    LaunchedEffect(networkStatus) {
        when (networkStatus) {
            NetworkConnectivityObserver.Status.UNAVAILABLE,
            NetworkConnectivityObserver.Status.LOST -> {
                snackbarHostState.showSnackbar("No network connection")
            }
            NetworkConnectivityObserver.Status.AVAILABLE -> {
                if (snackbarHostState.currentSnackbarData != null) {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    delay(300) // Give time for previous snackbar to dismiss
                    snackbarHostState.showSnackbar("Network connection restored")
                }
            }
            else -> {} // Do nothing for LOSING state
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            BottomNavigationBar(navController = nestedNavController)
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = nestedNavController, // Use nested controller here
                startDestination = BottomNavItem.Home.route
            ) {
                composable(BottomNavItem.Home.route) { HomeScreen(/* Pass dependencies */) }
                composable(BottomNavItem.Library.route) { YourLibraryScreen(/* Pass dependencies */) }
                composable(BottomNavItem.Profile.route) {
                    ProfileScreen(
                        authRepository = authRepository, // Pass the repository instance
                        networkConnectivityObserver = networkConnectivityObserver, // Add this parameter
                        onLogout = {
                            scope.launch { // Use the scope obtained from rememberCoroutineScope()
                                authRepository.logout()
                                navController.navigate("auth") { // Navigate back to auth flow
                                    popUpTo("main") { inclusive = true } // Clear main backstack
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
