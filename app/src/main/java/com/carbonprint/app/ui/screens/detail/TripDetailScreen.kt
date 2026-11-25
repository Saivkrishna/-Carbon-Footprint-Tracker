package com.carbonprint.app.ui.screens.detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carbonprint.app.ui.components.MetricCard
import com.carbonprint.app.ui.components.ModeChip
import com.carbonprint.app.ui.components.RouteMapView
import com.carbonprint.app.ui.theme.AccentRed
import com.carbonprint.app.ui.theme.EmeraldDark
import com.carbonprint.app.ui.theme.EmeraldPrimary
import com.carbonprint.app.ui.theme.MintLight
import com.carbonprint.app.ui.theme.TextMuted
import com.carbonprint.app.ui.theme.TextPrimary
import com.carbonprint.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    viewModel: TripDetailViewModel,
    onBack: () -> Unit
) {
    val trip by viewModel.trip.collectAsState()
    val waypoints by viewModel.waypoints.collectAsState()
    val processedRoute by viewModel.processedRoute.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Journey Record") },
            text = { Text("Are you sure you want to permanently delete this trip and its recorded GPS route?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteTrip(onDeleted = onBack)
                    }
                ) {
                    Text("Delete", color = AccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trip Summary", fontWeight = FontWeight.Bold, color = EmeraldDark) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Trip",
                            tint = AccentRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        val currentTrip = trip
        if (currentTrip == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EmeraldPrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Mode Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ModeChip(mode = currentTrip.mode, subMode = currentTrip.subMode)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            val dateStr = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
                                .format(Date(currentTrip.startTime))
                            val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault())
                                .format(Date(currentTrip.startTime))
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Started at $timeStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // Interactive GPS Route Map or Empty State
                if (processedRoute.hasValidRoute) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Route,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "GPS Route Trail",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = "${processedRoute.points.size} Waypoints",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            RouteMapView(
                                processedRoute = processedRoute,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFFE0E0E0), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Route,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "No GPS Route Points",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "This trip does not contain recorded GPS waypoints (e.g. stationary or manual entry).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                // Primary Metrics 2x2 Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Distance",
                        value = String.format("%.2f", currentTrip.distanceMeters / 1000.0),
                        unit = "km",
                        icon = Icons.Default.Route,
                        modifier = Modifier.weight(1f)
                    )
                    val mins = currentTrip.durationSeconds / 60
                    val secs = currentTrip.durationSeconds % 60
                    MetricCard(
                        title = "Duration",
                        value = String.format("%02d:%02d", mins, secs),
                        unit = "",
                        icon = Icons.Default.Timer,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "CO2 Emitted",
                        value = if (currentTrip.co2Grams >= 1000.0) String.format("%.2f", currentTrip.co2Grams / 1000.0) else String.format("%.0f", currentTrip.co2Grams),
                        unit = if (currentTrip.co2Grams >= 1000.0) "kg" else "g",
                        icon = Icons.Default.Speed,
                        iconTint = if (currentTrip.mode.isZeroEmission) EmeraldPrimary else AccentRed,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "CO2 Saved",
                        value = if (currentTrip.co2SavedGrams >= 1000.0) String.format("%.2f", currentTrip.co2SavedGrams / 1000.0) else String.format("%.0f", currentTrip.co2SavedGrams),
                        unit = if (currentTrip.co2SavedGrams >= 1000.0) "kg" else "g",
                        icon = Icons.Default.Eco,
                        iconTint = EmeraldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }

                val isWalkOrRun = currentTrip.mode == com.carbonprint.app.domain.model.TransportMode.WALKING ||
                        currentTrip.mode == com.carbonprint.app.domain.model.TransportMode.RUNNING

                if (isWalkOrRun) {
                    val wSteps = if (currentTrip.walkingSteps > 0) currentTrip.walkingSteps else (if (currentTrip.mode == com.carbonprint.app.domain.model.TransportMode.WALKING) currentTrip.stepCount else 0)
                    val rSteps = if (currentTrip.runningSteps > 0) currentTrip.runningSteps else (if (currentTrip.mode == com.carbonprint.app.domain.model.TransportMode.RUNNING) currentTrip.stepCount else 0)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "Walking Steps",
                            value = String.format("%,d", wSteps),
                            unit = "steps",
                            icon = Icons.Default.DirectionsWalk,
                            iconTint = EmeraldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Running Steps",
                            value = String.format("%,d", rSteps),
                            unit = "steps",
                            icon = Icons.Default.DirectionsRun,
                            iconTint = EmeraldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (currentTrip.walkingDurationSeconds > 0 || currentTrip.runningDurationSeconds > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val wMins = currentTrip.walkingDurationSeconds / 60
                            val wSecs = currentTrip.walkingDurationSeconds % 60
                            val rMins = currentTrip.runningDurationSeconds / 60
                            val rSecs = currentTrip.runningDurationSeconds % 60
                            MetricCard(
                                title = "Walk Duration",
                                value = String.format("%02d:%02d", wMins, wSecs),
                                unit = "",
                                icon = Icons.Default.Timer,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = "Run Duration",
                                value = String.format("%02d:%02d", rMins, rSecs),
                                unit = "",
                                icon = Icons.Default.Timer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "Avg Speed",
                            value = String.format("%.1f", currentTrip.averageSpeedKmh),
                            unit = "km/h",
                            icon = Icons.Default.Speed,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Calories",
                            value = String.format("%.0f", currentTrip.caloriesBurned),
                            unit = "kcal",
                            icon = Icons.Default.LocalFireDepartment,
                            iconTint = Color(0xFFE65100),
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "Avg Speed",
                            value = String.format("%.1f", currentTrip.averageSpeedKmh),
                            unit = "km/h",
                            icon = Icons.Default.Speed,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Max Speed",
                            value = String.format("%.1f", currentTrip.maxSpeedKmh),
                            unit = "km/h",
                            icon = Icons.Default.Speed,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val intensityLabel = when {
                            currentTrip.subMode.contains("MOTORCYCLE", ignoreCase = true) -> "103 g/km"
                            currentTrip.mode == com.carbonprint.app.domain.model.TransportMode.TRANSIT -> "68 g/km"
                            currentTrip.mode == com.carbonprint.app.domain.model.TransportMode.ON_BICYCLE -> "0 g/km"
                            else -> "192 g/km"
                        }
                        MetricCard(
                            title = "Est. Intensity",
                            value = intensityLabel,
                            unit = "",
                            icon = Icons.Default.Eco,
                            iconTint = EmeraldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Calories",
                            value = String.format("%.0f", currentTrip.caloriesBurned),
                            unit = "kcal",
                            icon = Icons.Default.LocalFireDepartment,
                            iconTint = Color(0xFFE65100),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Battery Usage Card (Part 4 requirement)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BatteryChargingFull,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Battery Usage During Trip",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricCard(
                                title = "Start Level",
                                value = currentTrip.startBatteryPct?.let { "$it%" } ?: "N/A",
                                unit = "",
                                icon = Icons.Default.BatteryChargingFull,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = "End Level",
                                value = currentTrip.endBatteryPct?.let { "$it%" } ?: "N/A",
                                unit = "",
                                icon = Icons.Default.BatteryChargingFull,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = "Decrease",
                                value = currentTrip.batteryDropPct?.let { "-$it%" } ?: "0%",
                                unit = "",
                                icon = Icons.Default.BatteryChargingFull,
                                iconTint = if ((currentTrip.batteryDropPct ?: 0) > 5) AccentRed else EmeraldPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // GPS Route & Waypoints Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GPS Path Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Recorded Waypoints: ${waypoints.size} GPS points",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        if (waypoints.isNotEmpty()) {
                            val first = waypoints.first()
                            val last = waypoints.last()
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Start: ${String.format("%.4f", first.latitude)}, ${String.format("%.4f", first.longitude)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            Text(
                                text = "End: ${String.format("%.4f", last.latitude)}, ${String.format("%.4f", last.longitude)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
