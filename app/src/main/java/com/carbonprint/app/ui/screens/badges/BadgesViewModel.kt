package com.carbonprint.app.ui.screens.badges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carbonprint.app.data.local.entity.BadgeEntity
import com.carbonprint.app.data.repository.BadgeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class BadgesViewModel(
    private val badgeRepository: BadgeRepository
) : ViewModel() {

    val badges: StateFlow<List<BadgeEntity>> = badgeRepository.allBadgesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
