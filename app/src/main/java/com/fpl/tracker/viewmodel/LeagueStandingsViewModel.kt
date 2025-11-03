package com.fpl.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fpl.tracker.data.api.RetrofitInstance
import com.fpl.tracker.data.models.*
import com.fpl.tracker.data.repository.FPLRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ManagerLiveData(
    val managerId: Int,
    val inPlay: Int,
    val toStart: Int
)

data class LeagueStandingsUiState(
    val isLoading: Boolean = false,
    val leagueStandings: LeagueStandings? = null,
    val currentEvent: Int = 1,
    val managerLiveData: Map<Int, ManagerLiveData> = emptyMap(),
    val error: String? = null
)

class LeagueStandingsViewModel : ViewModel() {
    private val repository = FPLRepository(RetrofitInstance.api)
    
    private val _uiState = MutableStateFlow(LeagueStandingsUiState())
    val uiState: StateFlow<LeagueStandingsUiState> = _uiState.asStateFlow()

    suspend fun calculateLeaguePlayerStats(
        playerId: Int,
        leagueStandings: LeagueStandings,
        currentEvent: Int,
        bootstrapData: BootstrapData
    ): LeaguePlayerStats {
        var startsCount = 0
        var benchCount = 0
        var captainCount = 0
        var viceCaptainCount = 0
        val captainedBy = mutableListOf<String>()
        val startedBy = mutableListOf<String>()
        val benchedBy = mutableListOf<String>()
        
        val managersToCheck = leagueStandings.standings.results.take(50)
        
        managersToCheck.forEach { standing ->
            try {
                val picksResult = repository.getManagerPicks(standing.entry.toLong(), currentEvent)
                val picks = picksResult.getOrNull()
                
                picks?.picks?.forEach { pick ->
                    if (pick.element == playerId) {
                        if (pick.position <= 11) {
                            startsCount++
                            startedBy.add(standing.entryName)
                            
                            if (pick.isCaptain) {
                                captainCount++
                                captainedBy.add(standing.entryName)
                            }
                            if (pick.isViceCaptain) {
                                viceCaptainCount++
                            }
                        } else {
                            benchCount++
                            benchedBy.add(standing.entryName)
                        }
                    }
                }
            } catch (e: Exception) {
                // Skip on error
            }
        }
        
        val totalManagers = managersToCheck.size
        val startsPercentage = (startsCount.toDouble() / totalManagers) * 100
        val ownedPercentage = ((startsCount + benchCount).toDouble() / totalManagers) * 100
        
        return LeaguePlayerStats(
            playerId = playerId,
            startsCount = startsCount,
            benchCount = benchCount,
            captainCount = captainCount,
            viceCaptainCount = viceCaptainCount,
            startsPercentage = startsPercentage,
            ownedPercentage = ownedPercentage,
            captainedBy = captainedBy,
            startedBy = startedBy,
            benchedBy = benchedBy
        )
    }
    
    fun loadLeagueStandings(leagueId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // Load bootstrap to get current event
            val bootstrapResult = repository.getBootstrapData()
            val currentEvent = bootstrapResult.getOrNull()?.events?.find { it.isCurrent }?.id ?: 1
            
            val result = repository.getLeagueStandings(leagueId)
            
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Unknown error"
                )
            } else {
                val leagueStandings = result.getOrNull()!!
                
                // Load fixtures for current gameweek
                val fixturesResult = repository.getFixturesByEvent(currentEvent)
                val fixtures = fixturesResult.getOrNull() ?: emptyList()
                
                // Load live data for each manager (limit to top 50 for performance)
                val managersToCheck = leagueStandings.standings.results.take(50)
                val managerLiveDataMap = mutableMapOf<Int, ManagerLiveData>()
                
                val deferredResults = managersToCheck.map { standing ->
                    async {
                        try {
                            val picksResult = repository.getManagerPicks(standing.entry.toLong(), currentEvent)
                            val picks = picksResult.getOrNull()
                            
                            if (picks != null) {
                                val bootstrap = bootstrapResult.getOrNull()
                                var inPlay = 0
                                var toStart = 0
                                
                                // Check each player's fixture status
                                picks.picks.forEach { pick ->
                                    val player = bootstrap?.elements?.find { it.id == pick.element }
                                    if (player != null) {
                                        val playerFixture = fixtures.find { 
                                            it.teamH == player.team || it.teamA == player.team 
                                        }
                                        
                                        when {
                                            playerFixture?.started == true && playerFixture.finished == false -> inPlay++
                                            playerFixture?.started == false -> toStart++
                                        }
                                    }
                                }
                                
                                ManagerLiveData(standing.entry, inPlay, toStart)
                            } else {
                                ManagerLiveData(standing.entry, 0, 0)
                            }
                        } catch (e: Exception) {
                            ManagerLiveData(standing.entry, 0, 0)
                        }
                    }
                }
                
                // Wait for all requests to complete
                val results = deferredResults.awaitAll()
                results.forEach { data ->
                    managerLiveDataMap[data.managerId] = data
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    leagueStandings = leagueStandings,
                    currentEvent = currentEvent,
                    managerLiveData = managerLiveDataMap
                )
            }
        }
    }
}

