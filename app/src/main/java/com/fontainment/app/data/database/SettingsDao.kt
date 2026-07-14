package com.fontainment.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT value FROM settings WHERE `key` = :key LIMIT 1")
    fun getSettingFlow(key: String): Flow<String?>

    @Query("SELECT value FROM settings WHERE `key` = :key LIMIT 1")
    suspend fun getSettingVal(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(entity: SettingsEntity)
}
