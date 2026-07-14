package com.fontainment.app.domain.model

data class VehicleState(
    val currentSpeedKmh: Int = 0,
    val compassDirection: String = "N",
    val compassHeadingDegrees: Float = 0f,
    val batteryPercentage: Int = 100,
    val isCharging: Boolean = false,
    val isBluetoothConnected: Boolean = false,
    val connectedDeviceName: String? = null,
    val isGpsConnected: Boolean = false,
    val networkStrength: String = "Excellent",
    val weatherTempCelsius: Int = 22,
    val weatherCondition: String = "Clear",
    val drivingTimeSeconds: Long = 0,
    val tripDistanceKm: Double = 0.0,
    val ramUsagePercent: Int = 45,
    val cpuUsagePercent: Int = 15,
    val altitudeMeters: Double = 0.0,
    val currentRoadName: String = "Interstate 95"
)
