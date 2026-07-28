package com.fontainment.app.domain.repository

import com.fontainment.app.domain.model.SpotifyTrack
import kotlinx.coroutines.flow.StateFlow

interface MediaRepository {
    val currentTrack: StateFlow<SpotifyTrack>
    val volume: StateFlow<Float>
    val isNotificationAccessGranted: StateFlow<Boolean>
    val activePlayerPackage: StateFlow<String?>

    fun play()
    fun pause()
    fun skipToNext()
    fun skipToPrevious()
    fun toggleShuffle()
    fun toggleRepeat()
    fun toggleFavorite()
    fun setVolume(value: Float)
    fun seekTo(positionMs: Long)
}
