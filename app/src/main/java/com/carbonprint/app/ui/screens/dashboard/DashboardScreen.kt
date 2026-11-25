package com.carbonprint.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carbonprint.app.data.local.entity.TripEntity
import com.carbonprint.app.ui.components.CarbonGauge
import com.carbonprint.app.ui.components.MetricCard
import com.carbonprint.app.ui.components.ModeChip
import com.carbonprint.app.ui.theme.AccentAmber
import com.carbonprint.app.ui.theme.EmeraldDark
import com.carbonprint.app.ui.theme.EmeraldPrimary
import com.carbonprint.app.ui.theme.MintLight
import com.carbonprint.app.ui.theme.MintSecondary
import com.carbonprint.app.ui.theme.TextMuted
import com.carbonprint.app.ui.theme.TextPrimary
import com.carbonprint.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToTracking: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToBadges: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onTripClick: (Long) -> Unit
) {
    val isTracking by viewModel.isTracking.collectAsState()
    val currentMode by viewModel.currentMode.collectAsState()
    val todaySummary by viewModel.todaySummary.collectAsState()
    val activeGoal by viewModel.activeGoal.collectAsState()
    val recentTrips by viewModel.recentTrips.collectAsState()

    val todayCo2Grams = todaySummary?.totalCo2Grams ?: 0.0
    val todayDistanceKm = (todaySummary?.totalDistanceMeters ?: 0.0) / 1000.0
    val todayCo2SavedGrams = todaySummary?.totalCo2SavedGrams ?: 0.0
    val todayCalories = todaySummary?.totalCaloriesBurned ?: 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "CarbonPrint",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDark
                        )
                        Text(
                            text = "Smart Carbon Tracker",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToBadges) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Achievements",
                            tint = AccentAmber
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Status Card
            item {
                LiveStatusBanner(
                    isTracking = isTracking,
                    currentMode = currentMode,
                    onClick = onNavigateToTracking
                )
            }

            // Metric Cards Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Today's CO2",
                        value = if (todayCo2Grams >= 1000.0) String.format("%.2f", todayCo2Grams / 1000.0) else String.format("%.0f", todayCo2Grams),
                        unit = if (todayCo2Grams >= 1000.0) "kg" else "g",
                        icon = Icons.Default.Speed,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Distance",
                        value = String.format("%.1f", todayDistanceKm),
                        unit = "km",
                        icon = Icons.Default.Route,
                        iconTint = MintSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "CO2 Saved",
                        value = if (todayCo2SavedGrams >= 1000.0) String.format("%.2f", todayCo2SavedGrams / 1000.0) else String.format("%.0f", todayCo2SavedGrams),
                        unit = if (todayCo2SavedGrams >= 1000.0) "kg" else "g",
                        subtitle = "vs Petrol Car",
                        icon = Icons.Default.Eco,
                        iconTint = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Calories",
                        value = String.format("%.0f", todayCalories),
                        unit = "kcal",
                        icon = Icons.Default.LocalFireDepartment,
                        iconTint = Color(0xFFE65100),
                        iconBackground = Color(0xFFFFF3E0),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Walking & Running Steps Separated
            item {
                val walkingSteps = todaySummary?.walkingSteps ?: 0
                val runningSteps = todaySummary?.runningSteps ?: 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Walking Steps",
                        value = String.format("%,d", walkingSteps),
                        unit = "steps",
                        icon = Icons.Default.DirectionsWalk,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Running Steps",
                        value = String.format("%,d", runningSteps),
                        unit = "steps",
                        icon = Icons.Default.DirectionsRun,
                        iconTint = EmeraldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Weekly Carbon Budget Gauge
            item {
                CarbonGauge(
                    currentKg = activeGoal?.currentCo2Kg ?: 0.0,
                    targetKg = activeGoal?.targetCo2Kg ?: 25.0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToGoals() }
                )
            }

            // Recent Trips Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Trips",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldPrimary,
                        modifier = Modifier.clickable { onNavigateToHistory() }
                    )
                }
            }

            if (recentTrips.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsRun,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No trips logged today",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Start a trip or let background detection run",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            } else {
                items(recentTrips) { trip ->
                    RecentTripItem(trip = trip, onClick = { onTripClick(trip.id) })
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun LiveStatusBanner(
    isTracking: Boolean,
    currentMode: com.carbonprint.app.domain.model.TransportMode,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isTracking) EmeraldPrimary else MintLight
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        if (isTracking) Color.White.copy(alpha = 0.2f) else EmeraldPrimary.copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsRun,
                    contentDescription = null,
                    tint = if (isTracking) Color.White else EmeraldPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isTracking) "Live Trip Active" else "Activity Detection Ready",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isTracking) Color.White else EmeraldDark
                )
                Text(
                    text = if (isTracking) "Recording ${currentMode.displayName} • Tap to view" else "Waiting for movement • Tap to start manually",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isTracking) Color.White.copy(alpha = 0.85f) else TextSecondary
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (isTracking) Color.White else EmeraldPrimary
            )
        }
    }
}

@Composable
fun RecentTripItem(
    trip: TripEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModeChip(mode = trip.mode, subMode = trip.subMode)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${String.format("%.2f", trip.distanceMeters / 1000.0)} km",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(trip.startTime))
                Text(
                    text = "$timeStr • ${trip.durationSeconds / 60}m ${trip.durationSeconds % 60}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (trip.co2Grams >= 1000.0) String.format("%.2f kg", trip.co2Grams / 1000.0) else String.format("%.0f g", trip.co2Grams),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (trip.mode.isZeroEmission) EmeraldPrimary else Color(0xFFD32F2F)
                )
                Text(
                    text = "CO2 emitted",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}
