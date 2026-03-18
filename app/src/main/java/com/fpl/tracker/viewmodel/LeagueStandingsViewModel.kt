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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Represents one chip usage: which chip, which GW, numbered within its type (1st or 2nd use). */
data class UsedChip(
    val name: String,   // "bboost", "wildcard", "3xc", "freehit"
    val event: Int,
    val number: Int     // 1 = first use of this chip type, 2 = second use
)

data class ManagerLiveData(
    val managerId: Int,
    val inPlay: Int,
    val toStart: Int,
    val livePoints: Int = 0,
    val totalLivePoints: Int = 0,
    val captainName: String? = null,
    val viceCaptainName: String? = null,
    val activeChip: String? = null,
    val chipNumber: Int = 1,
    val allChips: List<UsedChip> = emptyList()  // Full chip history for the season
)

data class LeagueStandingsUiState(
    val isLoading: Boolean = false,
    val leagueStandings: LeagueStandings? = null,
    val currentEvent: Int = 1,
    val selectedGameweek: Int? = null,       // null = live/current view; non-null = historical
    val availableGameweeks: List<Int> = emptyList(),
    val managerLiveData: Map<Int, ManagerLiveData> = emptyMap(),
    val liveRankings: List<StandingEntry> = emptyList(),
    val hasLiveFixtures: Boolean = false,
    val showChipHistory: Boolean = false,
    val error: String? = null
)

class LeagueStandingsViewModel : ViewModel() {
    private val repository = FPLRepository(RetrofitInstance.api)

    private val _uiState = MutableStateFlow(LeagueStandingsUiState())
    val uiState: StateFlow<LeagueStandingsUiState> = _uiState.asStateFlow()

    // Remember the league ID so selectGameweek() can reload without needing it passed in again
    private var storedLeagueId: Long = 0L

    fun toggleChipHistory() {
        _uiState.value = _uiState.value.copy(showChipHistory = !_uiState.value.showChipHistory)
    }

    /** Called when the user picks a GW from the dropdown. */
    fun selectGameweek(gw: Int) {
        loadLeagueStandings(storedLeagueId, selectedEventId = gw)
    }

    /** Resets to the live/current-GW view. */
    fun clearGameweekSelection() {
        loadLeagueStandings(storedLeagueId, selectedEventId = null)
    }

    /** Converts a raw chip name from the FPL API into its numbered UsedChip object. */
    private fun buildUsedChips(chips: List<ChipUsage>?): List<UsedChip> {
        if (chips.isNullOrEmpty()) return emptyList()
        // Count usages per type to assign number 1 / 2
        val countByName = mutableMapOf<String, Int>()
        return chips.sortedBy { it.event }.map { chip ->
            val count = (countByName[chip.name] ?: 0) + 1
            countByName[chip.name] = count
            UsedChip(name = chip.name, event = chip.event, number = count)
        }
    }

