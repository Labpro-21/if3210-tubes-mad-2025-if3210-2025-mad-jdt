package com.purrytify.mobile.utils

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log

data class OutputDevice(
    val id: Int,
    val name: String,
    val type: Int,
    val isActive: Boolean
)

fun getAvailableOutputDevices(context: Context, activeDeviceId: Int? = null): List<OutputDevice> {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
    Log.d("AudioDeviceHelper", "Detected devices:")
    devices.forEach {
        Log.d(
            "AudioDeviceHelper",
            "id=${it.id}, type=${it.type}, name='${it.productName}', isActive=${it.id == activeDeviceId}"
        )
    }
    val grouped = devices.groupBy { it.productName?.toString()?.trim()?.lowercase() ?: "unknown" }
    val uniqueDevices = grouped.map { (_, group) -> group.maxByOrNull { it.id }!! }
    Log.d("AudioDeviceHelper", "Unique devices after filtering by name (max id):")
    uniqueDevices.forEach {
        Log.d(
            "AudioDeviceHelper",
            "id=${it.id}, type=${it.type}, name='${it.productName}', isActive=${it.id == activeDeviceId}"
        )
    }
    return uniqueDevices.map {
        OutputDevice(
            id = it.id,
            name = it.productName?.toString() ?: "Unknown",
            type = it.type,
            isActive = (it.id == activeDeviceId)
        )
    }
}

fun setAudioOutput(context: Context, device: OutputDevice): Boolean {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
    val target = devices.find { it.id == device.id }
    return if (target != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        try {
            // If you use MediaPlayer, setPreferredDevice is available on API 23+
            com.purrytify.mobile.ui.MiniPlayerState.mediaPlayer?.setPreferredDevice(target)
            Log.d("AudioDeviceHelper", "Set preferred device: ${device.name}")
            true
        } catch (e: Exception) {
            Log.e("AudioDeviceHelper", "Failed to set preferred device: ${e.message}")
            false
        }
    } else {
        false
    }
}