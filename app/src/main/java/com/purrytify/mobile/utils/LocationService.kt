package com.purrytify.mobile.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

class LocationService(private val context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    
    private val geocoder = Geocoder(context, Locale.getDefault())
    
    /**
     * Get current location and convert to ISO 3166-1 alpha-2 country code
     * @return Country code (e.g., "US", "ID", "JP") or null if unable to determine
     */
    suspend fun getCurrentCountryCode(): String? {
        return try {
            val location = getCurrentLocation()
            location?.let { getCountryCodeFromLocation(it) }
        } catch (e: Exception) {
            Log.e("LocationService", "Error getting country code: ${e.message}", e)
            null
        }
    }
    
    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || 
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Get current location using FusedLocationProviderClient
     */
    private suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission()) {
            Log.w("LocationService", "Location permission not granted")
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        
        try {
            val cancellationTokenSource = CancellationTokenSource()
            
            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
            
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                Log.d("LocationService", "Location obtained: $location")
                continuation.resume(location)
            }.addOnFailureListener { exception ->
                Log.e("LocationService", "Failed to get location: ${exception.message}", exception)
                continuation.resume(null)
            }
        } catch (e: SecurityException) {
            Log.e("LocationService", "Security exception: ${e.message}", e)
            continuation.resume(null)
        }
    }
    
    /**
     * Convert location coordinates to country code using Geocoder
     */
    private suspend fun getCountryCodeFromLocation(location: Location): String? = 
        suspendCancellableCoroutine { continuation ->
            try {
                if (!Geocoder.isPresent()) {
                    Log.w("LocationService", "Geocoder not available")
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }
                
                // Use the older synchronous method for compatibility with API level 29+
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1
                )
                
                val countryCode = addresses?.firstOrNull()?.countryCode
                Log.d("LocationService", "Country code obtained: $countryCode")
                continuation.resume(countryCode)
                
            } catch (e: Exception) {
                Log.e("LocationService", "Error in geocoding: ${e.message}", e)
                continuation.resume(null)
            }
        }
} 