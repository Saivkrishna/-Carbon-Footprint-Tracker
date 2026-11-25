package com.carbonprint.app.ui.screens.history

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carbonprint.app.data.local.entity.TripEntity
import com.carbonprint.app.ui.components.ModeChip
import com.carbonprint.app.ui.theme.AccentRed
import com.carbonprint.app.ui.theme.EmeraldDark
import com.carbonprint.app.ui.theme.EmeraldPrimary
import com.carbonprint.app.ui.theme.TextMuted
import com.carbonprint.app.ui.theme.TextPrimary
import com.carbonprint.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onTripClick: (Long) -> Unit
) {
    val trips by viewModel.trips.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Trip History",
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
        if (trips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No recorded trips yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Start a trip in Live Tracking or allow background activity detection to log your journeys.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "${trips.size} journeys recorded",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(trips, key = { it.id }) { trip ->
                    TripHistoryCard(
                        trip = trip,
                        onClick = { onTripClick(trip.id) },
                        onDelete = { viewModel.deleteTrip(trip) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun TripHistoryCard(
    trip: TripEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatted = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        .format(Date(trip.startTime))
    val distanceKm = String.format("%.2f", trip.distanceMeters / 1000.0)
    val mins = trip.durationSeconds / 60
    val secs = trip.durationSeconds % 60
    val timeFormatted = "${mins}m ${secs}s"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModeChip(mode = trip.mode, subMode = trip.subMode)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dateFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isWalkOrRun = trip.mode == com.carbonprint.app.domain.model.TransportMode.WALKING ||
                        trip.mode == com.carbonprint.app.domain.model.TransportMode.RUNNING
                val stepsText = when {
                    trip.walkingSteps > 0 && trip.runningSteps > 0 ->
                        "${String.format("%,d", trip.walkingSteps)} walk • ${String.format("%,d", trip.runningSteps)} run"
                    trip.mode == com.carbonprint.app.domain.model.TransportMode.RUNNING || trip.runningSteps > 0 ->
                        "${String.format("%,d", if (trip.runningSteps > 0) trip.runningSteps else trip.stepCount)} run steps"
                    isWalkOrRun ->
                        "${String.format("%,d", if (trip.walkingSteps > 0) trip.walkingSteps else trip.stepCount)} walk steps"
                    else -> null
                }
                val detailSubtitle = if (stepsText != null) {
                    "$stepsText • $timeFormatted"
                } else {
                    "${String.format("%.1f", trip.averageSpeedKmh)} km/h • $timeFormatted"
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$distanceKm km",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = detailSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    if (trip.batteryDropPct != null) {
                        Text(
                            text = "Battery: -${trip.batteryDropPct}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (trip.co2Grams >= 1000.0) String.format("%.2f kg", trip.co2Grams / 1000.0) else "${String.format("%.0f", trip.co2Grams)}g CO2",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (trip.mode.isZeroEmission) EmeraldPrimary else AccentRed
                    )
                    if (trip.co2SavedGrams > 0.0) {
                        Text(
                            text = if (trip.co2SavedGrams >= 1000.0) "+${String.format("%.2f", trip.co2SavedGrams / 1000.0)} kg saved" else "+${String.format("%.0f", trip.co2SavedGrams)}g saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "CO2 Emitted",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted
                )
            }
        }
    }
}
