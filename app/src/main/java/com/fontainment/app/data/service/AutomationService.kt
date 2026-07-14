package com.fontainment.app.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.fontainment.app.MainActivity
import com.fontainment.app.R
import com.fontainment.app.domain.repository.AutomationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AutomationService : Service() {

    @Inject
    lateinit var automationRepository: AutomationRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.name?.let { deviceName ->
                        checkBluetoothAndLaunch(deviceName)
                    }
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    checkPowerAndLaunch()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification())

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(Intent.ACTION_POWER_CONNECTED)
        }
        registerReceiver(receiver, filter)
    }

    private fun checkBluetoothAndLaunch(deviceName: String) {
        serviceScope.launch {
            if (automationRepository.isAutomationEnabled().first()) {
                val targets = automationRepository.getTriggerBluetoothDevices().first()
                if (targets.contains(deviceName)) {
                    launchApp()
                }
            }
        }
    }

    private fun checkPowerAndLaunch() {
        serviceScope.launch {
            if (automationRepository.isAutomationEnabled().first() && 
                automationRepository.shouldLaunchOnPowerConnect().first()) {
                launchApp()
            }
        }
    }

    private fun launchApp() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("auto_launch", true)
        }
        startActivity(launchIntent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Fontainment Automation Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors system events to automatically open Drive/Desk modes."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Fontainment active")
            .setContentText("Monitoring Bluetooth speaker and charger connections...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "fontainment_automation"
    }
}
