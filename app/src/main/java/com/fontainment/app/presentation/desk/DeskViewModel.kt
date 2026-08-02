package com.fontainment.app.presentation.desk

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fontainment.app.domain.model.DeskWidget
import com.fontainment.app.domain.model.SpotifyTrack
import com.fontainment.app.domain.model.WidgetType
import com.fontainment.app.domain.repository.MediaRepository
import com.fontainment.app.domain.repository.SettingsRepository
import com.fontainment.app.presentation.drive.ActiveNavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class DeskViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _burnInOffset = MutableStateFlow(Pair(0f, 0f))
    val burnInOffset: StateFlow<Pair<Float, Float>> = _burnInOffset.asStateFlow()

    private val _widgets = MutableStateFlow(
        listOf(
            DeskWidget("1", WidgetType.CLOCK_DIGITAL, true, "Medium"),
            DeskWidget("2", WidgetType.WEATHER, true, "Medium"),
            DeskWidget("3", WidgetType.MUSIC, true, "Medium"),
            DeskWidget("4", WidgetType.SYSTEM_MONITOR, true, "Medium")
        )
    )
    val widgets: StateFlow<List<DeskWidget>> = _widgets.asStateFlow()

    val currentTheme = settingsRepository.getTheme().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Tesla Dark"
    )

    val currentTrack: StateFlow<SpotifyTrack> = mediaRepository.currentTrack
    val isNotificationAccessGranted: StateFlow<Boolean> = mediaRepository.isNotificationAccessGranted
    val activePlayerPackage: StateFlow<String?> = mediaRepository.activePlayerPackage

    // Live active navigation stats shared from DriveMode
    val destinationName: StateFlow<String?> = ActiveNavigationManager.destinationName
    val currentNavInstruction: StateFlow<String?> = ActiveNavigationManager.currentNavInstruction
    val distanceToTurnMeters: StateFlow<Int> = ActiveNavigationManager.distanceToTurnMeters
    val hasActiveRoute: StateFlow<Boolean> = ActiveNavigationManager.hasActiveRoute

    // Real Telemetry states
    private val _batteryPercentage = MutableStateFlow(100)
    val batteryPercentage: StateFlow<Int> = _batteryPercentage.asStateFlow()

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    private val _ramUsagePercent = MutableStateFlow(40)
    val ramUsagePercent: StateFlow<Int> = _ramUsagePercent.asStateFlow()

    private val _deviceTemp = MutableStateFlow(35)
    val deviceTemp: StateFlow<Int> = _deviceTemp.asStateFlow()

    private var tickerJob: Job? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            intent?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val temp = it.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                
                _batteryPercentage.value = if (level >= 0 && scale > 0) (level * 100 / scale) else 100
                _isCharging.value = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                                    status == BatteryManager.BATTERY_STATUS_FULL
                _deviceTemp.value = if (temp > 0) temp / 10 else 35
            }
        }
    }

    init {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)
        startBurnInProtectionTimer()
        startSystemMonitorTicker()
    }

    private fun startBurnInProtectionTimer() {
        viewModelScope.launch {
            while (true) {
                delay(60000)
                val dx = Random.nextFloat() * 10f - 5f
                val dy = Random.nextFloat() * 10f - 5f
                _burnInOffset.value = Pair(dx, dy)
            }
        }
    }

    private fun startSystemMonitorTicker() {
        tickerJob = viewModelScope.launch {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            
            while (true) {
                activityManager.getMemoryInfo(memoryInfo)
                val totalMemory = memoryInfo.totalMem
                val availableMemory = memoryInfo.availMem
                val usedPercent = ((totalMemory - availableMemory).toDouble() / totalMemory.toDouble() * 100.0).toInt()
                
                _ramUsagePercent.value = usedPercent.coerceIn(1, 99)
                delay(3000)
            }
        }
    }

    fun playPauseMusic() {
        if (currentTrack.value.isPlaying) {
            mediaRepository.pause()
        } else {
            mediaRepository.play()
        }
    }

    fun skipNext() = mediaRepository.skipToNext()
    fun skipPrevious() = mediaRepository.skipToPrevious()
    fun seekTo(positionMs: Long) = mediaRepository.seekTo(positionMs)

    private val sharedPrefs = context.getSharedPreferences("desk_ui_prefs", Context.MODE_PRIVATE)

    private val _uiMode = MutableStateFlow(
        UiMode.valueOf(sharedPrefs.getString("ui_mode", UiMode.MINIMAL.name) ?: UiMode.MINIMAL.name)
    )
    val uiMode: StateFlow<UiMode> = _uiMode.asStateFlow()

    private val _clockTheme = MutableStateFlow(
        ClockTheme.valueOf(sharedPrefs.getString("clock_theme", ClockTheme.ORANGE_STANDBY.name) ?: ClockTheme.ORANGE_STANDBY.name)
    )
    val clockTheme: StateFlow<ClockTheme> = _clockTheme.asStateFlow()

    private val _backgroundStyle = MutableStateFlow(
        BackgroundStyle.valueOf(sharedPrefs.getString("background_style", BackgroundStyle.DYNAMIC_BLUR.name) ?: BackgroundStyle.DYNAMIC_BLUR.name)
    )
    val backgroundStyle: StateFlow<BackgroundStyle> = _backgroundStyle.asStateFlow()

    fun setUiMode(mode: UiMode) {
        _uiMode.value = mode
        sharedPrefs.edit().putString("ui_mode", mode.name).apply()
    }

    fun setClockTheme(theme: ClockTheme) {
        _clockTheme.value = theme
        sharedPrefs.edit().putString("clock_theme", theme.name).apply()
    }

    fun setBackgroundStyle(style: BackgroundStyle) {
        _backgroundStyle.value = style
        sharedPrefs.edit().putString("background_style", style.name).apply()
    }

    override fun onCleared() {
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // Ignored
        }
        tickerJob?.cancel()
        super.onCleared()
    }
}

enum class UiMode {
    MINIMAL, CUSTOMISED, CLASSIC
}

enum class ClockTheme {
    ORANGE_STANDBY, MINIMALIST_WHITE, RETRO_GREEN, VECTOR_ANALOGUE
}

enum class BackgroundStyle {
    DYNAMIC_BLUR, PITCH_BLACK, CHARCOAL_GREY
}
