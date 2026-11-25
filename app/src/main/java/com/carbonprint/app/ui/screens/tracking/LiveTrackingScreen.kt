package com.carbonprint.app.ui.screens.tracking

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.carbonprint.app.domain.model.TransportMode
import com.carbonprint.app.ui.components.MetricCard
import com.carbonprint.app.ui.components.ModeChip
import com.carbonprint.app.ui.theme.AccentRed
import com.carbonprint.app.ui.theme.EmeraldDark
import com.carbonprint.app.ui.theme.EmeraldPrimary
import com.carbonprint.app.ui.theme.MintLight
import com.carbonprint.app.ui.theme.MintSecondary
import com.carbonprint.app.ui.theme.TextMuted
import com.carbonprint.app.ui.theme.TextPrimary
import com.carbonprint.app.ui.theme.TextSecondary

private data class ModeOption(
    val title: String,
    val mode: TransportMode,
    val subMode: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTrackingScreen(
    viewModel: LiveTrackingViewModel
) {
    val context = LocalContext.current
    val isTracking by viewModel.isTracking.collectAsState()
    val currentMode by viewModel.currentMode.collectAsState()
    val currentSubMode by viewModel.currentSubMode.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()
    val selectedSubMode by viewModel.selectedSubMode.collectAsState()
    val distanceMeters by viewModel.distanceMeters.collectAsState()
    val durationSeconds by viewModel.durationSeconds.collectAsState()
    val co2Grams by viewModel.co2Grams.collectAsState()
    val co2SavedGrams by viewModel.co2SavedGrams.collectAsState()
    val calories by viewModel.calories.collectAsState()
    val speedKmh by viewModel.speedKmh.collectAsState()
    val stepCount by viewModel.stepCount.collectAsState()
    val walkingSteps by viewModel.walkingSteps.collectAsState()
    val runningSteps by viewModel.runningSteps.collectAsState()
    val walkingDurationSeconds by viewModel.walkingDurationSeconds.collectAsState()
    val runningDurationSeconds by viewModel.runningDurationSeconds.collectAsState()
    val startBatteryPct by viewModel.startBatteryPct.collectAsState()
    val batteryDropPct by viewModel.batteryDropPct.collectAsState()

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var showPermissionRationaleDialog by remember { mutableStateOf(false) }
    var permissionDeniedOnce by remember { mutableStateOf(false) }

    val modeOptions = listOf(
        ModeOption("Walking", TransportMode.WALKING, "WALKING", Icons.Default.DirectionsWalk),
        ModeOption("Running", TransportMode.RUNNING, "RUNNING", Icons.Default.DirectionsRun),
        ModeOption("Cycling", TransportMode.ON_BICYCLE, "ON_BICYCLE", Icons.Default.DirectionsBike),
        ModeOption("Motorcycle", TransportMode.IN_VEHICLE, "MOTORCYCLE", Icons.Default.TwoWheeler),
        ModeOption("Car", TransportMode.IN_VEHICLE, "CAR", Icons.Default.DirectionsCar),
        ModeOption("Bus / Transit", TransportMode.TRANSIT, "TRANSIT", Icons.Default.DirectionsBus),
        ModeOption("Still (Rest)", TransportMode.STILL, "STILL", Icons.Default.LocationOn)
    )

    fun getRequiredPermissions(): Array<String> {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return permissions.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            permissionDeniedOnce = false
            viewModel.startTracking(context)
        } else {
            permissionDeniedOnce = true
        }
    }

    if (showPermissionRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionRationaleDialog = false },
            title = {
                Text(
                    text = "Location & Tracking Permissions",
                    fontWeight = FontWeight.Bold,
                    color = EmeraldDark
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "To accurately record your journeys and measure carbon savings, CarbonPrint needs:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "• 📍 Precise Location: Records GPS waypoints to compute traveled distance and tailpipe emissions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "• 🚶 Activity Recognition: Accurately detects walking, cycling, or vehicle trips.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "• 🔔 Notifications: Keeps live tracking running in the foreground with real-time stats and a stop button.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationaleDialog = false
                        permissionLauncher.launch(getRequiredPermissions())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Continue & Grant")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationaleDialog = false }) {
                    Text("Not Now")
                }
            }
        )
    }

    val activeMode = if (isTracking) currentMode else selectedMode
    val activeSubMode = if (isTracking) currentSubMode else selectedSubMode
    val isStill = activeMode == TransportMode.STILL
    val isWalkOrRun = activeMode == TransportMode.WALKING || activeMode == TransportMode.RUNNING
    val isMotorcycle = activeSubMode.contains("MOTORCYCLE", ignoreCase = true)
    val isCar = (activeMode == TransportMode.IN_VEHICLE && !isMotorcycle) || activeSubMode.contains("CAR", ignoreCase = true)
    val isCycling = activeMode == TransportMode.ON_BICYCLE
    val isTransit = activeMode == TransportMode.TRANSIT

    val mins = durationSeconds / 60
    val secs = durationSeconds % 60
    val timeFormatted = String.format("%02d:%02d", mins, secs)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Live GPS Tracking",
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mode Selector (Active if not currently tracking)
            if (!isTracking) {
                Text(
                    text = "Select Travel Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(modeOptions) { item ->
                        val isSelected = selectedMode == item.mode && selectedSubMode == item.subMode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surface)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) EmeraldPrimary else Color(0xFFE0E0E0),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { viewModel.selectMode(item.mode, item.subMode) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = if (isSelected) Color.White else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.title,
                                    color = if (isSelected) Color.White else TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Stationary Status Card when STILL is selected
            if (isStill && !isTracking) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Stationary Status Active",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "You are currently stationary. Trips are not recorded while at rest to prevent empty logs and save battery.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Big Speedometer & Distance Circle
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(CircleShape)
                    .background(if (isTracking) MintLight else MaterialTheme.colorScheme.surface)
                    .border(
                        width = 4.dp,
                        color = if (isTracking) EmeraldPrimary else Color(0xFFD6E8DE),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ModeChip(
                        mode = activeMode,
                        subMode = activeSubMode
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = String.format("%.2f", distanceMeters / 1000.0),
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 42.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "KILOMETERS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        letterSpacing = 1.5.sp
                    )
                    if (isWalkOrRun) {
                        val activeSteps = if (activeMode == TransportMode.RUNNING) runningSteps else walkingSteps
                        val stepLabel = if (activeMode == TransportMode.RUNNING) "RUN STEPS" else "WALK STEPS"
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$activeSteps $stepLabel",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${String.format("%.1f", speedKmh)} km/h",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Mode-Tailored Metric Grids
            when {
                // 1. Walking & Running: steps counted separately, distance, duration, CO2 emissions (0g) and saved
                isWalkOrRun -> {
                    val isRunning = activeMode == TransportMode.RUNNING
                    val modeDuration = if (isRunning) runningDurationSeconds else walkingDurationSeconds
                    val activeModeTime = if (modeDuration > 0) {
                        String.format("%02d:%02d", modeDuration / 60, modeDuration % 60)
                    } else {
                        timeFormatted
                    }
                    val activeModeSteps = if (isRunning) runningSteps else walkingSteps

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = if (isRunning) "Running Duration" else "Walking Duration",
                            value = activeModeTime,
                            unit = "",
                            icon = Icons.Default.Timer,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = if (isRunning) "Running Steps" else "Walking Steps",
                            value = String.format("%,d", activeModeSteps),
                            unit = "steps",
                            icon = if (isRunning) Icons.Default.DirectionsRun else Icons.Default.DirectionsWalk,
                            iconTint = EmeraldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "CO2 Emitted",
                            value = "0",
                            unit = "g",
                            subtitle = "Zero tailpipe emission",
                            icon = Icons.Default.Speed,
                            iconTint = EmeraldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "CO2 Saved",
                            value = if (co2SavedGrams >= 1000.0) String.format("%.2f", co2SavedGrams / 1000.0) else String.format("%.0f", co2SavedGrams),
                            unit = if (co2SavedGrams >= 1000.0) "kg" else "g",
                            subtitle = "vs 192g/km car",
                            icon = Icons.Default.Eco,
                            iconTint = EmeraldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "Calories",
                            value = String.format("%.0f", calories),
                            unit = "kcal",
                            icon = Icons.Default.LocalFireDepartment,
                            iconTint = Color(0xFFE65100),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Battery",
                            value = if (startBatteryPct != null) "-${batteryDropPct ?: 0}%" else "Normal",
                            unit = if (startBatteryPct != null) "drop" else "",
                            subtitle = if (startBatteryPct != null) "Started at ${startBatteryPct}%" else null,
                            icon = Icons.Default.BatteryChargingFull,
                            iconTint = EmeraldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 2. Cycling: distance, duration, average speed (and CO2 saved)
                isCycling -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "Duration",
                            value = timeFormatted,
                            unit = "",
                            icon = Icons.Default.Timer,
                            modifier = Modifier.weight(1f)
                        )
                        val avgSpeed = if (durationSeconds > 0) (distanceMeters / 1000.0) / (durationSeconds / 3600.0) else speedKmh
                        MetricCard(
                            title = "Average Speed",
                            value = String.format("%.1f", avgSpeed),
                            unit = "km/h",
                            icon = Icons.Default.Speed,
                            iconTint = MintSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "CO2 Emitted",
                            value = "0",
                            unit = "g",
                            subtitle = "Clean pedal power",
                            icon = Icons.Default.Speed,
                            iconTint = EmeraldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "CO2 Saved",
                            value = if (co2SavedGrams >= 1000.0) String.format("%.2f", co2SavedGrams / 1000.0) else String.format("%.0f", co2SavedGrams),
                            unit = if (co2SavedGrams >= 1000.0) "kg" else "g",
                            subtitle = "vs 192g/km car",
                            icon = Icons.Default.Eco,
                            iconTint = EmeraldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "Calories",
                            value = String.format("%.0f", calories),
                            unit = "kcal",
                            icon = Icons.Default.LocalFireDepartment,
                            iconTint = Color(0xFFE65100),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Current Speed",
                            value = String.format("%.1f", speedKmh),
                            unit = "km/h",
                            icon = Icons.Default.Speed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 3. Motorcycle: distance, duration, average speed and estimated CO2 emissions
                isMotorcycle -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "Duration",
                            value = timeFormatted,
                            unit = "",
                            icon = Icons.Default.Timer,
                            modifier = Modifier.weight(1f)
                        )
                        val avgSpeed = if (durationSeconds > 0) (distanceMeters / 1000.0) / (durationSeconds / 3600.0) else speedKmh
                        MetricCard(
                            title = "Average Speed",
                            value = String.format("%.1f", avgSpeed),
                            unit = "km/h",
                            icon = Icons.Default.Speed,
                            iconTint = MintSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "CO2 Emitted",
                            value = if (co2Grams >= 1000.0) String.format("%.2f", co2Grams / 1000.0) else String.format("%.0f", co2Grams),
                            unit = if (co2Grams >= 1000.0) "kg" else "g",
                            subtitle = "Est. 103 g/km",
                            icon = Icons.Default.Speed,
                            iconTint = Color(0xFFE65100),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "CO2 Saved",
                            value = if (co2SavedGrams >= 1000.0) String.format("%.2f", co2SavedGrams / 1000.0) else String.format("%.0f", co2SavedGrams),
                            unit = if (co2SavedGrams >= 1000.0) "kg" else "g",
                            subtitle = "+89g/km vs Car",
                            icon = Icons.Default.Eco,
                            iconTint = EmeraldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 4. Car: distance, duration, average speed and estimated CO2 emissions
                isCar -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "Duration",
                            value = timeFormatted,
                            unit = "",
                            icon = Icons.Default.Timer,
                            modifier = Modifier.weight(1f)
                        )
                        val avgSpeed = if (durationSeconds > 0) (distanceMeters / 1000.0) / (durationSeconds / 3600.0) else speedKmh
                        MetricCard(
                            title = "Average Speed",
                            value = String.format("%.1f", avgSpeed),
                            unit = "km/h",
                            icon = Icons.Default.Speed,
                            iconTint = MintSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "CO2 Emitted",
                            value = if (co2Grams >= 1000.0) String.format("%.2f", co2Grams / 1000.0) else String.format("%.0f", co2Grams),
                            unit = if (co2Grams >= 1000.0) "kg" else "g",
                            subtitle = "Est. 192 g/km",
                            icon = Icons.Default.Speed,
                            iconTint = AccentRed,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Baseline Rate",
                            value = "192",
                            unit = "g/km",
                            subtitle = "Standard Petrol",
                            icon = Icons.Default.Eco,
                            iconTint = TextMuted,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 5. Bus / Public Transport: distance, duration, average speed and estimated CO2 emissions
                isTransit -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "Duration",
                            value = timeFormatted,
                            unit = "",
                            icon = Icons.Default.Timer,
                            modifier = Modifier.weight(1f)
                        )
                        val avgSpeed = if (durationSeconds > 0) (distanceMeters / 1000.0) / (durationSeconds / 3600.0) else speedKmh
                        MetricCard(
                            title = "Average Speed",
                            value = String.format("%.1f", avgSpeed),
                            unit = "km/h",
                            icon = Icons.Default.Speed,
                            iconTint = MintSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "CO2 Emitted",
                            value = if (co2Grams >= 1000.0) String.format("%.2f", co2Grams / 1000.0) else String.format("%.0f", co2Grams),
                            unit = if (co2Grams >= 1000.0) "kg" else "g",
                            subtitle = "Est. 68 g/km rider",
                            icon = Icons.Default.Speed,
                            iconTint = EmeraldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "CO2 Saved",
                            value = if (co2SavedGrams >= 1000.0) String.format("%.2f", co2SavedGrams / 1000.0) else String.format("%.0f", co2SavedGrams),
                            unit = if (co2SavedGrams >= 1000.0) "kg" else "g",
                            subtitle = "+124g/km vs Car",
                            icon = Icons.Default.Eco,
                            iconTint = EmeraldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 6. Stationary / Default
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "Status",
                            value = "Stationary",
                            unit = "",
                            icon = Icons.Default.LocationOn,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Emissions",
                            value = "0",
                            unit = "g",
                            icon = Icons.Default.Speed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Graceful Permission Denial Recovery Banner
            if (!isTracking && permissionDeniedOnce && !hasLocationPermission) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Location Permission Required",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "CarbonPrint cannot record GPS distance or compute carbon emissions without location access. You can grant access below or open system settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { permissionLauncher.launch(getRequiredPermissions()) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Text("Grant Permission", fontSize = 13.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    )
                                    context.startActivity(intent)
                                }
                            ) {
                                Text("App Settings", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Start / Stop Primary Button
            if (!isTracking) {
                if (isStill) {
                    Button(
                        onClick = { /* Stationary mode: do not create unnecessary trips */ },
                        enabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = Color(0xFFE0E0E0),
                            disabledContentColor = TextMuted
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = TextMuted
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STATIONARY (NO TRIP CREATED)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            if (hasLocationPermission) {
                                viewModel.startTracking(context)
                            } else {
                                showPermissionRationaleDialog = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start Tracking",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "START TRACKING",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            } else {
                Button(
                    onClick = { viewModel.stopTracking(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop Tracking",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "STOP & SAVE TRIP",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
