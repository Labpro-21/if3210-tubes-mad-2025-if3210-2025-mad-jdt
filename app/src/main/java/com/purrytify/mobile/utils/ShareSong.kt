package com.purrytify.mobile.utils

import android.content.Context
import android.content.Intent

fun shareSong(context: Context, songId: Int) {
    val url = "purrytify://song/$songId"
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, url)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share song via")
    context.startActivity(shareIntent)
}