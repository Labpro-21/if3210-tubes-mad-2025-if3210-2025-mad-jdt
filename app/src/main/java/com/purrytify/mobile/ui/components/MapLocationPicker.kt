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
import java.util.*

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
    
    // Initialize OSMDroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = "Purrytify"
    }

    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF282828))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                            selectedCountryCode?.let { countryCode ->
                                onLocationSelected(countryCode)
                            }
                        },
                        enabled = selectedCountryCode != null && !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirm",
                            tint = if (selectedCountryCode != null && !isLoading) Color(0xFF1DB954) else Color.Gray
                        )
                    }
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
            }
            
            // Selected location info
            selectedCountryCode?.let { countryCode ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1DB954).copy(alpha = 0.1f))
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
            
            // Loading indicator
            if (isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF1DB954),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Getting location info...",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Instructions
            Text(
                text = "Tap on the map to select your location",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Map
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        
                        // Set initial position (Jakarta, Indonesia as default)
                        val mapController: IMapController = controller
                        mapController.setZoom(5.0)
                        val startPoint = GeoPoint(-6.2088, 106.8456) // Jakarta
                        mapController.setCenter(startPoint)
                        
                        // Add map events overlay for tap handling
                        val mapEventsReceiver = object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                p?.let { geoPoint ->
                                    selectedLocation = geoPoint
                                    
                                    // Clear existing markers
                                    overlays.removeAll { it is Marker }
                                    
                                    // Add new marker
                                    val marker = Marker(this@apply)
                                    marker.position = geoPoint
                                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    marker.title = "Selected Location"
                                    overlays.add(marker)
                                    
                                    // Get country code from coordinates
                                    coroutineScope.launch {
                                        isLoading = true
                                        try {
                                            val countryCode = getCountryCodeFromCoordinates(ctx, geoPoint)
                                            selectedCountryCode = countryCode
                                        } catch (e: Exception) {
                                            Log.e("MapLocationPicker", "Error getting country code", e)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
                    .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            )
        }
    }
}

private suspend fun getCountryCodeFromCoordinates(
    context: Context,
    geoPoint: GeoPoint
): String? = withContext(Dispatchers.IO) {
    try {
        val geocoder = Geocoder(context, Locale.getDefault())
        
        // Use the newer API if available (API 33+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // For newer API, we would use the callback-based approach
            // But for simplicity, we'll stick with the older approach for now
        }
        
        @Suppress("DEPRECATION")
        val addresses = geocoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1)
        
        if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            val countryCode = address.countryCode
            Log.d("MapLocationPicker", "Country code found: $countryCode for coordinates: ${geoPoint.latitude}, ${geoPoint.longitude}")
            return@withContext countryCode
        }
        
        Log.w("MapLocationPicker", "No address found for coordinates: ${geoPoint.latitude}, ${geoPoint.longitude}")
        return@withContext null
    } catch (e: Exception) {
        Log.e("MapLocationPicker", "Error getting country code from coordinates", e)
        return@withContext null
    }
} 