package com.fontainment.app.data.repository

import com.fontainment.app.data.database.SettingsDao
import com.fontainment.app.data.database.SettingsEntity
import com.fontainment.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(private val dao: SettingsDao) : SettingsRepository {
    
    override fun getTheme(): Flow<String> = 
        dao.getSettingFlow("theme").map { it ?: "Tesla Dark" }
        
    override suspend fun setTheme(themeName: String) = 
        dao.insertSetting(SettingsEntity("theme", themeName))

    override fun getAccentColor(): Flow<String> = 
        dao.getSettingFlow("accent_color").map { it ?: "#00FFC2" }
        
    override suspend fun setAccentColor(colorHex: String) = 
        dao.insertSetting(SettingsEntity("accent_color", colorHex))

    override fun getSpeedUnit(): Flow<String> = 
        dao.getSettingFlow("speed_unit").map { it ?: "KMH" }
        
    override suspend fun setSpeedUnit(unit: String) = 
        dao.insertSetting(SettingsEntity("speed_unit", unit))

    override fun getTempUnit(): Flow<String> = 
        dao.getSettingFlow("temp_unit").map { it ?: "C" }
        
    override suspend fun setTempUnit(unit: String) = 
        dao.insertSetting(SettingsEntity("temp_unit", unit))

    override fun getBrightness(): Flow<Float> = 
        dao.getSettingFlow("brightness").map { it?.toFloatOrNull() ?: 0.7f }
        
    override suspend fun setBrightness(brightness: Float) = 
        dao.insertSetting(SettingsEntity("brightness", brightness.toString()))

    override fun getAutoBrightnessEnabled(): Flow<Boolean> = 
        dao.getSettingFlow("auto_brightness").map { it?.toBoolean() ?: true }
        
    override suspend fun setAutoBrightnessEnabled(enabled: Boolean) = 
        dao.insertSetting(SettingsEntity("auto_brightness", enabled.toString()))

    override fun getAutoLaunchBluetoothDevice(): Flow<String?> = 
        dao.getSettingFlow("auto_launch_bt").map { if (it.isNullOrEmpty()) null else it }
        
    override suspend fun setAutoLaunchBluetoothDevice(deviceName: String?) = 
        dao.insertSetting(SettingsEntity("auto_launch_bt", deviceName ?: ""))
}
