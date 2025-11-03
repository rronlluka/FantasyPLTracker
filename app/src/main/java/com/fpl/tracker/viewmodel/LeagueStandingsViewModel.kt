package com.fpl.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.fpl.tracker.data.api.RetrofitInstance
import com.fpl.tracker.data.models.*
import com.fpl.tracker.data.repository.FPLRepository
import com.fpl.tracker.utils.AutoSubstitutionHelper
import com.fpl.tracker.utils.BonusPointsCalculator
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ManagerLiveData(
    val managerId: Int,
    val inPlay: Int,
    val toStart: Int,
    val livePoints: Int = 0,  // Points from players currently playing
    val totalLivePoints: Int = 0  // GW points + live points
)

data class LeagueStandingsUiState(
    val isLoading: Boolean = false,
    val leagueStandings: LeagueStandings? = null,
    val currentEvent: Int = 1,
    val managerLiveData: Map<Int, ManagerLiveData> = emptyMap(),
    val liveRankings: List<StandingEntry> = emptyList(),
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
                
                // Load live gameweek data
                val liveGameweekResult = repository.getLiveGameweek(currentEvent)
                val liveGameweek = liveGameweekResult.getOrNull()
                
                // Calculate provisional bonus ONCE per fixture (for ALL players)
                val globalProvisionalBonus = mutableMapOf<Int, Int>()
                
                if (liveGameweek != null) {
                    Log.d("BonusCalc", "=================================================")
                    Log.d("BonusCalc", "LIVE GAMEWEEK DATA - Event $currentEvent")
                    Log.d("BonusCalc", "=================================================")
                    
                    fixtures.filter { it.started == true && it.finished == false }.forEach { fixture ->
                        val homeTeam = bootstrapResult.getOrNull()?.teams?.find { it.id == fixture.teamH }
                        val awayTeam = bootstrapResult.getOrNull()?.teams?.find { it.id == fixture.teamA }
                        
                        Log.d("BonusCalc", "")
                        Log.d("BonusCalc", "🔴 LIVE FIXTURE ${fixture.id}: ${homeTeam?.shortName ?: fixture.teamH} vs ${awayTeam?.shortName ?: fixture.teamA}")
                        Log.d("BonusCalc", "Minutes: ${fixture.minutes}")
                        
                        // Get ALL players in this fixture (both teams, all 22 players)
                        val fixturePlayersBps = liveGameweek.elements
                            .filter { liveEl ->
                                val player = bootstrapResult.getOrNull()?.elements?.find { it.id == liveEl.id }
                                player != null && (player.team == fixture.teamH || player.team == fixture.teamA)
                            }
                            .sortedByDescending { it.stats.bps }
                        
                        Log.d("BonusCalc", "ALL PLAYERS IN THIS MATCH (sorted by BPS):")
                        fixturePlayersBps.forEachIndexed { index, liveEl ->
                            val player = bootstrapResult.getOrNull()?.elements?.find { it.id == liveEl.id }
                            val team = bootstrapResult.getOrNull()?.teams?.find { it.id == player?.team }
                            Log.d("BonusCalc", "  ${index + 1}. ${player?.webName ?: "Unknown"} (${team?.shortName}): " +
                                "BPS=${liveEl.stats.bps}, " +
                                "Points=${liveEl.stats.totalPoints}, " +
                                "Bonus=${liveEl.stats.bonus}, " +
                                "Goals=${liveEl.stats.goalsScored}, " +
                                "Assists=${liveEl.stats.assists}, " +
                                "Minutes=${liveEl.stats.minutes}")
                        }
                        
                        // Calculate provisional bonus based on ALL players in this fixture
                        if (fixturePlayersBps.isNotEmpty()) {
                            val fixtureBonus = BonusPointsCalculator.calculateProvisionalBonus(
                                fixturePlayersBps,
                                fixture.id
                            )
                            
                            // Add to global map
                            globalProvisionalBonus.putAll(fixtureBonus)
                            
                            if (fixtureBonus.isNotEmpty()) {
                                Log.d("BonusCalc", "PROVISIONAL BONUS AWARDS (from all ${fixturePlayersBps.size} players):")
                                fixtureBonus.forEach { (playerId, bonus) ->
                                    val player = bootstrapResult.getOrNull()?.elements?.find { it.id == playerId }
                                    val bps = fixturePlayersBps.find { it.id == playerId }?.stats?.bps
                                    Log.d("BonusCalc", "  ⭐ ${player?.webName ?: playerId} (BPS=$bps): +$bonus bonus points")
                                }
                            }
                        }
                    }
                    
                    Log.d("BonusCalc", "")
                    Log.d("BonusCalc", "=================================================")
                }
                
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
                                
                                if (bootstrap != null) {
                                    // Use auto-substitution logic to count only effective playing XI
                                    val (inPlay, toStart) = AutoSubstitutionHelper.countPlayersStatus(
                                        picks = picks.picks,
                                        players = bootstrap.elements,
                                        fixtures = fixtures
                                    )
                                    
                                    // Calculate live points from players currently playing
                                    val effectiveXI = AutoSubstitutionHelper.getEffectivePlayingXI(
                                        picks = picks.picks,
                                        players = bootstrap.elements,
                                        fixtures = fixtures
                                    )
                                    
                                    // Calculate live points using the GLOBAL provisional bonus
                                    var livePoints = 0
                                    
                                    effectiveXI.forEach { playerId ->
                                        val player = bootstrap.elements.find { it.id == playerId }
                                        if (player != null) {
                                            val fixture = fixtures.find { 
                                                it.teamH == player.team || it.teamA == player.team 
                                            }
                                            
                                            // Only count points from live matches
                                            if (fixture?.started == true && fixture.finished == false) {
                                                val liveStats = liveGameweek?.elements?.find { it.id == playerId }
                                                
                                                if (liveStats != null) {
                                                    var playerPoints = liveStats.stats.totalPoints
                                                    
                                                    // Use GLOBAL provisional bonus (calculated from all 22 players)
                                                    val currentBonus = liveStats.stats.bonus
                                                    val provisionalBonusPoints = globalProvisionalBonus[playerId] ?: 0
                                                    
                                                    if (currentBonus == 0 && provisionalBonusPoints > 0) {
                                                        playerPoints += provisionalBonusPoints
                                                        Log.d("BonusCalc", "${standing.entryName} - ${player.webName}: Adding provisional bonus +$provisionalBonusPoints (Total: $playerPoints)")
                                                    }
                                                    
                                                    // Apply captain multiplier
                                                    val pick = picks.picks.find { it.element == playerId }
                                                    val multiplier = pick?.multiplier ?: 1
                                                    
                                                    val pointsWithMultiplier = playerPoints * multiplier
                                                    livePoints += pointsWithMultiplier
                                                    
                                                    Log.d("BonusCalc", "${standing.entryName} - ${player.webName}: $playerPoints pts × $multiplier = $pointsWithMultiplier")
                                                }
                                            }
                                        }
                                    }
                                    
                                    val totalLivePoints = standing.eventTotal + livePoints
                                    
                                    Log.d("LeagueStandings", "Manager ${standing.entryName}: InPlay=$inPlay, ToStart=$toStart, LivePts=$livePoints, Total=$totalLivePoints")
                                    ManagerLiveData(standing.entry, inPlay, toStart, livePoints, totalLivePoints)
                                } else {
                                    ManagerLiveData(standing.entry, 0, 0, 0, standing.eventTotal)
                                }
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
                
                // Calculate live rankings based on total live points
                val liveRankings = leagueStandings.standings.results.map { standing ->
                    val liveData = managerLiveDataMap[standing.entry]
                    val totalWithLive = standing.total + (liveData?.livePoints ?: 0)
                    
                    // Create updated standing entry with live total
                    standing.copy(total = totalWithLive)
                }.sortedByDescending { it.total }
                    .mapIndexed { index, standing ->
                        standing.copy(rank = index + 1)
                    }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    leagueStandings = leagueStandings,
                    currentEvent = currentEvent,
                    managerLiveData = managerLiveDataMap,
                    liveRankings = liveRankings
                )
            }
        }
    }
}

