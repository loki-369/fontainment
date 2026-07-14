package com.fontainment.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.fontainment.app.presentation.desk.DeskModeScreen
import com.fontainment.app.presentation.desk.DeskViewModel
import com.fontainment.app.presentation.drive.DriveModeScreen
import com.fontainment.app.presentation.drive.DriveViewModel
import com.fontainment.app.presentation.settings.SettingsScreen
import com.fontainment.app.presentation.settings.SettingsViewModel

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.DriveMode.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = Screen.DriveMode.route) {
            val viewModel: DriveViewModel = hiltViewModel()
            DriveModeScreen(navController = navController, viewModel = viewModel)
        }
        composable(route = Screen.DeskMode.route) {
            val viewModel: DeskViewModel = hiltViewModel()
            DeskModeScreen(navController = navController, viewModel = viewModel)
        }
        composable(route = Screen.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(navController = navController, viewModel = viewModel)
        }
    }
}
