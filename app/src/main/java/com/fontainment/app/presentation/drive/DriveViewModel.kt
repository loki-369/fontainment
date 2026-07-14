package com.fontainment.app.presentation.drive

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fontainment.app.domain.model.SpotifyTrack
import com.fontainment.app.domain.model.VehicleState
import com.fontainment.app.domain.repository.MediaRepository
import com.fontainment.app.domain.repository.SettingsRepository
import com.google.android.gms.maps.model.LatLng
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

data class LatLngMarker(
    val title: String,
    val snippet: String,
    val position: LatLng,
    val category: String
)

enum class CallState {
    IDLE, INCOMING, ACTIVE
}

@HiltViewModel
class DriveViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel(), SensorEventListener, LocationListener {

    private val _vehicleState = MutableStateFlow(VehicleState())
    val vehicleState: StateFlow<VehicleState> = _vehicleState.asStateFlow()

    val currentTheme = settingsRepository.getTheme().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Tesla Dark"
    )

    val speedUnit = settingsRepository.getSpeedUnit().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "KMH"
    )

    val currentTrack: StateFlow<SpotifyTrack> = mediaRepository.currentTrack
    val musicVolume: StateFlow<Float> = mediaRepository.volume

    private val _assistantActive = MutableStateFlow(false)
    val assistantActive: StateFlow<Boolean> = _assistantActive.asStateFlow()

    private val _assistantSpeechText = MutableStateFlow("")
    val assistantSpeechText: StateFlow<String> = _assistantSpeechText.asStateFlow()

    private var timeTrackerJob: Job? = null
    
    // Hardware Sensors
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Rotation parameters
    private var gravityValues = FloatArray(3)
    private var geomagneticValues = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    // New flows for maps, call controls, and micro animations
    val currentLatLng = MutableStateFlow(LatLng(37.4220, -122.0841)) // Googleplex default
    val mapMarkers = MutableStateFlow<List<LatLngMarker>>(emptyList())
    val activeCallState = MutableStateFlow(CallState.IDLE)
    val callerName = MutableStateFlow("John Doe")
    val callerNumber = MutableStateFlow("+1 (555) 019-2834")
    val dialpadText = MutableStateFlow("")
    val recentCalls = MutableStateFlow(listOf("Mom (10 min ago)", "Office (1 hr ago)", "Tesla Service (Yesterday)"))
    val favoriteContacts = MutableStateFlow(listOf("Mom", "Office", "Alex", "Emma"))
    val activeCallTimeSeconds = MutableStateFlow(0L)
    val playbackDevices = MutableStateFlow(listOf("Phone Speaker", "Car Audio BT", "Sony WH-1000XM4"))
    val selectedPlaybackDevice = MutableStateFlow("Car Audio BT")
    val equalizerBands = MutableStateFlow(List(10) { 0.1f })

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            intent?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0
                val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                               status == BatteryManager.BATTERY_STATUS_FULL

                _vehicleState.value = _vehicleState.value.copy(
                    batteryPercentage = batteryPct,
                    isCharging = charging
                )
            }
        }
    }

    init {
        registerBatteryReceiver()
        registerLocationListener()
        registerSensorListeners()
        startTimeTracker()
        updateNetworkStatus()
    }

    private fun registerBatteryReceiver() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)
    }

    @SuppressLint("MissingPermission")
    private fun registerLocationListener() {
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L,
                    1f,
                    this
                )
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000L,
                    1f,
                    this
                )
            }
        } catch (e: SecurityException) {
            // Fallback to updates simulation if permissions not completed
        }
    }

    private fun registerSensorListeners() {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun startTimeTracker() {
        timeTrackerJob = viewModelScope.launch {
            var drivingTimeSeconds = 0L
            while (true) {
                delay(1000)
                drivingTimeSeconds++
                
                // Simulate speed if GPS isn't active
                var speed = _vehicleState.value.currentSpeedKmh
                if (speed == 0 && drivingTimeSeconds % 12 < 9) {
                    speed = 62 // mock driving speed
                } else if (drivingTimeSeconds % 12 >= 9) {
                    speed = 0 // mock stopping at red light
                }

                // Simulate coordinate movement if speed > 0
                if (speed > 0) {
                    val currentLat = currentLatLng.value.latitude
                    val currentLng = currentLatLng.value.longitude
                    // Move slightly northeast (approx 0.0001 degrees per second)
                    val nextLat = currentLat + 0.00008
                    val nextLng = currentLng + 0.0001
                    currentLatLng.value = LatLng(nextLat, nextLng)
                }

                val speedKmPerSecond = speed.toDouble() / 3600.0
                val newDistance = _vehicleState.value.tripDistanceKm + speedKmPerSecond

                // Mock road names along the simulated route
                val roadNames = listOf("Shoreline Blvd", "Charleston Rd", "US-101 North", "Grand Ave", "Bayshore Pkwy")
                val roadIndex = ((drivingTimeSeconds / 15) % roadNames.size).toInt()

                _vehicleState.value = _vehicleState.value.copy(
                    drivingTimeSeconds = drivingTimeSeconds,
                    currentSpeedKmh = speed,
                    tripDistanceKm = Math.round(newDistance * 10.0) / 10.0,
                    currentRoadName = roadNames[roadIndex]
                )

                // Simulate equalizer amplitudes
                if (currentTrack.value.isPlaying) {
                    equalizerBands.value = List(10) { (0.1f + Math.random() * 0.9f).toFloat() }
                } else {
                    equalizerBands.value = List(10) { 0.1f }
                }

                // Update active call timer if call is ongoing
                if (activeCallState.value == CallState.ACTIVE) {
                    activeCallTimeSeconds.value = activeCallTimeSeconds.value + 1
                }

                updateNetworkStatus()
            }
        }
    }

    private fun updateNetworkStatus() {
        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        val strength = when {
            caps == null -> "No Connection"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi Connected"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular LTE"
            else -> "Connected"
        }
        _vehicleState.value = _vehicleState.value.copy(networkStrength = strength)
    }

    // SensorEventListener callbacks
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(it.values, 0, gravityValues, 0, 3)
                hasGravity = true
            } else if (it.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(it.values, 0, geomagneticValues, 0, 3)
                hasGeomagnetic = true
            }

            if (hasGravity && hasGeomagnetic) {
                val r = FloatArray(9)
                val i = FloatArray(9)
                if (SensorManager.getRotationMatrix(r, i, gravityValues, geomagneticValues)) {
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(r, orientation)
                    
                    var azimuthDegrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    if (azimuthDegrees < 0) {
                        azimuthDegrees += 360f
                    }

                    val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
                    val dirIndex = (((azimuthDegrees + 22.5) % 360) / 45).toInt()
                    val compassDirection = dirs[dirIndex]

                    _vehicleState.value = _vehicleState.value.copy(
                        compassHeadingDegrees = azimuthDegrees,
                        compassDirection = compassDirection
                    )
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // LocationListener callbacks
    override fun onLocationChanged(location: Location) {
        val speedMs = location.speed
        val speedKmh = (speedMs * 3.6f).toInt()
        
        val prevDistance = _vehicleState.value.tripDistanceKm
        val speedKmPerSecond = speedKmh.toDouble() / 3600.0
        val newDistance = prevDistance + speedKmPerSecond

        _vehicleState.value = _vehicleState.value.copy(
            currentSpeedKmh = speedKmh,
            tripDistanceKm = Math.round(newDistance * 10.0) / 10.0
        )
        currentLatLng.value = LatLng(location.latitude, location.longitude)
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    // Audio Playback
    fun playPauseMusic() {
        if (currentTrack.value.isPlaying) {
            mediaRepository.pause()
        } else {
            mediaRepository.play()
        }
    }

    fun skipNext() = mediaRepository.skipToNext()
    fun skipPrevious() = mediaRepository.skipToPrevious()
    fun toggleShuffle() = mediaRepository.toggleShuffle()
    fun toggleRepeat() = mediaRepository.toggleRepeat()
    fun toggleFavorite() = mediaRepository.toggleFavorite()
    fun setVolume(value: Float) = mediaRepository.setVolume(value)
    fun seekTo(positionMs: Long) = mediaRepository.seekTo(positionMs)
    fun setPlaybackDevice(device: String) { selectedPlaybackDevice.value = device }

    // Phone Call Simulation Action handlers
    fun simulateIncomingCall() {
        callerName.value = "John Doe"
        callerNumber.value = "+1 (555) 019-2834"
        activeCallState.value = CallState.INCOMING
    }

    fun acceptCall() {
        activeCallState.value = CallState.ACTIVE
        activeCallTimeSeconds.value = 0L
    }

    fun rejectCall() {
        activeCallState.value = CallState.IDLE
    }

    fun endCall() {
        activeCallState.value = CallState.IDLE
        activeCallTimeSeconds.value = 0L
    }

    fun pressDialpadKey(char: String) {
        dialpadText.value = dialpadText.value + char
    }

    fun clearDialpad() {
        dialpadText.value = ""
    }

    fun makeCall(number: String) {
        if (number.isNotEmpty()) {
            callerName.value = number
            callerNumber.value = ""
            activeCallState.value = CallState.ACTIVE
            activeCallTimeSeconds.value = 0L
            dialpadText.value = ""
            recentCalls.value = listOf("Dialed: $number (Just now)") + recentCalls.value.take(3)
        }
    }

    // Google Maps Searching & Categories Quick Actions
    fun triggerQuickActionMarkers(category: String) {
        val lat = currentLatLng.value.latitude
        val lng = currentLatLng.value.longitude
        val list = when (category.lowercase()) {
            "coffee" -> listOf(
                LatLngMarker("Blue Bottle Coffee", "0.2 km away", LatLng(lat + 0.002, lng + 0.001), "Coffee"),
                LatLngMarker("Starbucks Reserve", "0.7 km away", LatLng(lat - 0.003, lng + 0.004), "Coffee")
            )
            "fuel" -> listOf(
                LatLngMarker("Shell Station", "1.1 km away", LatLng(lat + 0.005, lng - 0.006), "Fuel"),
                LatLngMarker("Tesla Supercharger", "0.4 km away", LatLng(lat - 0.001, lng + 0.003), "Fuel")
            )
            "hospital" -> listOf(
                LatLngMarker("Stanford Hospital Emergency", "4.2 km away", LatLng(lat + 0.015, lng + 0.02), "Hospital")
            )
            "parking" -> listOf(
                LatLngMarker("Public Parking Garage A", "0.1 km away", LatLng(lat + 0.001, lng - 0.001), "Parking"),
                LatLngMarker("Lot 4 Center Parking", "0.5 km away", LatLng(lat - 0.002, lng - 0.003), "Parking")
            )
            "restaurant" -> listOf(
                LatLngMarker("In-N-Out Burger", "0.8 km away", LatLng(lat + 0.004, lng + 0.004), "Restaurant"),
                LatLngMarker("Pizzeria Delfina", "1.5 km away", LatLng(lat - 0.006, lng + 0.002), "Restaurant")
            )
            "mechanic" -> listOf(
                LatLngMarker("Pep Boys Auto Care", "2.1 km away", LatLng(lat - 0.012, lng + 0.008), "Mechanic")
            )
            "car wash" -> listOf(
                LatLngMarker("Sparkle Hand Car Wash", "1.3 km away", LatLng(lat + 0.007, lng - 0.003), "Car Wash")
            )
            else -> emptyList()
        }
        mapMarkers.value = list
    }

    fun searchPlaces(query: String) {
        if (query.isNotEmpty()) {
            val lat = currentLatLng.value.latitude
            val lng = currentLatLng.value.longitude
            // Geocode simulated offset
            val searchDest = LatLng(lat + 0.008, lng + 0.01)
            mapMarkers.value = listOf(
                LatLngMarker(query, "Search Destination Point", searchDest, "Destination")
            )
            currentLatLng.value = searchDest
        }
    }

    // Voice Assistant HUD & Command parsing logic
    fun startVoiceAssistant() {
        viewModelScope.launch {
            _assistantActive.value = true
            _assistantSpeechText.value = "Listening..."
            delay(1500)
            
            // Auto simulate random command suggestions
            val commands = listOf(
                "Navigate to Blue Bottle Coffee",
                "Find nearest Supercharger station",
                "Play music",
                "Volume up",
                "Call Mom",
                "Theme Nothing Style",
                "Open settings"
            )
            val randomCmd = commands.random()
            _assistantSpeechText.value = "\"$randomCmd\""
            delay(1200)
            
            processVoiceCommand(randomCmd)
            delay(1500)
            _assistantActive.value = false
            _assistantSpeechText.value = ""
        }
    }

    private fun processVoiceCommand(command: String) {
        val cmdClean = command.lowercase()
        viewModelScope.launch {
            when {
                cmdClean.contains("navigate") || cmdClean.contains("find") -> {
                    val placeName = command.substringAfter("navigate to ", "").substringAfter("find nearest ", "").ifEmpty { "Simulated Target" }
                    searchPlaces(placeName)
                }
                cmdClean.contains("play") || cmdClean.contains("resume") -> {
                    mediaRepository.play()
                }
                cmdClean.contains("pause") || cmdClean.contains("stop") -> {
                    mediaRepository.pause()
                }
                cmdClean.contains("volume up") -> {
                    setVolume((musicVolume.value + 0.15f).coerceAtMost(1f))
                }
                cmdClean.contains("volume down") -> {
                    setVolume((musicVolume.value - 0.15f).coerceAtLeast(0f))
                }
                cmdClean.contains("call") -> {
                    val contact = command.substringAfter("call ", "John Doe")
                    callerName.value = contact
                    callerNumber.value = "Voice Dialed Contact"
                    activeCallState.value = CallState.ACTIVE
                    activeCallTimeSeconds.value = 0L
                }
                cmdClean.contains("theme") -> {
                    val themeName = command.substringAfter("theme ", "Tesla Dark")
                    // Capitalize
                    val formattedTheme = themeName.split(" ").joinToString(" ") { it.replaceFirstChar { it.uppercase() } }
                    settingsRepository.setTheme(formattedTheme)
                }
            }
        }
    }

    override fun onCleared() {
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // Ignored
        }
        sensorManager.unregisterListener(this)
        locationManager.removeUpdates(this)
        timeTrackerJob?.cancel()
        super.onCleared()
    }
}

