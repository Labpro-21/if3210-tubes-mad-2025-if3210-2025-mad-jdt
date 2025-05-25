package com.purrytify.mobile.ui

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.google.zxing.integration.android.IntentIntegrator

@Composable
fun ScanQrScreen(navController: NavHostController) {
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val intent = result.data
        val contents = IntentIntegrator.parseActivityResult(result.resultCode, intent)?.contents
        if (contents != null && contents.startsWith("purrytify://song/")) {
            val songId = contents.removePrefix("purrytify://song/").toIntOrNull()
            if (songId != null) {
                navController.navigate("global_song_player/$songId")
            } else {
                errorMessage = "QR tidak valid"
            }
        } else {
            errorMessage = "QR tidak valid"
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = {
            val integrator = IntentIntegrator(context as Activity)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            integrator.setPrompt("Scan QR lagu Purrytify")
            launcher.launch(integrator.createScanIntent())
        }) {
            Text("Scan QR Lagu")
        }
        errorMessage?.let {
            Text(it, color = Color.Red)
        }
    }
}
