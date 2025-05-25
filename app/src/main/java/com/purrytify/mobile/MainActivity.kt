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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.purrytify.mobile.data.SongRepository
import com.purrytify.mobile.data.CountrySongRepository
import com.purrytify.mobile.ui.ScanQrScreen
import com.purrytify.mobile.ui.playSong
import com.purrytify.mobile.data.room.TopSong
import com.purrytify.mobile.data.room.CountrySong


// Composition Local for Poppins Font
val LocalPoppinsFont = staticCompositionLocalOf<FontFamily> {
    error("Poppins font family not provided")
}

class MainActivity : ComponentActivity() {

    private var navController: NavHostController? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Log.d("MainActivity", "Notification permission denied")
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)
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
                    this@MainActivity.navController = navController

                    LaunchedEffect(Unit) {
                        val data = intent?.data
                        if (data != null && data.scheme == "purrytify" && data.host == "song") {
                            val songId = data.lastPathSegment?.toIntOrNull()
                            val type = data.getQueryParameter("type")
                            if (songId != null) {
                                if (type == "country") {
                                    navController.navigate("country_song_player/$songId")
                                } else {
                                    navController.navigate("global_song_player/$songId")
                                }
                            }
                        }
                    }

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

                        composable(
                            route = "global_song_player/{songId}",
                            arguments = listOf(navArgument("songId") { type = NavType.IntType })
                        ) { navBackStackEntry ->
                            val songId = navBackStackEntry.arguments?.getInt("songId")
                            if (songId != null) {
                                val songRepository = remember { createSongRepository(tokenManager) }
                                val context = LocalContext.current
                                val song by produceState<TopSong?>(initialValue = null, songId) {
                                    Log.d("MiniPlayer", "produceState: fetching songId=$songId")
                                    value = songRepository.getSongById(songId)
                                    Log.d("MiniPlayer", "produceState: result=${value?.title}")
                                }

                                LaunchedEffect(song) {
                                    if (song != null) {
                                        Log.d("MiniPlayer", "LaunchedEffect song type: ${song!!::class.simpleName}")
                                        playSong(song!!, context)
                                        navController.navigate("main") {
                                            popUpTo("global_song_player/{songId}") { inclusive = true }
                                        }
                                    }
                                }

                                if (song != null) {
                                    GlobalSongPlayer(
                                        navController = navController,
                                        repository = songRepository,
                                        songId = songId
                                    )
                                } else {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }

                        composable(
                            route = "country_song_player/{songId}",
                            arguments = listOf(navArgument("songId") { type = NavType.IntType })
                        ) { navBackStackEntry ->
                            val songId = navBackStackEntry.arguments?.getInt("songId")
                            if (songId != null) {
                                val countrySongRepository = remember { createCountrySongRepository(tokenManager) }
                                val context = LocalContext.current
                                val song by produceState<CountrySong?>(initialValue = null, songId) {
                                    Log.d("MiniPlayer", "produceState: fetching songId=$songId")
                                    value = countrySongRepository.getCountrySongById(songId)
                                    Log.d("MiniPlayer", "produceState: result=${value?.title}")
                                }
                        
                                LaunchedEffect(song) {
                                    if (song != null) {
                                        Log.d("MiniPlayer", "LaunchedEffect song type: ${song!!::class.simpleName}")
                                        playSong(song!!, context)
                                        navController.navigate("main") {
                                            popUpTo("country_song_player/{songId}") { inclusive = true }
                                        }
                                    }
                                }

                                if (song != null) {
                                    CountrySongPlayer(
                                        navController = navController,
                                        repository = countrySongRepository,
                                        songId = songId
                                    )
                                } else {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
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

    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data
        if (data != null && data.scheme == "purrytify" && data.host == "song") {
            val songId = data.lastPathSegment?.toIntOrNull()
            val type = data.getQueryParameter("type") // misal: purrytify://song/123?type=country
            if (songId != null) {
                if (type == "country") {
                    navController?.navigate("country_song_player/$songId")
                } else {
                    navController?.navigate("global_song_player/$songId")
                }
            }
        }
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
    navController: NavHostController,
    authRepository: AuthRepository,
    userRepository: UserRepository,
    networkConnectivityObserver: NetworkConnectivityObserver,
    tokenManager: TokenManager
) {
    val nestedNavController = rememberNavController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val networkStatus = networkConnectivityObserver.observe()
        .collectAsState(initial = NetworkConnectivityObserver.Status.AVAILABLE).value
    val localSongViewModel: LocalSongViewModel = viewModel()

    // Callback untuk handle QR result
    val handleQrResult: (Int, String?) -> Unit = { songId, type ->
        Log.d("MainActivity", "handleQrResult called with songId: $songId, type: $type")
        if (type == "country") {
            navController.navigate("country_song_player/$songId")
        } else {
            navController.navigate("global_song_player/$songId")
        }
    }

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
                    delay(300)
                    snackbarHostState.showSnackbar("Network connection restored")
                }
            }
            else -> {}
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
                    HomeScreen(
                        navController = navController, // Pass main navController
                        nestedNavController = nestedNavController,
                        onQrResult = handleQrResult // Pass callback
                    )
                }
                composable(BottomNavItem.Library.route) { 
                    YourLibraryScreen(/* Pass dependencies */) 
                }
                composable(BottomNavItem.Profile.route) {
                    ProfileScreen(
                        authRepository = authRepository,
                        userRepository = userRepository,
                        networkConnectivityObserver = networkConnectivityObserver,
                        onLogout = {
                            scope.launch {
                                authRepository.logout()
                                navController.navigate("auth") {
                                    popUpTo("main") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
                composable("global_song") {
                    val songRepository = remember {
                        createSongRepository(tokenManager)
                    }
                    GlobalSong(
                        navController = nestedNavController,
                        repository = songRepository
                    )
                }
                composable("country_song") {
                    val countrySongRepository = remember {
                        createCountrySongRepository(tokenManager)
                    }
                    CountrySong(
                        navController = nestedNavController,
                        repository = countrySongRepository
                    )
                }
                composable("scan_qr") {
                    ScanQrScreen(
                        navController = navController, // Pass main navController
                        onQrResult = handleQrResult // Pass callback
                    )
                }
            }
        }
    }
}

@Composable
fun GlobalSongPlayer(
    navController: NavHostController,
    repository: SongRepository,
    songId: Int
) {
    val context = LocalContext.current
    val song by produceState<TopSong?>(initialValue = null, songId) {
        value = repository.getSongById(songId)
    }

    LaunchedEffect(song) {
        if (song != null) {
            Log.d("MiniPlayer", "LaunchedEffect song type: ${song!!::class.simpleName}")
            playSong(song!!, context)
        }
    }

    // ...UI player untuk TopSong...
}

@Composable
fun CountrySongPlayer(
    navController: NavHostController,
    repository: CountrySongRepository,
    songId: Int
) {
    val context = LocalContext.current
    val song by produceState<CountrySong?>(initialValue = null, songId) {
        value = repository.getCountrySongById(songId)
    }

    LaunchedEffect(song) {
        if (song != null) {
            Log.d("MiniPlayer", "LaunchedEffect song type: ${song!!::class.simpleName}")
            playSong(song!!, context)
        }
    }

    // ...UI player untuk CountrySong (bisa sama dengan GlobalSongPlayer)...
}
