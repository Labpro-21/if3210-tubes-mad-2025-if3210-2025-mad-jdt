package com.purrytify.mobile.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.purrytify.mobile.utils.OutputDevice
import com.purrytify.mobile.utils.getAvailableOutputDevices

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioOutputBottomSheet(
    context: Context,
    activeDeviceId: Int?,
    onDismiss: () -> Unit,
    onDeviceSelected: (OutputDevice) -> Unit
) {
    val devices = remember(activeDeviceId) { getAvailableOutputDevices(context, activeDeviceId) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181818)
    ) {
        Text(
            "Pilih Output Audio",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
        devices.forEach { device ->
            val icon = when (device.type) {
                android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> Icons.Default.BluetoothAudio
                android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET -> Icons.Default.Headset
                else -> Icons.Default.Speaker
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDeviceSelected(device) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    device.name,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (device.isActive) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (device.isActive) Color(0xFF1DB954) else Color.Gray
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}