package com.fontainment.app.di

import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import android.telephony.TelephonyManager
import androidx.room.Room
import com.fontainment.app.data.database.AppDatabase
import com.fontainment.app.data.repository.AutomationRepositoryImpl
import com.fontainment.app.data.repository.MediaRepositoryImpl
import com.fontainment.app.data.repository.SettingsRepositoryImpl
import com.fontainment.app.domain.repository.AutomationRepository
import com.fontainment.app.domain.repository.MediaRepository
import com.fontainment.app.domain.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "fontainment.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(db: AppDatabase): SettingsRepository {
        return SettingsRepositoryImpl(db.settingsDao())
    }

    @Provides
    @Singleton
    fun provideMediaRepository(@ApplicationContext context: Context): MediaRepository {
        return MediaRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideAutomationRepository(
        @ApplicationContext context: Context,
        settingsRepository: SettingsRepository
    ): AutomationRepository {
        return AutomationRepositoryImpl(context, settingsRepository)
    }

    @Provides
    @Singleton
    fun provideLocationManager(@ApplicationContext context: Context): LocationManager {
        return context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    @Provides
    @Singleton
    fun provideTelephonyManager(@ApplicationContext context: Context): TelephonyManager {
        return context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }
}
