package com.purrytify.mobile.models

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val imageUrl: String,
    val audioUrl: String,
    var isLiked: Boolean
)