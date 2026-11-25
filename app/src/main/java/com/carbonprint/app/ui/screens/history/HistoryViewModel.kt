package com.carbonprint.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carbonprint.app.data.local.entity.TripEntity
import com.carbonprint.app.data.repository.TripRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val tripRepository: TripRepository
) : ViewModel() {

    val trips: StateFlow<List<TripEntity>> = tripRepository.allTripsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteTrip(trip: TripEntity) {
        viewModelScope.launch {
            tripRepository.deleteTrip(trip)
        }
    }
}
