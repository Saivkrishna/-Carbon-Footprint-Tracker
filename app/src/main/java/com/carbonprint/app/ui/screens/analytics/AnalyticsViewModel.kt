package com.carbonprint.app.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carbonprint.app.data.local.entity.DailySummaryEntity
import com.carbonprint.app.data.local.entity.TripEntity
import com.carbonprint.app.data.repository.AnalyticsRepository
import com.carbonprint.app.data.repository.TripRepository
import com.carbonprint.app.domain.battery.BatteryMonitor
import com.carbonprint.app.domain.battery.BatteryUsageSummary
import com.carbonprint.app.domain.model.TransportMode
import com.carbonprint.app.ui.components.BarData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnalyticsViewModel(
    private val analyticsRepository: AnalyticsRepository,
    private val tripRepository: TripRepository
) : ViewModel() {

    val recentSummaries: StateFlow<List<DailySummaryEntity>> =
        analyticsRepository.getRecentSummariesFlow(7)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTrips: StateFlow<List<TripEntity>> = tripRepository.allTripsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chartData: StateFlow<List<BarData>> = recentSummaries.map { summaries ->
        summaries.reversed().map { summary ->
            val dayName = try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(summary.dateString)
                SimpleDateFormat("EEE", Locale.getDefault()).format(date ?: Date())
            } catch (e: Exception) {
                summary.dateString.takeLast(2)
            }
            BarData(label = dayName, value = summary.totalCo2Grams.toFloat())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val modeBreakdown: StateFlow<Map<TransportMode, Double>> = allTrips.map { trips ->
        analyticsRepository.calculateModeBreakdown(trips)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val batterySummary: StateFlow<BatteryUsageSummary> = allTrips.map { trips ->
        BatteryMonitor.computeBatterySummary(trips)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BatteryUsageSummary())
}
