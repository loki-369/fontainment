package com.fontainment.app.domain.model

data class SpotifyTrack(
    val title: String = "Not Playing",
    val artist: String = "Unknown Artist",
    val album: String = "Unknown Album",
    val durationMs: Long = 0,
    val progressMs: Long = 0,
    val isPlaying: Boolean = false,
    val albumArtUri: String? = null,
    val isFavorite: Boolean = false,
    val shuffleActive: Boolean = false,
    val repeatMode: Int = 0 // 0 = off, 1 = all, 2 = one
)
