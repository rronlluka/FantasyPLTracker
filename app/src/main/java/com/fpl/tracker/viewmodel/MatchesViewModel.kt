package com.fpl.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fpl.tracker.data.api.RetrofitInstance
import com.fpl.tracker.data.models.Fixture
import com.fpl.tracker.data.models.LiveElement
import com.fpl.tracker.data.models.Player
import com.fpl.tracker.data.models.Team
import com.fpl.tracker.data.repository.FPLRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MatchesUiState(
    val isLoading: Boolean = false,
    val fixtures: List<Fixture> = emptyList(),
    val teams: List<Team> = emptyList(),
    val players: List<Player> = emptyList(),
    val liveElements: List<LiveElement> = emptyList(),
    val currentEvent: Int = 1,
    val error: String? = null,
    val hasLiveGames: Boolean = false
)

class MatchesViewModel : ViewModel() {
    private val repository = FPLRepository(RetrofitInstance.api)

    private val _uiState = MutableStateFlow(MatchesUiState())
    val uiState: StateFlow<MatchesUiState> = _uiState.asStateFlow()

    private var liveRefreshJob: Job? = null

    fun loadCurrentGameweekFixtures() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val bootstrapResult = repository.getBootstrapData()
            val bootstrap = bootstrapResult.getOrNull()

            if (bootstrap == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = bootstrapResult.exceptionOrNull()?.message ?: "Failed to load data"
                )
                return@launch
            }

            val currentEvent = bootstrap.events.find { it.isCurrent }?.id ?: 1
            val teams = bootstrap.teams
            val players = bootstrap.elements

            val fixturesResult = repository.getFixturesByEvent(currentEvent)
            val fixtures = fixturesResult.getOrNull()

            if (fixtures == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = fixturesResult.exceptionOrNull()?.message ?: "Failed to load fixtures"
                )
                return@launch
            }

            val hasLiveGames = fixtures.any { it.started == true && !it.finished }

            // Fetch live data if any games are in progress
            val liveElements = if (hasLiveGames || fixtures.any { it.started == true }) {
                repository.getLiveGameweek(currentEvent).getOrNull()?.elements ?: emptyList()
            } else {
                emptyList()
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                fixtures = fixtures,
                teams = teams,
                players = players,
                liveElements = liveElements,
                currentEvent = currentEvent,
                hasLiveGames = hasLiveGames
            )

            // Start auto-refresh if there are live games
            if (hasLiveGames) {
                startLiveRefresh(currentEvent)
            }
        }
    }

    private fun startLiveRefresh(eventId: Int) {
        liveRefreshJob?.cancel()
        liveRefreshJob = viewModelScope.launch {
            while (true) {
                delay(30_000L) // refresh every 30 seconds
                refreshLiveData(eventId)
            }
        }
    }

    private suspend fun refreshLiveData(eventId: Int) {
        val fixturesResult = repository.getFixturesByEvent(eventId)
        val fixtures = fixturesResult.getOrNull() ?: return

        val hasLiveGames = fixtures.any { it.started == true && !it.finished }

        val liveElements = if (hasLiveGames || fixtures.any { it.started == true }) {
            repository.getLiveGameweek(eventId).getOrNull()?.elements ?: emptyList()
        } else {
            emptyList()
        }

        _uiState.value = _uiState.value.copy(
            fixtures = fixtures,
            liveElements = liveElements,
            hasLiveGames = hasLiveGames
        )

        // Stop refresh loop if no more live games
        if (!hasLiveGames) {
            liveRefreshJob?.cancel()
            liveRefreshJob = null
        }
    }

    fun refreshFixtures() {
        val currentEvent = _uiState.value.currentEvent
        liveRefreshJob?.cancel()
        liveRefreshJob = null
        loadCurrentGameweekFixtures()
    }

    override fun onCleared() {
        super.onCleared()
        liveRefreshJob?.cancel()
    }
}
