package com.carbonprint.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null
) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    data object Tracking : Screen("tracking", "Live Tracking", Icons.Default.DirectionsRun)
    data object History : Screen("history", "Trip History", Icons.Default.History)
    data object Analytics : Screen("analytics", "Analytics", Icons.Default.BarChart)
    data object Goals : Screen("goals", "Goals", Icons.Default.TrackChanges)
    data object Badges : Screen("badges", "Badges", Icons.Default.EmojiEvents)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object TripDetail : Screen("trip_detail/{tripId}", "Trip Detail") {
        fun createRoute(tripId: Long) = "trip_detail/$tripId"
    }

    companion object {
        val bottomNavItems = listOf(
            Dashboard,
            Tracking,
            History,
            Analytics,
            Goals
        )
    }
}
