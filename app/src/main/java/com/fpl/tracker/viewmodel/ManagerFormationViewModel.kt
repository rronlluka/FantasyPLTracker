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
    val isLive: Boolean = false,  // Visual indicator - only truly live games
    val hasLivePoints: Boolean = false,  // True if we should count points from live API (live OR just finished)
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
                    val isTrulyLive = fixture?.started == true && 
                        fixture.finished == false && 
                        fixture.finishedProvisional == false
                    
                    // Check if just finished (within 3 hours)
                    val isJustFinished = if (fixture != null && (fixture.finished == true || fixture.finishedProvisional == true)) {
                        try {
                            val kickoffTime = fixture.kickoffTime?.let { 
                                java.time.Instant.parse(it) 
                            }
                            if (kickoffTime != null) {
                                val now = java.time.Instant.now()
                                val gameEndTime = kickoffTime.plusSeconds(90 * 60 + 45 * 60) // ~135 mins for full game
                                val hoursSinceEnd = java.time.Duration.between(gameEndTime, now).toHours()
                                hoursSinceEnd < 3
                            } else {
                                false
                            }
                        } catch (e: Exception) {
                            false
                        }
                    } else {
                        false
                    }
                    
                    // isLive = visual indicator (only truly live games)
                    // hasLivePoints = should count points from live API (truly live OR just finished within 3 hours)
                    val isLive = isTrulyLive
                    val hasLivePoints = isTrulyLive || isJustFinished
                    val hasPlayed = fixture?.finished == true && !isJustFinished
                    
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
                
                // Calculate provisional bonus for fixtures needing it (live OR just finished)
                val provisionalBonus = mutableMapOf<Int, Int>()
                if (live != null) {
                    val liveFixtures = fixtures.filter { 
                        it.started == true && it.finished == false && it.finishedProvisional == false
                    }
                    
                    // Just finished fixtures (within 3 hours)
                    val justFinishedFixtures = fixtures.filter { fixture ->
                        if (fixture.finished == true || fixture.finishedProvisional == true) {
                            try {
                                val kickoffTime = fixture.kickoffTime?.let { java.time.Instant.parse(it) }
                                if (kickoffTime != null) {
                                    val now = java.time.Instant.now()
                                    val gameEndTime = kickoffTime.plusSeconds(90 * 60 + 45 * 60)
                                    val hoursSinceEnd = java.time.Duration.between(gameEndTime, now).toHours()
                                    hoursSinceEnd < 3
                                } else {
                                    false
                                }
                            } catch (e: Exception) {
                                false
                            }
                        } else {
                            false
                        }
                    }
                    
                    val fixturesNeedingBonus = liveFixtures + justFinishedFixtures
                    
                    Log.d("FormationBonus", "Fixtures needing bonus: ${fixturesNeedingBonus.size} (Live: ${liveFixtures.size}, Just finished: ${justFinishedFixtures.size})")
                    
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

