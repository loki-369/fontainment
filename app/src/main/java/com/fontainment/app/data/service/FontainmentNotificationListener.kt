package com.fontainment.app.data.service

import android.content.ComponentName
import android.media.session.MediaController
import android.media.session.MediaSessionManager
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
        val activeController = controllers?.firstOrNull()
        (mediaRepository as? MediaRepositoryImpl)?.setActiveController(activeController)
    }
}
