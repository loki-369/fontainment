package com.fontainment.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.provider.Settings
import com.fontainment.app.domain.model.SpotifyTrack
import com.fontainment.app.domain.repository.MediaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val context: Context
) : MediaRepository {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Real audio stream track models
    private val onlineTracks = listOf(
        SpotifyTrack(
            title = "Lofi Dreams",
            artist = "Lofi Sleep",
            album = "Chill Beats Vol. 1",
            durationMs = 210000,
            progressMs = 0,
            isPlaying = false,
            albumArtUri = null,
            isFavorite = false
        ),
        SpotifyTrack(
            title = "Synthwave Cruise",
            artist = "Retro Rider",
            album = "Neon Grid",
            durationMs = 244000,
            progressMs = 0,
            isPlaying = false,
            albumArtUri = null,
            isFavorite = true
        )
    )

    private val streamUrls = listOf(
        "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
        "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
    )

    private var currentTrackIndex = 0
    private var mediaPlayer: MediaPlayer? = null

    private val _currentTrack = MutableStateFlow(
        SpotifyTrack(
            title = "Not Playing",
            artist = "Tap to connect Spotify",
            album = "",
            durationMs = 0L,
            progressMs = 0L,
            isPlaying = false,
            albumArtUri = null,
            isFavorite = false
        )
    )
    override val currentTrack: StateFlow<SpotifyTrack> = _currentTrack.asStateFlow()

    private val _volume = MutableStateFlow(0.7f)
    override val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _isNotificationAccessGranted = MutableStateFlow(false)
    override val isNotificationAccessGranted: StateFlow<Boolean> = _isNotificationAccessGranted.asStateFlow()

    private val _activePlayerPackage = MutableStateFlow<String?>(null)
    override val activePlayerPackage: StateFlow<String?> = _activePlayerPackage.asStateFlow()

    private var activeController: MediaController? = null

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateMetadata(metadata)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updatePlaybackState(state)
        }
    }

    init {
        // Sync initial volume
        syncVolume()

        // Start progress sync & permission status ticker
        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                delay(1000)
                _isNotificationAccessGranted.value = checkNotificationAccessGranted()
                
                val controller = activeController
                if (controller != null) {
                    // Update current progress from playback state
                    val state = controller.playbackState
                    if (state != null && state.state == PlaybackState.STATE_PLAYING) {
                        val currentProgress = state.position
                        _currentTrack.value = _currentTrack.value.copy(
                            progressMs = currentProgress,
                            isPlaying = true
                        )
                    } else if (state != null) {
                        _currentTrack.value = _currentTrack.value.copy(
                            isPlaying = state.state == PlaybackState.STATE_PLAYING
                        )
                    }
                } else {
                    // Sync progress from active MediaPlayer
                    mediaPlayer?.let { mp ->
                        if (mp.isPlaying) {
                            _currentTrack.value = _currentTrack.value.copy(
                                progressMs = mp.currentPosition.toLong(),
                                isPlaying = true
                            )
                        }
                    }
                }
            }
        }
    }

    fun setActiveController(controller: MediaController?) {
        try {
            activeController?.unregisterCallback(controllerCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        activeController = controller
        _activePlayerPackage.value = controller?.packageName

        if (controller != null) {
            // Stop local MediaPlayer if Spotify takes over
            mediaPlayer?.release()
            mediaPlayer = null

            controller.registerCallback(controllerCallback)
            updateMetadata(controller.metadata)
            updatePlaybackState(controller.playbackState)
        }
    }

    private fun checkNotificationAccessGranted(): Boolean {
        val packageName = context.packageName
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return flat != null && flat.contains(packageName)
    }

    private fun syncVolume() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        _volume.value = currentVolume.toFloat() / maxVolume.toFloat()
    }

    private fun updateMetadata(metadata: MediaMetadata?) {
        if (metadata == null) return
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown Track"
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist"
        val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)

        // Cache bitmap art to cache directory to get a local file Uri
        val artBitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) 
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
        val cachedArtUri = cacheAlbumArt(artBitmap) ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
        
        _currentTrack.value = _currentTrack.value.copy(
            title = title,
            artist = artist,
            album = album,
            durationMs = if (duration > 0) duration else 240000L,
            albumArtUri = cachedArtUri
        )
    }

    private fun updatePlaybackState(state: PlaybackState?) {
        if (state == null) return
        _currentTrack.value = _currentTrack.value.copy(
            isPlaying = state.state == PlaybackState.STATE_PLAYING,
            progressMs = state.position
        )
    }

    private fun cacheAlbumArt(bitmap: Bitmap?): String? {
        if (bitmap == null) return null
        return try {
            val file = File(context.cacheDir, "current_album_art.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            "file://${file.absolutePath}"
        } catch (e: Exception) {
            null
        }
    }

    private fun sendMediaButtonToSpotify(keyCode: Int) {
        try {
            val intentDown = android.content.Intent(android.content.Intent.ACTION_MEDIA_BUTTON).apply {
                `package` = "com.spotify.music"
                putExtra(android.content.Intent.EXTRA_KEY_EVENT, android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode))
            }
            context.sendOrderedBroadcast(intentDown, null)
            val intentUp = android.content.Intent(android.content.Intent.ACTION_MEDIA_BUTTON).apply {
                `package` = "com.spotify.music"
                putExtra(android.content.Intent.EXTRA_KEY_EVENT, android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode))
            }
            context.sendOrderedBroadcast(intentUp, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun play() {
        val controller = activeController
        if (controller != null) {
            controller.transportControls.play()
        } else {
            sendMediaButtonToSpotify(android.view.KeyEvent.KEYCODE_MEDIA_PLAY)
        }
    }

    override fun pause() {
        val controller = activeController
        if (controller != null) {
            controller.transportControls.pause()
        } else {
            sendMediaButtonToSpotify(android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)
        }
    }

    override fun skipToNext() {
        val controller = activeController
        if (controller != null) {
            controller.transportControls.skipToNext()
        } else {
            sendMediaButtonToSpotify(android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
        }
    }

    override fun skipToPrevious() {
        val controller = activeController
        if (controller != null) {
            controller.transportControls.skipToPrevious()
        } else {
            sendMediaButtonToSpotify(android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        }
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
        mediaPlayer?.setVolume(boundedVal, boundedVal)
    }

    override fun seekTo(positionMs: Long) {
        val controller = activeController
        if (controller != null) {
            controller.transportControls.seekTo(positionMs)
        } else {
            mediaPlayer?.let { mp ->
                mp.seekTo(positionMs.toInt())
                _currentTrack.value = _currentTrack.value.copy(progressMs = positionMs)
            }
        }
    }
}
