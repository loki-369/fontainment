package com.fontainment.app.domain.model

enum class WidgetType(val displayName: String) {
    CLOCK_DIGITAL("Digital Clock"),
    CLOCK_ANALOG("Analog Clock"),
    WEATHER("Weather Dashboard"),
    CALENDAR("Calendar"),
    MUSIC("Spotify Player"),
    SYSTEM_MONITOR("System Monitor"),
    PHOTO_SLIDESHOW("Photos Slideshow"),
    NEWS("News Headlines")
}

data class DeskWidget(
    val id: String,
    val type: WidgetType,
    val isPinned: Boolean = false,
    val size: String = "Medium" // Small, Medium, Large
)
