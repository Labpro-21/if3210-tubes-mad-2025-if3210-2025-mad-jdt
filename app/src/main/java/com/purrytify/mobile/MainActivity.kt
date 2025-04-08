package com.purrytify.mobile

import android.os.Bundle
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
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.purrytify.mobile.ui.BottomNavigationBar
import com.purrytify.mobile.ui.BottomNavItem
import com.purrytify.mobile.ui.HomeScreen
import com.purrytify.mobile.ui.ProfileScreen
import com.purrytify.mobile.ui.YourLibraryScreen
import com.purrytify.mobile.ui.theme.PurrytifyTheme
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.font.FontWeight

// Composition Local for Poppins Font
val LocalPoppinsFont = staticCompositionLocalOf<FontFamily> {
    error("Poppins font family not provided")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PurrytifyTheme {
                val poppinsFontFamily: FontFamily = rememberPoppinsFontFamily() // Explicit type

                CompositionLocalProvider(LocalPoppinsFont provides poppinsFontFamily) {
                    val navController = rememberNavController()
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Black,
                        bottomBar = { BottomNavigationBar(navController = navController) }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = BottomNavItem.Home.route,
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(BottomNavItem.Home.route) { HomeScreen() }
                            composable(BottomNavItem.Library.route) { YourLibraryScreen() }
                            composable(BottomNavItem.Profile.route) { ProfileScreen() }
                        }
                    }
                }
            }
        }
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
