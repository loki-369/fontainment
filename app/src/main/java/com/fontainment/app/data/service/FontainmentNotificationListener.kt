package com.fontainment.app.data.service

import android.content.ComponentName
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import com.fontainment.app.domain.repository.MediaRepository
import com.fontainment.app.data.repository.MediaRepositoryImpl
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FontainmentNotificationListener : NotificationListenerService() {

    @Inject
    lateinit var mediaRepository: MediaRepository

    private var sessionManager: MediaSessionManager? = null

    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        updateActiveController(controllers)
    }

    override fun onCreate() {
        super.onCreate()
        sessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        try {
            val component = ComponentName(this, FontainmentNotificationListener::class.java)
            sessionManager?.addOnActiveSessionsChangedListener(sessionsChangedListener, component)
            val activeControllers = sessionManager?.getActiveSessions(component)
            updateActiveController(activeControllers)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onListenerDisconnected() {
        try {
            sessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onListenerDisconnected()
    }

    private fun updateActiveController(controllers: List<MediaController>?) {
        if (controllers == null || controllers.isEmpty()) {
            (mediaRepository as? MediaRepositoryImpl)?.setActiveController(null)
            return
        }

        // 1. Prioritize any controller that is actively playing
        val playingController = controllers.firstOrNull { controller ->
            val state = controller.playbackState
            state != null && state.state == PlaybackState.STATE_PLAYING
        }

        // 2. If none are playing, find one from a known media package (preferring Spotify/YT Music)
        val mediaPackages = listOf("com.spotify.music", "com.google.android.apps.youtube.music", "com.apple.android.music")
        val preferredController = playingController ?: controllers.firstOrNull { controller ->
            mediaPackages.contains(controller.packageName)
        }

        // 3. Fallback to the first controller in the list
        val finalController = preferredController ?: controllers.firstOrNull()

        (mediaRepository as? MediaRepositoryImpl)?.setActiveController(finalController)
    }
}
