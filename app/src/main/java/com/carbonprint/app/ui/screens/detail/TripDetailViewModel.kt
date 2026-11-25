package com.carbonprint.app.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carbonprint.app.data.local.entity.TripEntity
import com.carbonprint.app.data.local.entity.WaypointEntity
import com.carbonprint.app.data.repository.TripRepository
import com.carbonprint.app.domain.location.ProcessedRoute
import com.carbonprint.app.domain.location.RouteProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TripDetailViewModel(
    private val tripRepository: TripRepository,
    private val tripId: Long
) : ViewModel() {

    private val _trip = MutableStateFlow<TripEntity?>(null)
    val trip: StateFlow<TripEntity?> = _trip.asStateFlow()

    private val _waypoints = MutableStateFlow<List<WaypointEntity>>(emptyList())
    val waypoints: StateFlow<List<WaypointEntity>> = _waypoints.asStateFlow()

    private val _processedRoute = MutableStateFlow(
        ProcessedRoute(
            points = emptyList(),
            startPoint = null,
            destinationPoint = null,
            boundingBox = null,
            totalWaypointsCount = 0
        )
    )
    val processedRoute: StateFlow<ProcessedRoute> = _processedRoute.asStateFlow()

    init {
        loadTripDetails()
    }

    private fun loadTripDetails() {
        viewModelScope.launch {
            _trip.value = tripRepository.getTripById(tripId)
            val loadedWaypoints = tripRepository.getWaypointsForTrip(tripId)
            _waypoints.value = loadedWaypoints
            _processedRoute.value = RouteProcessor.processWaypoints(loadedWaypoints)
        }
    }

    fun deleteTrip(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _trip.value?.let {
                tripRepository.deleteTrip(it)
                onDeleted()
            }
        }
    }
}
