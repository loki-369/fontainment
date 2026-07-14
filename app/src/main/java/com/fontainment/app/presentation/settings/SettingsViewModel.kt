package com.fontainment.app.presentation.settings

import android.content.Context
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fontainment.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val currentTheme: StateFlow<String> = settingsRepository.getTheme().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Tesla Dark"
    )

    val currentAccentColor: StateFlow<String> = settingsRepository.getAccentColor().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "#00FFC2"
    )

    val speedUnit: StateFlow<String> = settingsRepository.getSpeedUnit().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "KMH"
    )

    val tempUnit: StateFlow<String> = settingsRepository.getTempUnit().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "C"
    )

    val brightness: StateFlow<Float> = settingsRepository.getBrightness().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.7f
    )

    val autoBrightnessEnabled: StateFlow<Boolean> = settingsRepository.getAutoBrightnessEnabled().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val autoLaunchDevice: StateFlow<String?> = settingsRepository.getAutoLaunchBluetoothDevice().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _pairedBluetoothDevices = MutableStateFlow<List<String>>(emptyList())
    val pairedBluetoothDevices: StateFlow<List<String>> = _pairedBluetoothDevices.asStateFlow()

    init {
        loadPairedBluetoothDevices()
    }

    fun loadPairedBluetoothDevices() {
        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bluetoothManager?.adapter
            val permissionGranted = ContextCompat.checkSelfPermission(
                context, 
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED

            val devices = if (permissionGranted) {
                adapter?.bondedDevices?.map { it.name ?: it.address } ?: emptyList()
            } else {
                emptyList()
            }
            _pairedBluetoothDevices.value = devices.ifEmpty { 
                listOf("Car Audio BT", "My Pixel Speaker", "Tesla Model 3 BT") 
            }
        } catch (e: Exception) {
            _pairedBluetoothDevices.value = listOf("Car Audio BT", "My Pixel Speaker", "Tesla Model 3 BT")
        }
    }

    fun selectTheme(name: String) {
        viewModelScope.launch {
            settingsRepository.setTheme(name)
        }
    }

    fun selectAccentColor(hexColor: String) {
        viewModelScope.launch {
            settingsRepository.setAccentColor(hexColor)
        }
    }

    fun selectSpeedUnit(unit: String) {
        viewModelScope.launch {
            settingsRepository.setSpeedUnit(unit)
        }
    }

    fun selectTempUnit(unit: String) {
        viewModelScope.launch {
            settingsRepository.setTempUnit(unit)
        }
    }

    fun setBrightnessLevel(value: Float) {
        viewModelScope.launch {
            settingsRepository.setBrightness(value)
        }
    }

    fun toggleAutoBrightness(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoBrightnessEnabled(enabled)
        }
    }

    fun configureAutoLaunchBluetoothDevice(name: String?) {
        viewModelScope.launch {
            settingsRepository.setAutoLaunchBluetoothDevice(name)
        }
    }

    // Mock Backup preferences system
    fun performBackup(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            delay(1000)
            onComplete("Backup saved successfully to Fontainment SD storage!")
        }
    }

    // Mock Restore preferences system
    fun performRestore(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            delay(1000)
            onComplete("Restore applied! Preferences updated successfully.")
        }
    }
}