    suspend fun calculateLeaguePlayerStats(
        playerId: Int,
        leagueStandings: LeagueStandings,
        currentEvent: Int,
        bootstrapData: BootstrapData
    ): LeaguePlayerStats = coroutineScope {
        val managersToCheck = leagueStandings.standings.results.take(50)

        // Fetch all managers' picks in parallel instead of one by one
        val picksResults = managersToCheck.map { standing ->
            async {
                try {
                    val picks = repository.getManagerPicks(standing.entry.toLong(), currentEvent).getOrNull()
                    Pair(standing, picks)
                } catch (e: Exception) {
                    Pair(standing, null)
                }
            }
        }.awaitAll()

        var startsCount = 0
        var benchCount = 0
        var captainCount = 0
        var viceCaptainCount = 0
        val captainedBy = mutableListOf<String>()
        val startedBy = mutableListOf<String>()
        val benchedBy = mutableListOf<String>()

        picksResults.forEach { (standing, picks) ->
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
        }

        val totalManagers = managersToCheck.size
        val startsPercentage = if (totalManagers > 0) (startsCount.toDouble() / totalManagers) * 100 else 0.0
        val ownedPercentage = if (totalManagers > 0) ((startsCount + benchCount).toDouble() / totalManagers) * 100 else 0.0

        LeaguePlayerStats(
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
    
    fun loadLeagueStandings(leagueId: Long, selectedEventId: Int? = null) {
        storedLeagueId = leagueId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Load bootstrap to get current event
            val bootstrapResult = repository.getBootstrapData()
            val currentEvent = bootstrapResult.getOrNull()?.events?.find { it.isCurrent }?.id ?: 1
            val availableGameweeks = (1..currentEvent).toList()

            // Clamp selected GW to valid range
            val clampedGw = selectedEventId?.coerceIn(1, currentEvent)

            val result = repository.getLeagueStandings(leagueId, eventId = clampedGw)
            
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Unknown error"
                )
            } else {
                val leagueStandings = result.getOrNull()!!
                val isHistoricalView = clampedGw != null && clampedGw != currentEvent

                // ── Historical GW path ────────────────────────────────────────────────────
                // The API standings endpoint doesn't return per-GW correct pts/totals.
                // We fetch every manager's history in parallel to get the exact
                // `points` (GW pts that week) and `totalPoints` (cumulative up to that GW),
                // then re-rank so the table is fully accurate for the chosen GW.
                if (isHistoricalView) {
                    val targetGw = clampedGw!!

                    // Fetch history for all managers in the league concurrently
                    val historicalEntries = leagueStandings.standings.results
                        .map { standing ->
                            async {
                                try {
                                    val hist = repository.getManagerHistory(standing.entry.toLong()).getOrNull()
                                    val gwData = hist?.current?.find { it.event == targetGw }
                                    // Preserve existing allChips from already-loaded managerLiveData if available
                                    val existingChips = _uiState.value.managerLiveData[standing.entry]?.allChips
                                        ?: buildUsedChips(hist?.chips)
                                    Triple(standing, gwData, existingChips)
                                } catch (e: Exception) {
                                    Triple(standing, null, emptyList<UsedChip>())
                                }
                            }
                        }.awaitAll()

                    // Build corrected StandingEntry list sorted by cumulative total at that GW
                    var rank = 0
                    val correctedRankings = historicalEntries
                        .sortedByDescending { (_, gwData, _) -> gwData?.totalPoints ?: 0 }
                        .mapIndexed { index, (standing, gwData, chips) ->
                            rank = index + 1
                            // Overwrite eventTotal and total with historically correct values
                            val correctedEntry = standing.copy(
                                eventTotal = gwData?.points ?: 0,
                                total      = gwData?.totalPoints ?: 0,
                                rank       = rank,
                                lastRank   = rank   // no prev-rank context for historical, keep neutral
                            )
                            // Merge chip history into existing managerLiveData (or create minimal entry)
                            val updatedLiveData = (_uiState.value.managerLiveData[standing.entry]
                                ?: ManagerLiveData(standing.entry, 0, 0)).copy(allChips = chips)
                            Pair(correctedEntry, updatedLiveData)
                        }

                    val correctedStandings = correctedRankings.map { it.first }
                    val updatedLiveDataMap = _uiState.value.managerLiveData.toMutableMap().also { map ->
                        correctedRankings.forEach { (_, liveData) -> map[liveData.managerId] = liveData }
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading          = false,
                        leagueStandings    = leagueStandings,
                        currentEvent       = currentEvent,
                        selectedGameweek   = targetGw,
                        availableGameweeks = availableGameweeks,
                        managerLiveData    = updatedLiveDataMap,
                        liveRankings       = correctedStandings,  // Screen uses this list when non-empty
                        hasLiveFixtures    = false,
                        showChipHistory    = _uiState.value.showChipHistory
                    )
                    return@launch
                }

                // ── Live / current-GW path below ──────────────────────────────────────────

                // Load fixtures for current gameweek
                val fixturesResult = repository.getFixturesByEvent(currentEvent)
                val fixtures = fixturesResult.getOrNull() ?: emptyList()
                
                // Log fixture statuses for debugging
                Log.d("LeagueStandings", "=== FIXTURE STATUS DEBUG ===")
                fixtures.forEach { fixture ->
                    val homeTeam = bootstrapResult.getOrNull()?.teams?.find { it.id == fixture.teamH }
                    val awayTeam = bootstrapResult.getOrNull()?.teams?.find { it.id == fixture.teamA }
                    Log.d("LeagueStandings", "${homeTeam?.shortName} vs ${awayTeam?.shortName}: " +
                        "Started=${fixture.started}, Finished=${fixture.finished}, " +
                        "FinishedProvisional=${fixture.finishedProvisional}, Minutes=${fixture.minutes}")
                }
                Log.d("LeagueStandings", "===========================")
                
                // Load live gameweek data
                val liveGameweekResult = repository.getLiveGameweek(currentEvent)
                val liveGameweek = liveGameweekResult.getOrNull()
                
                // Calculate provisional bonus ONCE per fixture (for ALL players)
                val globalProvisionalBonus = mutableMapOf<Int, Int>()
                
                // Truly live fixtures (for visual "LIVE" indicator)
                val liveFixtures = fixtures.filter { 
                    it.started == true && it.finished == false && it.finishedProvisional == false
                }
                
                // All fixtures with live data (started but not finished by FPL)
                // API will tell us if bonus is ready via the bonus field
                val fixturesNeedingBonus = fixtures.filter {
                    it.started == true && it.finished == false
                }
                
                Log.d("LeagueStandings", "Truly live fixtures: ${liveFixtures.size}")
                Log.d("LeagueStandings", "Fixtures with live data: ${fixturesNeedingBonus.size}")
                fixturesNeedingBonus.forEach { fixture ->
                    val homeTeam = bootstrapResult.getOrNull()?.teams?.find { it.id == fixture.teamH }
                    val awayTeam = bootstrapResult.getOrNull()?.teams?.find { it.id == fixture.teamA }
                    val status = if (fixture.finishedProvisional == true) "PROCESSING" else "LIVE"
                    Log.d("LeagueStandings", "  - [$status] ${homeTeam?.shortName} vs ${awayTeam?.shortName}")
                }
                
                if (liveGameweek != null) {
                    Log.d("BonusCalc", "=================================================")
                    Log.d("BonusCalc", "CALCULATING BONUS FOR ${fixturesNeedingBonus.size} FIXTURES")
                    Log.d("BonusCalc", "=================================================")
                    
                    fixturesNeedingBonus.forEach { fixture ->
                        val homeTeam = bootstrapResult.getOrNull()?.teams?.find { it.id == fixture.teamH }
                        val awayTeam = bootstrapResult.getOrNull()?.teams?.find { it.id == fixture.teamA }
                        val status = if (fixture.finishedProvisional == true) "PROCESSING" else "LIVE"
                        
                        Log.d("BonusCalc", "")
                        Log.d("BonusCalc", "[$status] FIXTURE ${fixture.id}: ${homeTeam?.shortName ?: fixture.teamH} vs ${awayTeam?.shortName ?: fixture.teamA}")
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
                
                // Build a chip-use-count map from ALL managers' history so we can label BB1/BB2 etc.
                // We do this lazily per manager below using their picks activeChip field and history.
                // Since we don't load history for all managers here, we'll detect chip number
                // by fetching history only when we see an active chip (to keep it performant).

                val deferredResults = managersToCheck.map { standing ->
                    async {
                        try {
                            val picksResult = repository.getManagerPicks(standing.entry.toLong(), currentEvent)
                            val picks = picksResult.getOrNull()

                            if (picks != null) {
                                val bootstrap = bootstrapResult.getOrNull()
                                val activeChip = picks.activeChip  // e.g. "bboost", "3xc", "wildcard", "freehit"
                                val isBenchBoost = activeChip == "bboost"

                                // Always fetch history so we can show full chip history in the toggle view
                                var chipNumber = 1
                                var allChips = emptyList<UsedChip>()
                                try {
                                    val historyResult = repository.getManagerHistory(standing.entry.toLong())
                                    val history = historyResult.getOrNull()
                                    if (history != null) {
                                        allChips = buildUsedChips(history.chips)
                                        if (activeChip != null) {
                                            val usages = history.chips?.count { it.name == activeChip } ?: 1
                                            chipNumber = usages
                                        }
                                    }
                                } catch (e: Exception) {
                                    chipNumber = 1
                                }

                                if (bootstrap != null) {
                                    // For Bench Boost: all 15 players score. Use all picks.
                                    // For all others: use effective playing XI (with auto-subs).
                                    val scoringPlayerIds: List<Int> = if (isBenchBoost) {
                                        // All 15 picks score (bench included)
                                        picks.picks.map { it.element }
                                    } else {
                                        AutoSubstitutionHelper.getEffectivePlayingXI(
                                            picks = picks.picks,
                                            players = bootstrap.elements,
                                            fixtures = fixtures
                                        )
                                    }

                                    // In/ToStart counts use bench boost all-15 too
                                    val (inPlay, toStart) = if (isBenchBoost) {
                                        AutoSubstitutionHelper.countPlayersStatusForList(
                                            playerIds = scoringPlayerIds,
                                            players = bootstrap.elements,
                                            fixtures = fixtures
                                        )
                                    } else {
                                        AutoSubstitutionHelper.countPlayersStatus(
                                            picks = picks.picks,
                                            players = bootstrap.elements,
                                            fixtures = fixtures
                                        )
                                    }

                                    // Calculate FULL scoring player points
                                    var calculatedGwPoints = 0

                                    // Find captain and vice captain
                                    val captainPick = picks.picks.find { it.isCaptain }
                                    val viceCaptainPick = picks.picks.find { it.isViceCaptain }
                                    val captainPlayer = captainPick?.let { pick ->
                                        bootstrap.elements.find { it.id == pick.element }
                                    }
                                    val viceCaptainPlayer = viceCaptainPick?.let { pick ->
                                        bootstrap.elements.find { it.id == pick.element }
                                    }

                                    scoringPlayerIds.forEach { playerId ->
                                        val player = bootstrap.elements.find { it.id == playerId }
                                        if (player != null) {
                                            val fixture = fixtures.find {
                                                it.teamH == player.team || it.teamA == player.team
                                            }

                                            val pick = picks.picks.find { it.element == playerId }
                                            // multiplier from API already handles TC (3), normal captain (2), bench boost bench players (1)
                                            val multiplier = pick?.multiplier ?: 1

                                            // Use live data if game started but not finished by FPL
                                            val shouldUseLiveData = fixture?.started == true && fixture.finished == false

                                            var playerPoints = 0

                                            if (shouldUseLiveData) {
                                                val liveStats = liveGameweek?.elements?.find { it.id == playerId }

                                                if (liveStats != null) {
                                                    playerPoints = liveStats.stats.totalPoints

                                                    // Add provisional bonus ONLY if API hasn't provided bonus yet
                                                    val currentBonus = liveStats.stats.bonus
                                                    val provisionalBonusPoints = globalProvisionalBonus[playerId] ?: 0

                                                    if (currentBonus == 0 && provisionalBonusPoints > 0) {
                                                        playerPoints += provisionalBonusPoints
                                                        Log.d("LeagueStandings", "${standing.entryName} - ${player.webName}: +$provisionalBonusPoints provisional bonus")
                                                    }

                                                    Log.d("LeagueStandings", "${standing.entryName} - ${player.webName}: ${playerPoints}pts × ${multiplier} = ${playerPoints * multiplier} (live${if (isBenchBoost && (pick?.position ?: 0) > 11) "/BB" else ""})")
                                                } else {
                                                    playerPoints = player.eventPoints
                                                }
                                            } else {
                                                // Game not started or finished, use base points
                                                playerPoints = player.eventPoints
                                                Log.d("LeagueStandings", "${standing.entryName} - ${player.webName}: ${playerPoints}pts × ${multiplier} = ${playerPoints * multiplier} (base)")
                                            }

                                            calculatedGwPoints += playerPoints * multiplier
                                        }
                                    }

                                    // Use the higher of calculated vs API reported (in case API hasn't updated)
                                    val finalGwPoints = maxOf(calculatedGwPoints, standing.eventTotal)
                                    val livePointsDiff = calculatedGwPoints - standing.eventTotal

                                    Log.d("LeagueStandings", "Manager ${standing.entryName}: Calculated=$calculatedGwPoints, API=${standing.eventTotal}, Using=$finalGwPoints (diff=$livePointsDiff, chip=$activeChip)")

                                    val totalLivePoints = standing.total + livePointsDiff

                                    Log.d("LeagueStandings", "Manager ${standing.entryName}: InPlay=$inPlay, ToStart=$toStart, GWPts=$finalGwPoints, Total=$totalLivePoints, Chip=$activeChip#$chipNumber")
                                    ManagerLiveData(
                                        managerId = standing.entry,
                                        inPlay = inPlay,
                                        toStart = toStart,
                                        livePoints = livePointsDiff,
                                        totalLivePoints = totalLivePoints,
                                        captainName = captainPlayer?.webName,
                                        viceCaptainName = viceCaptainPlayer?.webName,
                                        activeChip = activeChip,
                                        chipNumber = chipNumber,
                                        allChips = allChips
                                    )
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
                    selectedGameweek = null,          // Live view = no selection
                    availableGameweeks = availableGameweeks,
                    managerLiveData = managerLiveDataMap,
                    liveRankings = liveRankings,
                    hasLiveFixtures = liveFixtures.isNotEmpty()
                )
            }
        }
    }
}
