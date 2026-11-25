package com.carbonprint.app.ui.screens.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carbonprint.app.data.local.entity.GoalEntity
import com.carbonprint.app.data.repository.GoalRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GoalsViewModel(
    private val goalRepository: GoalRepository
) : ViewModel() {

    val activeGoal: StateFlow<GoalEntity?> = goalRepository.activeGoalFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateTargetGoal(targetKg: Double) {
        viewModelScope.launch {
            goalRepository.setCustomWeeklyGoal(targetKg)
        }
    }
}
