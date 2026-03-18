package com.fpl.tracker.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fpl.tracker.data.models.*
import com.fpl.tracker.data.repository.FPLRepository
import com.fpl.tracker.utils.AutoSubstitutionHelper
import com.fpl.tracker.utils.BonusPointsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ManagerStatsUiState(
    val isLoading: Boolean = false,
    val managerData: ManagerData? = null,
    val managerHistory: ManagerHistory? = null,
    val bootstrapData: BootstrapData? = null,
    val livePoints: Int = 0,  // Points from players currently playing or just finished
    val totalLivePoints: Int = 0,  // GW points + live points
    val inPlay: Int = 0,
    val toStart: Int = 0,
    val hasLiveFixtures: Boolean = false,  // True only when games are actually live
    val error: String? = null
)

class ManagerStatsViewModel : ViewModel() {
    private val repository = FPLRepository()
    
    private val _uiState = MutableStateFlow(ManagerStatsUiState())
    val uiState: StateFlow<ManagerStatsUiState> = _uiState.asStateFlow()

    fun loadManagerData(managerId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // Load bootstrap data
            val bootstrapResult = repository.getBootstrapData()
            val bootstrap = bootstrapResult.getOrNull()
            
            // Load manager data
            val managerResult = repository.getManagerData(managerId)
            
            // Load manager history
            val historyResult = repository.getManagerHistory(managerId)
            
            if (managerResult.isFailure || historyResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = managerResult.exceptionOrNull()?.message ?: 
                           historyResult.exceptionOrNull()?.message ?: "Unknown error"
                )
            } else {
                val managerData = managerResult.getOrNull()!!
                val currentEvent = bootstrap?.events?.find { it.isCurrent }?.id ?: 1
                
                // Load live data for current gameweek
                val fixturesResult = repository.getFixturesByEvent(currentEvent)
                val fixtures = fixturesResult.getOrNull() ?: emptyList()
                
                // Log fixture statuses for debugging
                Log.d("ManagerStats", "=== FIXTURE STATUS DEBUG ===")
                fixtures.forEach { fixture ->
                    val homeTeam = bootstrap?.teams?.find { it.id == fixture.teamH }
                    val awayTeam = bootstrap?.teams?.find { it.id == fixture.teamA }
                    Log.d("ManagerStats", "${homeTeam?.shortName} vs ${awayTeam?.shortName}: " +
                        "Started=${fixture.started}, Finished=${fixture.finished}, " +
                        "FinishedProvisional=${fixture.finishedProvisional}, Minutes=${fixture.minutes}")
                }
                Log.d("ManagerStats", "===========================")
                
                val liveGameweekResult = repository.getLiveGameweek(currentEvent)
                val liveGameweek = liveGameweekResult.getOrNull()
                
                val picksResult = repository.getManagerPicks(managerId, currentEvent)
                val picks = picksResult.getOrNull()
                
                var livePoints = 0
                var inPlay = 0
                var toStart = 0
                
                if (bootstrap != null && picks != null && liveGameweek != null) {
                    // Calculate provisional bonus ONCE for all fixtures needing it
                    val globalProvisionalBonus = mutableMapOf<Int, Int>()
                    
                    // Truly live fixtures (for visual "LIVE" indicator)
                    val liveFixtures = fixtures.filter { 
                        it.started == true && it.finished == false && it.finishedProvisional == false
                    }
                    
                    // All fixtures with live data (started but not finished by FPL)
                    val fixturesNeedingBonus = fixtures.filter {
                        it.started == true && it.finished == false
                    }
                    
                    Log.d("ManagerStats", "Truly live fixtures: ${liveFixtures.size}")
                    Log.d("ManagerStats", "Fixtures with live data: ${fixturesNeedingBonus.size}")
                    fixturesNeedingBonus.forEach { fixture ->
                        val homeTeam = bootstrap.teams.find { it.id == fixture.teamH }
                        val awayTeam = bootstrap.teams.find { it.id == fixture.teamA }
                        val status = if (fixture.finishedProvisional == true) "PROCESSING" else "LIVE"
                        Log.d("ManagerStats", "  - [$status] ${homeTeam?.shortName} vs ${awayTeam?.shortName}")
                    }
                    
                    fixturesNeedingBonus.forEach { fixture ->
                        val fixturePlayersBps = liveGameweek.elements
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
                            globalProvisionalBonus.putAll(fixtureBonus)
                        }
                    }
                    
                    // Use auto-substitution logic to count only effective playing XI
                    val (inPlayCount, toStartCount) = AutoSubstitutionHelper.countPlayersStatus(
                        picks = picks.picks,
                        players = bootstrap.elements,
                        fixtures = fixtures
                    )
                    
                    inPlay = inPlayCount
                    toStart = toStartCount
                    
                    // Calculate live points from players currently playing
                    val effectiveXI = AutoSubstitutionHelper.getEffectivePlayingXI(
                        picks = picks.picks,
                        players = bootstrap.elements,
                        fixtures = fixtures
                    )
                    
                    effectiveXI.forEach { playerId ->
                        val player = bootstrap.elements.find { it.id == playerId }
                        if (player != null) {
                            val fixture = fixtures.find { 
                                it.teamH == player.team || it.teamA == player.team 
                            }
                            
                            // Use live data if game started but not finished by FPL
                            val shouldCountLivePoints = fixture?.started == true && fixture.finished == false
                            
                            if (shouldCountLivePoints) {
                                val liveStats = liveGameweek.elements.find { it.id == playerId }
                                
                                if (liveStats != null) {
                                    var playerPoints = liveStats.stats.totalPoints
                                    
                                    // Add provisional bonus ONLY if API hasn't provided bonus yet
                                    // If API has bonus > 0, it means FPL already calculated it
                                    val currentBonus = liveStats.stats.bonus
                                    val provisionalBonusPoints = globalProvisionalBonus[playerId] ?: 0
                                    
                                    if (currentBonus == 0 && provisionalBonusPoints > 0) {
                                        playerPoints += provisionalBonusPoints
                                        Log.d("ManagerStats", "${player.webName}: +$provisionalBonusPoints provisional bonus")
                                    } else if (currentBonus > 0) {
                                        Log.d("ManagerStats", "${player.webName}: API provided bonus=$currentBonus (no provisional needed)")
                                    }
                                    
                                    // Apply captain multiplier
                                    val pick = picks.picks.find { it.element == playerId }
                                    val multiplier = pick?.multiplier ?: 1
                                    
                                    val pointsWithMultiplier = playerPoints * multiplier
                                    livePoints += pointsWithMultiplier
                                    
                                    Log.d("ManagerStats", "${player.webName}: ${playerPoints}pts × ${multiplier} = $pointsWithMultiplier")
                                }
                            }
                        }
                    }
                    
                    Log.d("ManagerStats", "=== FINAL CALCULATION ===")
                    Log.d("ManagerStats", "Base GW Points (summaryEventPoints): ${managerData.summaryEventPoints}")
                    Log.d("ManagerStats", "Live Points: $livePoints")
                    Log.d("ManagerStats", "Total: ${managerData.summaryEventPoints + livePoints}")
                }
                
                val totalLivePoints = managerData.summaryEventPoints + livePoints
                val hasLiveFixtures = if (bootstrap != null && liveGameweek != null) {
                    fixtures.any { it.started == true && it.finished == false && it.finishedProvisional == false }
                } else {
                    false
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    managerData = managerData,
                    managerHistory = historyResult.getOrNull(),
                    bootstrapData = bootstrap,
                    livePoints = livePoints,
                    totalLivePoints = totalLivePoints,
                    inPlay = inPlay,
                    toStart = toStart,
                    hasLiveFixtures = hasLiveFixtures
                )
            }
        }
    }
}
