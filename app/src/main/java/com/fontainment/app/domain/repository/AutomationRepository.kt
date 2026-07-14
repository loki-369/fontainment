package com.fontainment.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface AutomationRepository {
    fun isAutomationEnabled(): Flow<Boolean>
    suspend fun setAutomationEnabled(enabled: Boolean)

    fun getTriggerBluetoothDevices(): Flow<Set<String>>
    suspend fun addTriggerBluetoothDevice(deviceName: String)
    suspend fun removeTriggerBluetoothDevice(deviceName: String)

    fun shouldLaunchOnPowerConnect(): Flow<Boolean>
    suspend fun setLaunchOnPowerConnect(enabled: Boolean)

    fun shouldLaunchOnLandscape(): Flow<Boolean>
    suspend fun setLaunchOnLandscape(enabled: Boolean)
}
