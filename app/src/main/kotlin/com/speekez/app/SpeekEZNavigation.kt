package com.speekez.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.material3.Text
import com.speekez.app.screens.settings.GeneralSettingsScreen

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object History : Screen("history")
    data object Settings : Screen("settings")
}

@Composable
fun SpeekEZNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen()
        }
        composable(Screen.History.route) {
            HistoryScreen()
        }
        composable(Screen.Settings.route) {
            GeneralSettingsScreen()
        }
    }
}

@Composable
fun DashboardScreen() {
    Text(text = "Dashboard Screen")
}

@Composable
fun HistoryScreen() {
    Text(text = "History Screen")
}
