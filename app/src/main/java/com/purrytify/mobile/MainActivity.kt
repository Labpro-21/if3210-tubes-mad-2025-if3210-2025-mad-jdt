package com.purrytify.mobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.purrytify.mobile.api.ApiClient
import com.purrytify.mobile.data.AuthRepository
import com.purrytify.mobile.data.TokenManager
import com.purrytify.mobile.data.UserRepository
import com.purrytify.mobile.data.createCountrySongRepository
import com.purrytify.mobile.data.createSongRepository
import com.purrytify.mobile.data.room.AppDatabase
import com.purrytify.mobile.ui.BottomNavItem
import com.purrytify.mobile.ui.BottomNavigationBar
import com.purrytify.mobile.ui.MiniPlayer
import com.purrytify.mobile.ui.initializeMediaPlayer
import com.purrytify.mobile.ui.screens.CountrySong
import com.purrytify.mobile.ui.screens.GlobalSong
import com.purrytify.mobile.ui.screens.HomeScreen
import com.purrytify.mobile.ui.screens.LoginScreen
import com.purrytify.mobile.ui.screens.ProfileScreen
import com.purrytify.mobile.ui.screens.SplashScreen
import com.purrytify.mobile.ui.screens.YourLibraryScreen
import com.purrytify.mobile.ui.theme.PurrytifyTheme
import com.purrytify.mobile.utils.NetworkConnectivityObserver
import com.purrytify.mobile.viewmodel.LocalSongViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Composition Local for Poppins Font
val LocalPoppinsFont = staticCompositionLocalOf<FontFamily> {
    error("Poppins font family not provided")
}

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Log.d("MainActivity", "Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate: MainActivity is starting!")

        // Handle logout intent
        if (intent?.getBooleanExtra("isLogout", false) == true) {
            Log.d("MainActivity", "Received logout intent, clearing tokens")
            // Clear tokens synchronously to ensure they're cleared before UI setup
            TokenManager(applicationContext).clearTokensSync()
        }

        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }

        // --- Dependencies ---
        val tokenManager = TokenManager(applicationContext)

        // Create logout callback that restarts MainActivity with logout flag
        val onLogoutRequired = {
            Log.d(
                "MainActivity",
                "Logout required from interceptor, restarting activity"
            )
            val intent = Intent(this, MainActivity::class.java).apply {
                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("isLogout", true)
            }
            startActivity(intent)
            finish()
        }

        val retrofit = ApiClient.buildRetrofit(
            tokenManager,
            onLogoutRequired
        ) // Pass tokenManager and logout callback
        val authService = ApiClient.createAuthService(retrofit)
        val userService = ApiClient.createUserService(retrofit)
        val authRepository =
            AuthRepository(tokenManager, authService)
        val userRepository = UserRepository(tokenManager, userService)
        val networkConnectivityObserver =
            NetworkConnectivityObserver(applicationContext)
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
                    Log.d(
                        "MainActivity",
                        "Initial start destination: $startDestination"
                    )

                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        // Authentication Flow Graph
                        navigation(startDestination = "splash", route = "auth") {
                            composable("splash") {
                                SplashScreen(
                                    tokenManager = tokenManager,
                                    authRepository = authRepository,
                                    navController = navController
                                )
                            }
                            composable("login") {
                                Log.d(
                                    "MainActivity",
                                    "Navigating to LoginScreen"
                                )
                                LoginScreen(
                                    authRepository = authRepository,
                                    navController = navController
                                )
                            }
                        }

                        // Main Application Flow Graph
                        composable("main") {
                            Log.d(
                                "MainActivity",
                                "Navigating to MainContent (main graph)"
                            )
                            MainContent(
                                navController = navController,
                                authRepository = authRepository,
                                userRepository = userRepository,
                                networkConnectivityObserver = networkConnectivityObserver,
                                tokenManager = tokenManager // Pass tokenManager
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
        val hasToken =
            tokenManager.getAccessToken() != null // Use the blocking version here
        Log.d("MainActivity", "checkInitialAuthState: hasToken = $hasToken")
        return hasToken
    }

    @Composable
    fun rememberPoppinsFontFamily(): FontFamily {
        val provider = GoogleFont.Provider(
            providerAuthority = "com.google.android.gms.fonts",
            providerPackage = "com.google.android.gms",
            certificates = R.array.com_google_android_gms_fonts_certs
        )
        val fontName = GoogleFont("Poppins")

        return remember {
            FontFamily(
                Font(
                    googleFont = fontName,
                    fontProvider = provider,
                    weight = FontWeight.Light
                ),
                Font(
                    googleFont = fontName,
                    fontProvider = provider,
                    weight = FontWeight.Normal
                ),
                Font(
                    googleFont = fontName,
                    fontProvider = provider,
                    weight = FontWeight.Medium
                ),
                Font(
                    googleFont = fontName,
                    fontProvider = provider,
                    weight = FontWeight.SemiBold
                ),
                Font(
                    googleFont = fontName,
                    fontProvider = provider,
                    weight = FontWeight.Bold
                ),
                Font(
                    googleFont = fontName,
                    fontProvider = provider,
                    weight = FontWeight.ExtraBold
                ),
                Font(
                    googleFont = fontName,
                    fontProvider = provider,
                    weight = FontWeight.Black
                )
            )
        }
    }
}

// --- Main Authenticated Content Composable ---
@Composable
fun MainContent(
    navController: NavHostController, // Top-level controller for logout
    authRepository: AuthRepository, // Pass the repository instance
    userRepository: UserRepository,
    networkConnectivityObserver: NetworkConnectivityObserver,
    tokenManager: TokenManager // Added tokenManager parameter
) {
    val nestedNavController = rememberNavController() // Controller for bottom nav sections
    val scope =
        rememberCoroutineScope() // Get a coroutine scope tied to this composable's lifecycle
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val networkStatus = networkConnectivityObserver.observe()
        .collectAsState(initial = NetworkConnectivityObserver.Status.AVAILABLE).value
    val localSongViewModel: LocalSongViewModel = viewModel()

    // Initialize MediaPlayer
    LaunchedEffect(Unit) { initializeMediaPlayer(context) }

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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Black,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                Column {
                    MiniPlayer(
                        onDeleteClick = { song ->
                            localSongViewModel.deleteSong(song)
                        },
                        viewModel = localSongViewModel,
                    )
                    BottomNavigationBar(navController = nestedNavController)
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = nestedNavController,
                startDestination = BottomNavItem.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(BottomNavItem.Home.route) {
                    HomeScreen(navController = nestedNavController)
                }
                composable(BottomNavItem.Library.route) { YourLibraryScreen(/* Pass dependencies */) }
                composable(BottomNavItem.Profile.route) {
                    ProfileScreen(
                        authRepository = authRepository, // Pass the repository instance
                        userRepository = userRepository,
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
                composable("global_song") {
                    val context = LocalContext.current
                    val localSongDao = remember {
                        AppDatabase.getDatabase(context).localSongDao()
                    }
                    val songRepository = remember {
                        createSongRepository(
                            tokenManager = tokenManager,
                            localSongDao = localSongDao,
                            context = context
                        )
                    }
                    GlobalSong(
                        navController = nestedNavController,
                        repository = songRepository
                    )
                }
                composable("country_song") {
                    val countrySongRepository = remember {
                        createCountrySongRepository(tokenManager) // Use passed tokenManager
                    }
                    CountrySong(
                        navController = nestedNavController,
                        repository = countrySongRepository
                    )
                }
            }
        }
    }
}
