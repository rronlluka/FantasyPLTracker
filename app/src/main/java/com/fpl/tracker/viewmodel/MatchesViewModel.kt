package com.fpl.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fpl.tracker.data.models.*
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
    val events: List<Event> = emptyList(),
    val error: String? = null,
    val hasLiveGames: Boolean = false
)

class MatchesViewModel : ViewModel() {
    private val repository = FPLRepository()

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

            val events = bootstrap.events
            val defaultEvent = events.find { it.isCurrent }?.id ?: events.lastOrNull()?.id ?: 1

            _uiState.value = _uiState.value.copy(
                events = events,
                teams = teams,
                players = players
            )

            loadFixturesForEvent(defaultEvent)
        }
    }

    fun selectGameweek(eventId: Int) {
        if (_uiState.value.currentEvent == eventId) return
        liveRefreshJob?.cancel()
        liveRefreshJob = null
        viewModelScope.launch {
            loadFixturesForEvent(eventId)
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

    private suspend fun loadFixturesForEvent(eventId: Int) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        val fixturesResult = repository.getFixturesByEvent(eventId)
        val fixtures = fixturesResult.getOrNull()

        if (fixtures == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = fixturesResult.exceptionOrNull()?.message ?: "Failed to load fixtures"
            )
            return
        }

        val hasLiveGames = fixtures.any { it.started == true && !it.finished && !it.finishedProvisional }
        val liveElements = if (hasLiveGames || fixtures.any { it.started == true }) {
            repository.getLiveGameweek(eventId).getOrNull()?.elements ?: emptyList()
        } else {
            emptyList()
        }

        liveRefreshJob?.cancel()
        liveRefreshJob = null

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            fixtures = fixtures,
            liveElements = liveElements,
            currentEvent = eventId,
            hasLiveGames = hasLiveGames
        )

        if (hasLiveGames) {
            startLiveRefresh(eventId)
        }
    }

    private suspend fun refreshLiveData(eventId: Int) {
        val fixturesResult = repository.getFixturesByEvent(eventId)
        val fixtures = fixturesResult.getOrNull()

        if (fixtures == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = fixturesResult.exceptionOrNull()?.message ?: "Failed to load fixtures"
            )
            return
        }

        val hasLiveGames = fixtures.any { it.started == true && !it.finished && !it.finishedProvisional }
        val liveElements = if (hasLiveGames || fixtures.any { it.started == true }) {
            repository.getLiveGameweek(eventId).getOrNull()?.elements ?: emptyList()
        } else {
            emptyList()
        }

        liveRefreshJob?.cancel()
        liveRefreshJob = null

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            fixtures = fixtures,
            liveElements = liveElements,
            currentEvent = eventId,
            hasLiveGames = hasLiveGames
        )

        if (hasLiveGames) {
            startLiveRefresh(eventId)
        }
    }

    fun refreshFixtures() {
        val currentEvent = _uiState.value.currentEvent
        liveRefreshJob?.cancel()
        liveRefreshJob = null
        viewModelScope.launch {
            loadFixturesForEvent(currentEvent)
        }
    }

    override fun onCleared() {
        super.onCleared()
        liveRefreshJob?.cancel()
    }
}
