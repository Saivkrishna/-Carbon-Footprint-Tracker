package com.carbonprint.app.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.carbonprint.app.ui.components.MetricCard
import com.carbonprint.app.ui.components.ModeDonutChart
import com.carbonprint.app.ui.components.SimpleBarChart
import com.carbonprint.app.ui.theme.AccentRed
import com.carbonprint.app.ui.theme.EmeraldDark
import com.carbonprint.app.ui.theme.EmeraldPrimary
import com.carbonprint.app.ui.theme.MintLight
import com.carbonprint.app.ui.theme.MintSecondary
import com.carbonprint.app.ui.theme.TextMuted
import com.carbonprint.app.ui.theme.TextPrimary
import com.carbonprint.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel
) {
    val chartData by viewModel.chartData.collectAsState()
    val modeBreakdown by viewModel.modeBreakdown.collectAsState()
    val allTrips by viewModel.allTrips.collectAsState()
    val batterySummary by viewModel.batterySummary.collectAsState()

    val totalCo2Grams = allTrips.sumOf { it.co2Grams }
    val totalDistanceKm = allTrips.sumOf { it.distanceMeters } / 1000.0
    val totalSavedGrams = allTrips.sumOf { it.co2SavedGrams }
    val totalCalories = allTrips.sumOf { it.caloriesBurned }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Carbon Analytics",
                        fontWeight = FontWeight.Bold,
                        color = EmeraldDark
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Lifetime Aggregates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Total CO2",
                    value = if (totalCo2Grams >= 1000.0) String.format("%.1f", totalCo2Grams / 1000.0) else String.format("%.0f", totalCo2Grams),
                    unit = if (totalCo2Grams >= 1000.0) "kg" else "g",
                    icon = Icons.Default.Speed,
                    iconTint = AccentRed,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Total Distance",
                    value = String.format("%.1f", totalDistanceKm),
                    unit = "km",
                    icon = Icons.Default.Route,
                    iconTint = MintSecondary,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Total Saved",
                    value = if (totalSavedGrams >= 1000.0) String.format("%.1f", totalSavedGrams / 1000.0) else String.format("%.0f", totalSavedGrams),
                    unit = if (totalSavedGrams >= 1000.0) "kg" else "g",
                    icon = Icons.Default.Eco,
                    iconTint = EmeraldPrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Total Calories",
                    value = String.format("%.0f", totalCalories),
                    unit = "kcal",
                    icon = Icons.Default.LocalFireDepartment,
                    iconTint = Color(0xFFE65100),
                    modifier = Modifier.weight(1f)
                )
            }

            // Daily CO2 Emission Bar Chart
            SimpleBarChart(
                title = "Daily Carbon Emissions",
                subtitle = "CO2 footprint trend for the past 7 active days",
                data = chartData,
                unit = "g",
                modifier = Modifier.fillMaxWidth()
            )

            // Mode Distribution Donut Chart
            ModeDonutChart(
                modeDistances = modeBreakdown,
                modifier = Modifier.fillMaxWidth()
            )

            // Battery Efficiency Monitoring Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Battery Efficiency & Usage",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Observed device battery levels during active tracking sessions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Latest Tracking Session Metrics
                    Text(
                        text = "LATEST TRACKING SESSION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            title = "Starting",
                            value = batterySummary.latestStartPct?.let { "$it%" } ?: "N/A",
                            unit = "",
                            icon = Icons.Default.BatteryChargingFull,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Ending",
                            value = batterySummary.latestEndPct?.let { "$it%" } ?: "N/A",
                            unit = "",
                            icon = Icons.Default.BatteryChargingFull,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Decrease",
                            value = batterySummary.latestDropPct?.let { "-$it%" } ?: "0%",
                            unit = "",
                            icon = Icons.Default.BatteryChargingFull,
                            iconTint = if ((batterySummary.latestDropPct ?: 0) > 5) AccentRed else EmeraldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Daily & Weekly Observations
                    Text(
                        text = "USAGE OBSERVATIONS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            title = "Today's Decrease",
                            value = "-${batterySummary.todayTotalDropPct}%",
                            unit = "",
                            subtitle = "${batterySummary.todayTripCountWithBattery} trip(s) • ${batterySummary.todayTrackedDurationSeconds / 60}m",
                            icon = Icons.Default.Timer,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Weekly Decrease",
                            value = "-${batterySummary.weeklyTotalDropPct}%",
                            unit = "",
                            subtitle = "${batterySummary.weeklyTripCountWithBattery} trip(s) • ${batterySummary.weeklyTrackedDurationSeconds / 60}m",
                            icon = Icons.Default.Timer,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Battery Drain Rate Compared with Duration
                    if (batterySummary.weeklyAverageDrainRatePerHour != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MintLight.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Average Drain Rate: ~${batterySummary.weeklyAverageDrainRatePerHour}% per hour of tracking",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EmeraldDark
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Explanatory Note (Part 4 requirement)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Notice: Battery percentage decrease during tracking reflects overall device discharge (screen, cellular, background apps) and is not an exact measurement of power consumed by CarbonPrint alone.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
