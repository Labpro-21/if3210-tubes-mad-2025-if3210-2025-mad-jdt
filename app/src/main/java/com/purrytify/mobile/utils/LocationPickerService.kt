package com.purrytify.mobile.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

class LocationPickerService(private val context: Context) {
    
    /**
     * Open Google Maps for location selection
     * Note: This opens Google Maps but doesn't return location data directly
     * For production use, consider implementing Google Places API
     */
    fun openGoogleMapsForLocationSelection(
        launcher: ActivityResultLauncher<Intent>
    ) {
        try {
            // Create intent to open Google Maps
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("geo:0,0?q=")
                setPackage("com.google.android.apps.maps")
            }
            
            // Check if Google Maps is installed
            if (intent.resolveActivity(context.packageManager) != null) {
                launcher.launch(intent)
                Log.d("LocationPickerService", "Opened Google Maps for location selection")
            } else {
                // Fallback to web version
                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://maps.google.com/")
                }
                context.startActivity(webIntent)
                Log.d("LocationPickerService", "Opened web version of Google Maps")
            }
        } catch (e: Exception) {
            Log.e("LocationPickerService", "Error opening Google Maps: ${e.message}", e)
        }
    }
    
    /**
     * Open Google Maps with a specific location query
     */
    fun openGoogleMapsWithQuery(
        query: String,
        launcher: ActivityResultLauncher<Intent>
    ) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
                setPackage("com.google.android.apps.maps")
            }
            
            if (intent.resolveActivity(context.packageManager) != null) {
                launcher.launch(intent)
                Log.d("LocationPickerService", "Opened Google Maps with query: $query")
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://maps.google.com/maps?q=${Uri.encode(query)}")
                }
                context.startActivity(webIntent)
                Log.d("LocationPickerService", "Opened web Google Maps with query: $query")
            }
        } catch (e: Exception) {
            Log.e("LocationPickerService", "Error opening Google Maps with query: ${e.message}", e)
        }
    }
    
    /**
     * Validate if a location string is a valid country code (ISO 3166-1 alpha-2)
     */
    fun isValidCountryCode(countryCode: String): Boolean {
        return countryCode.length == 2 && countryCode.all { it.isLetter() }
    }
    
    /**
     * Convert country name to country code (basic implementation)
     * For production, use a proper country code mapping library
     */
    fun getCountryCodeFromName(countryName: String): String? {
        val countryMap = mapOf(
            "indonesia" to "ID",
            "united states" to "US",
            "united kingdom" to "GB",
            "japan" to "JP",
            "singapore" to "SG",
            "malaysia" to "MY",
            "thailand" to "TH",
            "philippines" to "PH",
            "vietnam" to "VN",
            "australia" to "AU",
            "canada" to "CA",
            "germany" to "DE",
            "france" to "FR",
            "italy" to "IT",
            "spain" to "ES",
            "netherlands" to "NL",
            "brazil" to "BR",
            "india" to "IN",
            "china" to "CN",
            "south korea" to "KR"
        )
        
        return countryMap[countryName.lowercase()]
    }
} 