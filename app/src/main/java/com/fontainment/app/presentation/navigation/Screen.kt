package com.fontainment.app.presentation.navigation

sealed class Screen(val route: String) {
    object DriveMode : Screen("drive_mode")
    object DeskMode : Screen("desk_mode")
    object Settings : Screen("settings")
}
