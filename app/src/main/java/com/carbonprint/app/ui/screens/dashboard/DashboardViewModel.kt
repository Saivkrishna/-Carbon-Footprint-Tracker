package com.carbonprint.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carbonprint.app.data.local.entity.DailySummaryEntity
import com.carbonprint.app.data.local.entity.GoalEntity
import com.carbonprint.app.data.local.entity.TripEntity
import com.carbonprint.app.data.repository.AnalyticsRepository
import com.carbonprint.app.data.repository.GoalRepository
import com.carbonprint.app.data.repository.TripRepository
import com.carbonprint.app.domain.model.TransportMode
import com.carbonprint.app.service.TripTrackingService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val tripRepository: TripRepository,
    private val goalRepository: GoalRepository,
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    val isTracking: StateFlow<Boolean> = TripTrackingService.isTracking

    val currentMode: StateFlow<TransportMode> = TripTrackingService.currentModeState

    val activeGoal: StateFlow<GoalEntity?> = goalRepository.activeGoalFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todaySummary: StateFlow<DailySummaryEntity?> =
        analyticsRepository.getDailySummaryFlow(analyticsRepository.getTodayDateString())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentTrips: StateFlow<List<TripEntity>> = tripRepository.allTripsFlow
        .map { it.take(3) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            goalRepository.updateCurrentWeekGoalProgress()
        }
    }
}
