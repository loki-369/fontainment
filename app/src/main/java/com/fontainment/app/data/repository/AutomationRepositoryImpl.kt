package com.fontainment.app.data.repository

import android.content.Context
import com.fontainment.app.domain.repository.AutomationRepository
import com.fontainment.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutomationRepositoryImpl @Inject constructor(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) : AutomationRepository {

    private val bluetoothDevices = MutableStateFlow<Set<String>>(
        setOf("Car Audio BT", "My Pixel Speaker", "Tesla Model 3 BT")
    )

    private val _automationEnabled = MutableStateFlow(true)
    override fun isAutomationEnabled(): Flow<Boolean> = _automationEnabled

    override suspend fun setAutomationEnabled(enabled: Boolean) {
        _automationEnabled.value = enabled
    }

    override fun getTriggerBluetoothDevices(): Flow<Set<String>> = bluetoothDevices

    override suspend fun addTriggerBluetoothDevice(deviceName: String) {
        bluetoothDevices.value = bluetoothDevices.value + deviceName
    }

    override suspend fun removeTriggerBluetoothDevice(deviceName: String) {
        bluetoothDevices.value = bluetoothDevices.value - deviceName
    }

    private val _launchOnPower = MutableStateFlow(true)
    override fun shouldLaunchOnPowerConnect(): Flow<Boolean> = _launchOnPower

    override suspend fun setLaunchOnPowerConnect(enabled: Boolean) {
        _launchOnPower.value = enabled
    }

    private val _launchOnLandscape = MutableStateFlow(true)
    override fun shouldLaunchOnLandscape(): Flow<Boolean> = _launchOnLandscape

    override suspend fun setLaunchOnLandscape(enabled: Boolean) {
        _launchOnLandscape.value = enabled
    }
}
