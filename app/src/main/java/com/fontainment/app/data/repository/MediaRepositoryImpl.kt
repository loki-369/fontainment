package com.fontainment.app.data.repository

import android.content.Context
import android.media.AudioManager
import com.fontainment.app.domain.model.SpotifyTrack
import com.fontainment.app.domain.repository.MediaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val context: Context
) : MediaRepository {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _currentTrack = MutableStateFlow(
        SpotifyTrack(
            title = "Starlight",
            artist = "Muse",
            album = "Black Holes and Revelations",
            durationMs = 240000,
            progressMs = 45000,
            isPlaying = false,
            albumArtUri = null,
            isFavorite = false
        )
    )
    override val currentTrack: StateFlow<SpotifyTrack> = _currentTrack.asStateFlow()

    private val _volume = MutableStateFlow(0.7f)
    override val volume: StateFlow<Float> = _volume.asStateFlow()

    init {
        // Sync initial volume
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        _volume.value = currentVolume.toFloat() / maxVolume.toFloat()

        // Start local simulation/ticker for progress when playing
        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                delay(1000)
                val track = _currentTrack.value
                if (track.isPlaying) {
                    val nextProgress = track.progressMs + 1000
                    if (nextProgress >= track.durationMs) {
                        // Skip to next or loop
                        skipToNext()
                    } else {
                        _currentTrack.value = track.copy(progressMs = nextProgress)
                    }
                }
            }
        }
    }

    override fun play() {
        _currentTrack.value = _currentTrack.value.copy(isPlaying = true)
    }

    override fun pause() {
        _currentTrack.value = _currentTrack.value.copy(isPlaying = false)
    }

    override fun skipToNext() {
        // Mock simple playlist transitions
        val tracks = listOf(
            SpotifyTrack("Blinding Lights", "The Weeknd", "After Hours", 200000, 0, true, null, false),
            SpotifyTrack("Sweater Weather", "The Neighbourhood", "I Love You.", 240000, 0, true, null, true),
            SpotifyTrack("Starlight", "Muse", "Black Holes and Revelations", 240000, 0, true, null, false)
        )
        val currentIndex = tracks.indexOfFirst { it.title == _currentTrack.value.title }
        val nextIndex = (currentIndex + 1) % tracks.size
        _currentTrack.value = tracks[nextIndex]
    }

    override fun skipToPrevious() {
        val tracks = listOf(
            SpotifyTrack("Blinding Lights", "The Weeknd", "After Hours", 200000, 0, true, null, false),
            SpotifyTrack("Sweater Weather", "The Neighbourhood", "I Love You.", 240000, 0, true, null, true),
            SpotifyTrack("Starlight", "Muse", "Black Holes and Revelations", 240000, 0, true, null, false)
        )
        var currentIndex = tracks.indexOfFirst { it.title == _currentTrack.value.title }
        if (currentIndex == -1) currentIndex = 0
        val prevIndex = if (currentIndex - 1 < 0) tracks.size - 1 else currentIndex - 1
        _currentTrack.value = tracks[prevIndex]
    }

    override fun toggleShuffle() {
        val current = _currentTrack.value
        _currentTrack.value = current.copy(shuffleActive = !current.shuffleActive)
    }

    override fun toggleRepeat() {
        val current = _currentTrack.value
        val nextMode = (current.repeatMode + 1) % 3
        _currentTrack.value = current.copy(repeatMode = nextMode)
    }

    override fun toggleFavorite() {
        val current = _currentTrack.value
        _currentTrack.value = current.copy(isFavorite = !current.isFavorite)
    }

    override fun setVolume(value: Float) {
        val boundedVal = value.coerceIn(0f, 1f)
        _volume.value = boundedVal
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val targetVolume = (boundedVal * maxVolume).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
    }

    override fun seekTo(positionMs: Long) {
        val current = _currentTrack.value
        val boundedPos = positionMs.coerceIn(0L, current.durationMs)
        _currentTrack.value = current.copy(progressMs = boundedPos)
    }
}
