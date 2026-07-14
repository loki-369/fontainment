package com.fontainment.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SettingsEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
}
