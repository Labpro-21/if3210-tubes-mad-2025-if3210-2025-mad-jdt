package com.purrytify.mobile.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.purrytify.mobile.viewmodel.MonthlyListeningData
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {

    fun exportMonthlyDataToCsv(
            context: Context,
            monthlyData: List<MonthlyListeningData>,
            onSuccess: (Uri) -> Unit,
            onError: (String) -> Unit
    ) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "purrytify_sound_capsule_$timestamp.csv"

            // Create file in app's external files directory
            val file = File(context.getExternalFilesDir(null), fileName)

            FileWriter(file).use { writer ->
                // Write CSV header
                writer.append("Month,Total Minutes,Top Artist,Top Song\n")

                // Write data rows
                monthlyData.forEach { data ->
                    writer.append("\"${data.month}\",")
                    writer.append("${data.totalMinutes},")
                    writer.append("\"${data.topArtist ?: "N/A"}\",")
                    writer.append("\"${data.topSong ?: "N/A"}\"\n")
                }
            }

            // Get URI for sharing
            val uri =
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

            onSuccess(uri)
        } catch (e: Exception) {
            onError("Failed to export data: ${e.message}")
        }
    }

    fun saveToDownloads(
            context: Context,
            monthlyData: List<MonthlyListeningData>,
            onSuccess: (String) -> Unit,
            onError: (String) -> Unit
    ) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "purrytify_sound_capsule_$timestamp.csv"

            // Save to Downloads folder
            val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)

            FileWriter(file).use { writer ->
                // Write CSV header
                writer.append("Month,Total Minutes,Top Artist,Top Song\n")

                // Write data rows
                monthlyData.forEach { data ->
                    writer.append("\"${data.month}\",")
                    writer.append("${data.totalMinutes},")
                    writer.append("\"${data.topArtist ?: "N/A"}\",")
                    writer.append("\"${data.topSong ?: "N/A"}\"\n")
                }
            }

            onSuccess("File saved to Downloads: $fileName")
        } catch (e: Exception) {
            onError("Failed to save file: ${e.message}")
        }
    }

    fun shareFile(context: Context, uri: Uri, @Suppress("UNUSED_PARAMETER") fileName: String) {
        val shareIntent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Purrytify Sound Capsule Analytics")
                    putExtra(
                            Intent.EXTRA_TEXT,
                            "Here's my music listening analytics from Purrytify!"
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

        val chooser = Intent.createChooser(shareIntent, "Export Sound Capsule Analytics")
        context.startActivity(chooser)
    }
}
