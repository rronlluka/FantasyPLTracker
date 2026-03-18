package com.fpl.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
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
    val isLive: Boolean = false,  // Visual indicator - only truly live games
    val hasLivePoints: Boolean = false,  // True if we should count points from live API (live OR just finished)
    val hasPlayed: Boolean = false
)

data class ManagerFormationUiState(
    val isLoading: Boolean = false,
    val managerPicks: ManagerPicks? = null,
    val managerHistory: ManagerHistory? = null,
    val bootstrapData: BootstrapData? = null,
    val liveGameweek: LiveGameweek? = null,
    val fixtures: List<Fixture> = emptyList(),
    val playersWithDetails: List<PlayerWithDetails> = emptyList(),
    val provisionalBonus: Map<Int, Int> = emptyMap(),
    val error: String? = null
)

class ManagerFormationViewModel : ViewModel() {
    private val repository = FPLRepository()
    
    private val _uiState = MutableStateFlow(ManagerFormationUiState())
    val uiState: StateFlow<ManagerFormationUiState> = _uiState.asStateFlow()

    fun loadManagerFormation(managerId: Long, eventId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // Load bootstrap data
            val bootstrapResult = repository.getBootstrapData()
            
            // Load manager picks
            val picksResult = repository.getManagerPicks(managerId, eventId)
            // Load manager history for chips
            val historyResult = repository.getManagerHistory(managerId)
            
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
                    
                    // Determine game state
                    val isTrulyLive = fixture?.started == true &&
                        fixture.finished == false &&
                        fixture.finishedProvisional == false

                    // isLive = visual indicator (only truly live games)
                    // hasLivePoints = should count points from live API:
                    //   - live or just finished (current GW): fixture started but not finished by FPL
                    //   - historical GW: fixture is finished, so use live API data (which is already
                    //     loaded for the correct eventId) whenever liveStats are available
                    val isLive = isTrulyLive
                    val hasLivePoints = (fixture?.started == true && fixture.finished == false)
                        || (fixture?.finished == true && liveStats != null)
                    val hasPlayed = fixture?.finished == true
                    
                    PlayerWithDetails(
                        pick = pick,
                        player = player,
                        liveStats = liveStats,
                        team = team,
                        fixture = fixture,
                        opponentTeam = opponentTeam,
                        isLive = isLive,
                        hasLivePoints = hasLivePoints,
                        hasPlayed = hasPlayed
                    )
                }
                
                // Calculate provisional bonus for fixtures with live data
                val provisionalBonus = mutableMapOf<Int, Int>()
                if (live != null) {
                    val liveFixtures = fixtures.filter { 
                        it.started == true && it.finished == false && it.finishedProvisional == false
                    }
                    
                    val fixturesNeedingBonus = fixtures.filter {
                        it.started == true && it.finished == false
                    }
                    
                    Log.d("FormationBonus", "Fixtures with live data: ${fixturesNeedingBonus.size} (Truly live: ${liveFixtures.size})")
                    
                    fixturesNeedingBonus.forEach { fixture ->
                        val homeTeam = bootstrap.teams.find { it.id == fixture.teamH }
                        val awayTeam = bootstrap.teams.find { it.id == fixture.teamA }
                        
                        Log.d("BonusCalc", "")
                        Log.d("BonusCalc", "🔴 LIVE FIXTURE ${fixture.id}: ${homeTeam?.shortName} vs ${awayTeam?.shortName} (${fixture.minutes}')")
                        
                        val fixturePlayersBps = live.elements
                            .filter { liveEl ->
                                val player = bootstrap.elements.find { it.id == liveEl.id }
                                player != null && (player.team == fixture.teamH || player.team == fixture.teamA)
                            }
                            .sortedByDescending { it.stats.bps }
                        
                        Log.d("BonusCalc", "Players in fixture: ${fixturePlayersBps.size}")
                        fixturePlayersBps.take(5).forEach { liveEl ->
                            val player = bootstrap.elements.find { it.id == liveEl.id }
                            val team = bootstrap.teams.find { it.id == player?.team }
                            Log.d("BonusCalc", "  ${player?.webName} (${team?.shortName}): BPS=${liveEl.stats.bps}, Current Bonus=${liveEl.stats.bonus}, Points=${liveEl.stats.totalPoints}")
                        }
                        
                        if (fixturePlayersBps.isNotEmpty()) {
                            val fixtureBonus = BonusPointsCalculator.calculateProvisionalBonus(
                                fixturePlayersBps,
                                fixture.id
                            )
                            
                            if (fixtureBonus.isNotEmpty()) {
                                Log.d("BonusCalc", "PROVISIONAL BONUS AWARDS:")
                                fixtureBonus.forEach { (playerId, bonus) ->
                                    val player = bootstrap.elements.find { it.id == playerId }
                                    val bps = fixturePlayersBps.find { it.id == playerId }?.stats?.bps
                                    Log.d("BonusCalc", "  ⭐ ${player?.webName} (BPS=$bps): +$bonus bonus points")
                                }
                                provisionalBonus.putAll(fixtureBonus)
                            } else {
                                Log.d("BonusCalc", "No provisional bonus calculated")
                            }
                        }
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    managerPicks = picks,
                    managerHistory = historyResult.getOrNull(),
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
