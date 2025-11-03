package com.fpl.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fpl.tracker.data.api.RetrofitInstance
import com.fpl.tracker.data.models.Fixture
import com.fpl.tracker.data.models.Team
import com.fpl.tracker.data.repository.FPLRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MatchesUiState(
    val isLoading: Boolean = false,
    val fixtures: List<Fixture> = emptyList(),
    val teams: List<Team> = emptyList(),
    val currentEvent: Int = 1,
    val error: String? = null
)

class MatchesViewModel : ViewModel() {
    private val repository = FPLRepository(RetrofitInstance.api)
    
    private val _uiState = MutableStateFlow(MatchesUiState())
    val uiState: StateFlow<MatchesUiState> = _uiState.asStateFlow()

    fun loadCurrentGameweekFixtures() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // Load bootstrap to get current event and teams
            val bootstrapResult = repository.getBootstrapData()
            val currentEvent = bootstrapResult.getOrNull()?.events?.find { it.isCurrent }?.id ?: 1
            val teams = bootstrapResult.getOrNull()?.teams ?: emptyList()
            
            // Load fixtures
            val fixturesResult = repository.getFixturesByEvent(currentEvent)
            
            if (fixturesResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = fixturesResult.exceptionOrNull()?.message ?: "Unknown error"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    fixtures = fixturesResult.getOrNull() ?: emptyList(),
                    teams = teams,
                    currentEvent = currentEvent
                )
            }
        }
    }
}

