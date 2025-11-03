package com.fpl.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.fpl.tracker.data.api.RetrofitInstance
import com.fpl.tracker.data.models.*
import com.fpl.tracker.data.repository.FPLRepository
import com.fpl.tracker.utils.BonusPointsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayerWithDetails(
    val pick: Pick,
    val player: Player,
    val liveStats: LiveElement?,
    val team: Team,
    val fixture: Fixture? = null,
    val opponentTeam: Team? = null,
    val isLive: Boolean = false,
    val hasPlayed: Boolean = false
)

data class ManagerFormationUiState(
    val isLoading: Boolean = false,
    val managerPicks: ManagerPicks? = null,
    val bootstrapData: BootstrapData? = null,
    val liveGameweek: LiveGameweek? = null,
    val fixtures: List<Fixture> = emptyList(),
    val playersWithDetails: List<PlayerWithDetails> = emptyList(),
    val provisionalBonus: Map<Int, Int> = emptyMap(),
    val error: String? = null
)

class ManagerFormationViewModel : ViewModel() {
    private val repository = FPLRepository(RetrofitInstance.api)
    
    private val _uiState = MutableStateFlow(ManagerFormationUiState())
    val uiState: StateFlow<ManagerFormationUiState> = _uiState.asStateFlow()

    fun loadManagerFormation(managerId: Long, eventId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // Load bootstrap data
            val bootstrapResult = repository.getBootstrapData()
            
            // Load manager picks
            val picksResult = repository.getManagerPicks(managerId, eventId)
            
            // Load live gameweek data
            val liveResult = repository.getLiveGameweek(eventId)
            
            // Load fixtures for the gameweek
            val fixturesResult = repository.getFixturesByEvent(eventId)
            
            if (picksResult.isFailure || bootstrapResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = picksResult.exceptionOrNull()?.message ?: 
                           bootstrapResult.exceptionOrNull()?.message ?: "Unknown error"
                )
            } else {
                val picks = picksResult.getOrNull()!!
                val bootstrap = bootstrapResult.getOrNull()!!
                val live = liveResult.getOrNull()
                val fixtures = fixturesResult.getOrNull() ?: emptyList()
                
                // Create player details list with fixture info
                val playersWithDetails = picks.picks.map { pick ->
                    val player = bootstrap.elements.find { it.id == pick.element }!!
                    val team = bootstrap.teams.find { it.id == player.team }!!
                    val liveStats = live?.elements?.find { it.id == pick.element }
                    
                    // Find player's fixture
                    val fixture = fixtures.find { 
                        it.teamH == player.team || it.teamA == player.team 
                    }
                    
                    // Get opponent team
                    val opponentTeamId = when {
                        fixture?.teamH == player.team -> fixture.teamA
                        fixture?.teamA == player.team -> fixture.teamH
                        else -> null
                    }
                    val opponentTeam = opponentTeamId?.let { 
                        bootstrap.teams.find { it.id == opponentTeamId } 
                    }
                    
                    // Determine if player is live or has played
                    val isLive = fixture?.started == true && fixture.finished == false
                    val hasPlayed = fixture?.finished == true
                    
                    PlayerWithDetails(
                        pick = pick,
                        player = player,
                        liveStats = liveStats,
                        team = team,
                        fixture = fixture,
                        opponentTeam = opponentTeam,
                        isLive = isLive,
                        hasPlayed = hasPlayed
                    )
                }
                
                // Calculate provisional bonus for live fixtures
                val provisionalBonus = mutableMapOf<Int, Int>()
                if (live != null) {
                    fixtures.filter { it.started == true && it.finished == false }.forEach { fixture ->
                        val fixturePlayersBps = live.elements
                            .filter { liveEl ->
                                val player = bootstrap.elements.find { it.id == liveEl.id }
                                player != null && (player.team == fixture.teamH || player.team == fixture.teamA)
                            }
                            .sortedByDescending { it.stats.bps }
                        
                        if (fixturePlayersBps.isNotEmpty()) {
                            val fixtureBonus = BonusPointsCalculator.calculateProvisionalBonus(
                                fixturePlayersBps,
                                fixture.id
                            )
                            provisionalBonus.putAll(fixtureBonus)
                            
                            Log.d("FormationBonus", "Fixture ${fixture.id} provisional bonus: $fixtureBonus")
                        }
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    managerPicks = picks,
                    bootstrapData = bootstrap,
                    liveGameweek = live,
                    fixtures = fixtures,
                    playersWithDetails = playersWithDetails,
                    provisionalBonus = provisionalBonus
                )
            }
        }
    }
}

