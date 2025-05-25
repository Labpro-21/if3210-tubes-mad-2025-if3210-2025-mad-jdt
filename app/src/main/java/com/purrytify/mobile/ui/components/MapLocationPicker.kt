package com.purrytify.mobile.ui.components

import android.content.Context
import android.location.Geocoder
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@Composable
fun MapLocationPicker(
        onLocationSelected: (String) -> Unit,
        onDismiss: () -> Unit,
        modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var selectedCountryCode by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val isValidSelection =
            selectedCountryCode?.let { code ->
                code.isNotBlank() && code.length == 2 && code.all { it.isLetter() }
            }
                    ?: false

    val isCheckEnabled = isValidSelection && !isLoading

    LaunchedEffect(Unit) {
        Configuration.getInstance()
                .load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = "Purrytify"
    }

    Card(
            modifier = modifier.fillMaxSize().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF282828))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = "Select Location",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                )

                Row {
                    IconButton(
                            onClick = {
                                if (isCheckEnabled) {
                                    selectedCountryCode?.let { countryCode ->
                                        Log.d(
                                                "MapLocationPicker",
                                                "Confirming selection: $countryCode"
                                        )
                                        onLocationSelected(countryCode)
                                    }
                                }
                            },
                            enabled = isCheckEnabled
                    ) {
                        Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription =
                                        if (isCheckEnabled) "Confirm Selection"
                                        else "Select a location first",
                                tint = if (isCheckEnabled) Color(0xFF1DB954) else Color.Gray
                        )
                    }

                    IconButton(
                            onClick = {
                                Log.d("MapLocationPicker", "Dismissing location picker")
                                onDismiss()
                            }
                    ) {
                        Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                        )
                    }
                }
            }

            if (isValidSelection) {
                selectedCountryCode?.let { countryCode ->
                    Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            colors =
                                    CardDefaults.cardColors(
                                            containerColor = Color(0xFF1DB954).copy(alpha = 0.1f)
                                    )
                    ) {
                        Text(
                                text = "Selected: $countryCode",
                                color = Color(0xFF1DB954),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (isLoading) {
                Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                            color = Color(0xFF1DB954),
                            modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Getting location info...", color = Color.Gray, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (selectedLocation != null && !isLoading && !isValidSelection) {
                Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = Color.Red.copy(alpha = 0.1f)
                                )
                ) {
                    Text(
                            text =
                                    "Unable to determine country for this location. Please try another location.",
                            color = Color.Red.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                    text = "Tap on the map to select your location",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)

                            val mapController: IMapController = controller
                            mapController.setZoom(5.0)
                            val startPoint = GeoPoint(-6.2088, 106.8456) // Jakarta
                            mapController.setCenter(startPoint)

                            val mapEventsReceiver =
                                    object : MapEventsReceiver {
                                        override fun singleTapConfirmedHelper(
                                                p: GeoPoint?
                                        ): Boolean {
                                            p?.let { geoPoint ->
                                                Log.d(
                                                        "MapLocationPicker",
                                                        "Location tapped: ${geoPoint.latitude}, ${geoPoint.longitude}"
                                                )
                                                selectedLocation = geoPoint

                                                overlays.removeAll { it is Marker }

                                                val marker = Marker(this@apply)
                                                marker.position = geoPoint
                                                marker.setAnchor(
                                                        Marker.ANCHOR_CENTER,
                                                        Marker.ANCHOR_BOTTOM
                                                )
                                                marker.title = "Selected Location"
                                                overlays.add(marker)

                                                selectedCountryCode = null

                                                coroutineScope.launch {
                                                    isLoading = true
                                                    try {
                                                        val countryCode =
                                                                getCountryCodeFromCoordinates(
                                                                        ctx,
                                                                        geoPoint
                                                                )
                                                        Log.d(
                                                                "MapLocationPicker",
                                                                "Country code retrieved: $countryCode"
                                                        )
                                                        selectedCountryCode = countryCode
                                                    } catch (e: Exception) {
                                                        Log.e(
                                                                "MapLocationPicker",
                                                                "Error getting country code",
                                                                e
                                                        )
                                                        selectedCountryCode = null
                                                    } finally {
                                                        isLoading = false
                                                    }
                                                }

                                                invalidate()
                                            }
                                            return true
                                        }

                                        override fun longPressHelper(p: GeoPoint?): Boolean {
                                            return false
                                        }
                                    }

                            val mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)
                            overlays.add(0, mapEventsOverlay)
                        }
                    },
                    modifier =
                            Modifier.fillMaxWidth()
                                    .weight(1f)
                                    .padding(16.dp)
                                    .background(
                                            Color.Gray.copy(alpha = 0.1f),
                                            RoundedCornerShape(8.dp)
                                    )
            )
        }
    }
}

private suspend fun getCountryCodeFromCoordinates(context: Context, geoPoint: GeoPoint): String? =
        withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())

                if (!Geocoder.isPresent()) {
                    Log.e("MapLocationPicker", "Geocoder is not available on this device")
                    return@withContext null
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {}

                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1)

                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val countryCode = address.countryCode

                    if (countryCode != null &&
                                    countryCode.length == 2 &&
                                    countryCode.all { it.isLetter() }
                    ) {
                        Log.d(
                                "MapLocationPicker",
                                "Valid country code found: $countryCode for coordinates: ${geoPoint.latitude}, ${geoPoint.longitude}"
                        )
                        return@withContext countryCode.uppercase()
                    } else {
                        Log.w("MapLocationPicker", "Invalid country code format: $countryCode")
                        return@withContext null
                    }
                }

                Log.w(
                        "MapLocationPicker",
                        "No address found for coordinates: ${geoPoint.latitude}, ${geoPoint.longitude}"
                )
                return@withContext null
            } catch (e: Exception) {
                Log.e("MapLocationPicker", "Error getting country code from coordinates", e)
                return@withContext null
            }
        }
