package com.fontainment.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getTheme(): Flow<String>
    suspend fun setTheme(themeName: String)

    fun getAccentColor(): Flow<String>
    suspend fun setAccentColor(colorHex: String)

    fun getSpeedUnit(): Flow<String> // "KMH" or "MPH"
    suspend fun setSpeedUnit(unit: String)

    fun getTempUnit(): Flow<String> // "C" or "F"
    suspend fun setTempUnit(unit: String)

    fun getBrightness(): Flow<Float>
    suspend fun setBrightness(brightness: Float)

    fun getAutoBrightnessEnabled(): Flow<Boolean>
    suspend fun setAutoBrightnessEnabled(enabled: Boolean)

    fun getAutoLaunchBluetoothDevice(): Flow<String?>
    suspend fun setAutoLaunchBluetoothDevice(deviceName: String?)
}
