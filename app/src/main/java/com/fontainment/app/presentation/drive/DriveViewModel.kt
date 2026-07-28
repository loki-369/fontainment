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
import kotlinx.coroutines.Dispatchers
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

data class NavStep(
    val instruction: String,
    val streetName: String,
    val distanceMeters: Double,
    val position: LatLng
)

enum class CallState {
    IDLE, INCOMING, ACTIVE
}

// Singleton helper to share active navigation properties globally (e.g. to Desk Mode widgets)
object ActiveNavigationManager {
    val destinationName = MutableStateFlow<String?>(null)
    val currentNavInstruction = MutableStateFlow<String?>(null)
    val distanceToTurnMeters = MutableStateFlow(0)
    val hasActiveRoute = MutableStateFlow(false)
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

    // Integration of system player metadata status and route coordinates
    val currentRoutePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val targetDestination = MutableStateFlow<LatLng?>(null)
    val activePlayerPackage = mediaRepository.activePlayerPackage
    val isNotificationAccessGranted = mediaRepository.isNotificationAccessGranted

    // Live Turn-by-Turn navigation flows
    val currentNavInstruction = MutableStateFlow<String?>(null)
    val distanceToTurnMeters = MutableStateFlow(0)
    private val navigationSteps = MutableStateFlow<List<NavStep>>(emptyList())
    private var currentRouteIndex = 0

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
            // Attempt to snap map camera to best last known location immediately
            val lastKnownGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastKnownNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val bestLocation = lastKnownGps ?: lastKnownNetwork
            bestLocation?.let {
                currentLatLng.value = LatLng(it.latitude, it.longitude)
            }

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
                    val target = targetDestination.value
                    val route = currentRoutePoints.value
                    if (target != null && route.isNotEmpty()) {
                        if (currentRouteIndex < route.size) {
                            val nextPoint = route[currentRouteIndex]
                            val currentLat = currentLatLng.value.latitude
                            val currentLng = currentLatLng.value.longitude
                            val dLat = nextPoint.latitude - currentLat
                            val dLng = nextPoint.longitude - currentLng
                            val distance = Math.sqrt(dLat * dLat + dLng * dLng)
                            
                            if (distance < 0.00015) {
                                currentLatLng.value = nextPoint
                                currentRouteIndex++
                            } else {
                                val step = 0.0001
                                val nextLat = currentLat + (dLat / distance) * step
                                val nextLng = currentLng + (dLng / distance) * step
                                currentLatLng.value = LatLng(nextLat, nextLng)
                            }
                            
                            // Check active turn steps countdown
                            val stepsList = navigationSteps.value
                            val currentStep = stepsList.firstOrNull()
                            if (currentStep != null) {
                                val distToStep = calculateDistanceMeters(currentLatLng.value, currentStep.position)
                                if (distToStep < 35.0) {
                                    val nextSteps = stepsList.drop(1)
                                    navigationSteps.value = nextSteps
                                    currentNavInstruction.value = nextSteps.firstOrNull()?.instruction ?: "Drive to destination"
                                    distanceToTurnMeters.value = nextSteps.firstOrNull()?.distanceMeters?.toInt() ?: 0
                                } else {
                                    distanceToTurnMeters.value = distToStep.toInt()
                                }
                            }
                        } else {
                            // Arrived at destination
                            currentLatLng.value = target
                            targetDestination.value = null
                            currentRoutePoints.value = emptyList()
                            navigationSteps.value = emptyList()
                            currentNavInstruction.value = "Arrived at destination"
                            distanceToTurnMeters.value = 0
                        }
                    } else {
                        // No active route, lock map coordinates to your actual device's GPS telemetry (no drift)
                    }
                }

                // Update global navigation status manager
                ActiveNavigationManager.hasActiveRoute.value = targetDestination.value != null
                ActiveNavigationManager.currentNavInstruction.value = currentNavInstruction.value
                ActiveNavigationManager.distanceToTurnMeters.value = distanceToTurnMeters.value

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

    private fun calculateDistanceMeters(p1: LatLng, p2: LatLng): Double {
        val r = 6371000.0
        val lat1 = Math.toRadians(p1.latitude)
        val lat2 = Math.toRadians(p2.latitude)
        val dLat = lat2 - lat1
        val dLng = Math.toRadians(p2.longitude - p1.longitude)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
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

    private fun generateSimulatedRoute(start: LatLng, end: LatLng): List<LatLng> {
        val points = mutableListOf<LatLng>()
        points.add(start)
        val steps = 15
        for (i in 1 until steps) {
            val fraction = i.toDouble() / steps.toDouble()
            val lat = start.latitude + (end.latitude - start.latitude) * fraction
            val lng = start.longitude + (end.longitude - start.longitude) * fraction
            val offsetLat = if (i % 3 == 1) 0.0006 * Math.sin(fraction * Math.PI * 4) else 0.0
            val offsetLng = if (i % 3 == 2) 0.0006 * Math.cos(fraction * Math.PI * 4) else 0.0
            points.add(LatLng(lat + offsetLat, lng + offsetLng))
        }
        points.add(end)
        return points
    }

    private fun buildManeuverInstruction(type: String, modifier: String, streetName: String): String {
        val action = when (type.lowercase()) {
            "turn" -> when (modifier.lowercase()) {
                "left" -> "Turn left"
                "right" -> "Turn right"
                "sharp left" -> "Sharp left turn"
                "sharp right" -> "Sharp right turn"
                "slight left" -> "Slight left turn"
                "slight right" -> "Slight right turn"
                else -> "Turn"
            }
            "merge" -> "Merge"
            "exit roundabout" -> "Exit roundabout"
            "off ramp" -> "Take off ramp"
            "on ramp" -> "Take on ramp"
            "roundabout" -> "Enter roundabout"
            "arrive" -> "Arriving"
            else -> type.replaceFirstChar { it.uppercase() }
        }
        return if (streetName.isNotEmpty() && streetName != "Street") {
            "$action onto $streetName"
        } else {
            action
        }
    }

    private fun fetchOSRMRoute(start: LatLng, end: LatLng, destinationName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val urlString = "https://router.project-osrm.org/route/v1/driving/" +
                        "${start.longitude},${start.latitude};${end.longitude},${end.latitude}" +
                        "?overview=full&geometries=geojson&steps=true"
                val url = java.net.URL(urlString)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 6000
                connection.readTimeout = 6000
                
                if (connection.responseCode == 200) {
                    val text = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(text)
                    val routes = json.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val route = routes.getJSONObject(0)
                        val geometry = route.getJSONObject("geometry")
                        val coordinates = geometry.getJSONArray("coordinates")
                        val routePointsList = mutableListOf<LatLng>()
                        for (i in 0 until coordinates.length()) {
                            val point = coordinates.getJSONArray(i)
                            val lng = point.getDouble(0)
                            val lat = point.getDouble(1)
                            routePointsList.add(LatLng(lat, lng))
                        }
                        
                        val parsedSteps = mutableListOf<NavStep>()
                        val legs = route.getJSONArray("legs")
                        if (legs.length() > 0) {
                            val leg = legs.getJSONObject(0)
                            val steps = leg.getJSONArray("steps")
                            for (j in 0 until steps.length()) {
                                val step = steps.getJSONObject(j)
                                val name = step.getString("name").ifEmpty { "Street" }
                                val distance = step.getDouble("distance")
                                val maneuver = step.getJSONObject("maneuver")
                                val type = maneuver.getString("type")
                                val modifier = maneuver.optString("modifier", "")
                                
                                val stepLocation = maneuver.getJSONArray("location")
                                val stepLng = stepLocation.getDouble(0)
                                val stepLat = stepLocation.getDouble(1)
                                
                                val instructionText = buildManeuverInstruction(type, modifier, name)
                                parsedSteps.add(NavStep(instructionText, name, distance, LatLng(stepLat, stepLng)))
                            }
                        }
                        
                        launch(Dispatchers.Main) {
                            targetDestination.value = end
                            currentRoutePoints.value = routePointsList
                            navigationSteps.value = parsedSteps
                            currentRouteIndex = 0
                            currentNavInstruction.value = parsedSteps.firstOrNull()?.instruction ?: "Drive to $destinationName"
                            distanceToTurnMeters.value = parsedSteps.firstOrNull()?.distanceMeters?.toInt() ?: 0
                            ActiveNavigationManager.destinationName.value = destinationName
                            ActiveNavigationManager.hasActiveRoute.value = true
                        }
                        return@launch
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // Fallback
            launch(Dispatchers.Main) {
                val mockRoute = generateSimulatedRoute(start, end)
                targetDestination.value = end
                currentRoutePoints.value = mockRoute
                currentRouteIndex = 0
                val mockSteps = listOf(
                    NavStep("In 500m, Turn Left on Charleston Rd", "Charleston Rd", 500.0, mockRoute[5]),
                    NavStep("In 1.2km, Merge onto US-101 North", "US-101 North", 1200.0, mockRoute[10]),
                    NavStep("Drive to destination", destinationName, 300.0, end)
                )
                navigationSteps.value = mockSteps
                currentNavInstruction.value = mockSteps.first().instruction
                distanceToTurnMeters.value = 500
                ActiveNavigationManager.destinationName.value = destinationName
                ActiveNavigationManager.hasActiveRoute.value = true
            }
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
        list.firstOrNull()?.let {
            fetchOSRMRoute(currentLatLng.value, it.position, it.title)
        }
    }

    fun searchPlaces(query: String) {
        if (query.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                var searchDest: LatLng? = null
                try {
                    val geocoder = android.location.Geocoder(context)
                    val addresses = geocoder.getFromLocationName(query, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        searchDest = LatLng(address.latitude, address.longitude)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                launch(Dispatchers.Main) {
                    val finalDest = searchDest ?: LatLng(
                        currentLatLng.value.latitude + 0.012, 
                        currentLatLng.value.longitude + 0.015
                    )
                    mapMarkers.value = listOf(
                        LatLngMarker(query, "Destination Point", finalDest, "Destination")
                    )
                    fetchOSRMRoute(currentLatLng.value, finalDest, query)
                }
            }
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
                "Open settings",
                "Play Coldplay on Spotify"
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
                cmdClean.contains("play") && cmdClean.contains("spotify") -> {
                    val query = command.substringAfter("play ", "").substringBefore(" on spotify").ifEmpty { "Coldplay" }
                    val intent = Intent(android.provider.MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                        putExtra(android.provider.MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
                        putExtra("query", query)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        `package` = "com.spotify.music"
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.spotify.music")
                        launchIntent?.let { context.startActivity(it) }
                    }
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
                    val contact = command.substringAfter("call ", "").ifEmpty { "John Doe" }
                    makeCall(contact)
                }
                cmdClean.contains("theme") -> {
                    val theme = command.substringAfter("theme ", "").trim()
                    // Map theme match
                    val themeNameMapped = when {
                        theme.contains("tesla") -> "Tesla Dark"
                        theme.contains("bmw") -> "BMW Blue"
                        theme.contains("amoled") -> "AMOLED Black"
                        theme.contains("nothing") -> "Nothing Style"
                        theme.contains("lucid") -> "Lucid White"
                        theme.contains("midnight") -> "Midnight Black"
                        theme.contains("minimal") -> "Minimal Gray"
                        theme.contains("classic") -> "Classic Dashboard"
                        else -> "Wallpaper Adaptive"
                    }
                    settingsRepository.setTheme(themeNameMapped)
                }
                cmdClean.contains("settings") -> {
                    // Open settings panel
                }
            }
        }
    }
}
