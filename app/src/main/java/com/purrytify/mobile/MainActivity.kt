package com.purrytify.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.purrytify.mobile.ui.theme.PurrytifyTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
                val poppinsFontFamily = rememberPoppinsFontFamily()

                CompositionLocalProvider(LocalPoppinsFont provides poppinsFontFamily) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Black
                    ) { innerPadding ->
                        NewSongsContainer(
                            modifier = Modifier.padding(innerPadding)
                        )
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
                weight = FontWeight.W600
            ),
        )
    }
}


@Composable
fun NewSongsContainer(modifier: Modifier = Modifier) {
    val poppinsFontFamily = LocalPoppinsFont.current

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "New Songs",
            style = TextStyle(
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.W600,
                fontFamily = poppinsFontFamily
            )
        )
    }
}


//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    PurrytifyTheme {
//        NewSongsContainer()
//    }
//}
