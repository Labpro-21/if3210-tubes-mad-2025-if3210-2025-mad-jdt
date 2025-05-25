package com.purrytify.mobile.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.util.Log
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
fun ScanQrScreen(
    navController: NavHostController,
    onQrResult: (Int, String?) -> Unit // Callback untuk QR result
) {
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            Log.d("ScanQR", "Launcher result received")
            val intent = result.data
            val contents = IntentIntegrator.parseActivityResult(result.resultCode, intent)?.contents
            Log.d("ScanQR", "Scanned contents: $contents")
            
            if (contents != null && contents.startsWith("purrytify://song/")) {
                Log.d("ScanQR", "Valid QR detected")
                
                val songIdString = contents.removePrefix("purrytify://song/").split("?")[0]
                val songId = songIdString.toIntOrNull()
                val type = if (contents.contains("type=country")) "country" else null
                
                Log.d("ScanQR", "Parsed songId: $songId, type: $type")
                
                if (songId != null) {
                    Log.d("ScanQR", "Calling onQrResult callback")
                    onQrResult(songId, type)
                } else {
                    errorMessage = "QR tidak valid"
                }
            } else {
                errorMessage = "QR tidak valid atau tidak dikenali"
            }
        } catch (e: Exception) {
            Log.e("ScanQR", "Error processing scan result: ${e.message}")
            errorMessage = "Error: ${e.message}"
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = {
            val integrator = IntentIntegrator(context as Activity)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            integrator.setPrompt("Scan QR lagu Purrytify")
            integrator.setOrientationLocked(true)
            launcher.launch(integrator.createScanIntent())
        }) {
            Text("Scan QR Lagu")
        }
        errorMessage?.let {
            Text(it, color = Color.Red)
        }
    }
}
