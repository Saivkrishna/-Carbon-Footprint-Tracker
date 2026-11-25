package com.carbonprint.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.carbonprint.app.CarbonPrintApp
import com.carbonprint.app.ui.navigation.BottomNavBar
import com.carbonprint.app.ui.navigation.Screen
import com.carbonprint.app.ui.screens.analytics.AnalyticsScreen
import com.carbonprint.app.ui.screens.analytics.AnalyticsViewModel
import com.carbonprint.app.ui.screens.badges.BadgesScreen
import com.carbonprint.app.ui.screens.badges.BadgesViewModel
import com.carbonprint.app.ui.screens.dashboard.DashboardScreen
import com.carbonprint.app.ui.screens.dashboard.DashboardViewModel
import com.carbonprint.app.ui.screens.detail.TripDetailScreen
import com.carbonprint.app.ui.screens.detail.TripDetailViewModel
import com.carbonprint.app.ui.screens.goals.GoalsScreen
import com.carbonprint.app.ui.screens.goals.GoalsViewModel
import com.carbonprint.app.ui.screens.history.HistoryScreen
import com.carbonprint.app.ui.screens.history.HistoryViewModel
import com.carbonprint.app.ui.screens.settings.SettingsScreen
import com.carbonprint.app.ui.screens.settings.SettingsViewModel
import com.carbonprint.app.ui.screens.tracking.LiveTrackingScreen
import com.carbonprint.app.ui.screens.tracking.LiveTrackingViewModel

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as CarbonPrintApp
    val navController = rememberNavController()

    // Screen ViewModels created from application singletons
    val dashboardViewModel = remember {
        DashboardViewModel(
            tripRepository = app.tripRepository,
            goalRepository = app.goalRepository,
            analyticsRepository = app.analyticsRepository
        )
    }

    val trackingViewModel = remember { LiveTrackingViewModel() }

    val historyViewModel = remember {
        HistoryViewModel(tripRepository = app.tripRepository)
    }

    val analyticsViewModel = remember {
        AnalyticsViewModel(
            analyticsRepository = app.analyticsRepository,
            tripRepository = app.tripRepository
        )
    }

    val goalsViewModel = remember {
        GoalsViewModel(goalRepository = app.goalRepository)
    }

    val badgesViewModel = remember {
        BadgesViewModel(badgeRepository = app.badgeRepository)
    }

    val settingsViewModel = remember {
        SettingsViewModel(preferencesManager = app.preferencesManager)
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show bottom bar only on primary top-level tabs
    val showBottomBar = currentRoute in Screen.bottomNavItems.map { it.route }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToTracking = { navController.navigate(Screen.Tracking.route) },
                    onNavigateToHistory = { navController.navigate(Screen.History.route) },
                    onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) },
                    onNavigateToGoals = { navController.navigate(Screen.Goals.route) },
                    onNavigateToBadges = { navController.navigate(Screen.Badges.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onTripClick = { tripId ->
                        navController.navigate(Screen.TripDetail.createRoute(tripId))
                    }
                )
            }

            composable(Screen.Tracking.route) {
                LiveTrackingScreen(viewModel = trackingViewModel)
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    viewModel = historyViewModel,
                    onTripClick = { tripId ->
                        navController.navigate(Screen.TripDetail.createRoute(tripId))
                    }
                )
            }

            composable(
                route = Screen.TripDetail.route,
                arguments = listOf(navArgument("tripId") { type = NavType.LongType })
            ) { backStackEntry ->
                val tripId = backStackEntry.arguments?.getLong("tripId") ?: -1L
                val tripDetailViewModel = remember(tripId) {
                    TripDetailViewModel(
                        tripRepository = app.tripRepository,
                        tripId = tripId
                    )
                }
                TripDetailScreen(
                    viewModel = tripDetailViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Analytics.route) {
                AnalyticsScreen(viewModel = analyticsViewModel)
            }

            composable(Screen.Goals.route) {
                GoalsScreen(viewModel = goalsViewModel)
            }

            composable(Screen.Badges.route) {
                BadgesScreen(
                    viewModel = badgesViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
