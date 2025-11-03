package com.fpl.tracker.utils

import com.fpl.tracker.data.models.*

object AutoSubstitutionHelper {
    
    // Formation constraints
    private const val MIN_GOALKEEPERS = 1
    private const val MIN_DEFENDERS = 3
    private const val MIN_MIDFIELDERS = 2
    private const val MIN_FORWARDS = 1
    
    /**
     * Determines the effective playing XI after automatic substitutions
     * Returns a list of player IDs that are actually playing (including auto-subs)
     */
    fun getEffectivePlayingXI(
        picks: List<Pick>,
        players: List<Player>,
        fixtures: List<Fixture>
    ): List<Int> {
        val startingXI = picks.filter { it.position <= 11 }.sortedBy { it.position }
        val bench = picks.filter { it.position > 11 }.sortedBy { it.position }
        
        val effectivePlayers = mutableListOf<Int>()
        val playersNotPlaying = mutableListOf<Pick>() // Starting XI players with 0 minutes
        
        // First, identify starting XI players who didn't play
        startingXI.forEach { pick ->
            val player = players.find { it.id == pick.element }
            if (player != null) {
                val fixture = fixtures.find { 
                    it.teamH == player.team || it.teamA == player.team 
                }
                
                // Player didn't play if their match finished and they have 0 minutes
                val didntPlay = fixture?.finished == true && 
                    (fixture.started == false || fixture.minutes == 0)
                
                if (didntPlay) {
                    playersNotPlaying.add(pick)
                } else {
                    effectivePlayers.add(pick.element)
                }
            } else {
                effectivePlayers.add(pick.element)
            }
        }
        
        // Now process automatic substitutions
        if (playersNotPlaying.isNotEmpty()) {
            val currentFormation = getCurrentFormation(
                effectivePlayers.map { id -> 
                    picks.find { it.element == id } to players.find { it.id == id }
                }.mapNotNull { if (it.first != null && it.second != null) it.first!! to it.second!! else null }
            )
            
            // Try to substitute each non-playing player
            playersNotPlaying.forEach { nonPlayingPick ->
                val nonPlayingPlayer = players.find { it.id == nonPlayingPick.element }
                if (nonPlayingPlayer != null) {
                    // Find first eligible bench player
                    val substitute = findEligibleSubstitute(
                        nonPlayingPlayer = nonPlayingPlayer,
                        currentFormation = currentFormation,
                        bench = bench,
                        players = players,
                        alreadySubbed = effectivePlayers
                    )
                    
                    if (substitute != null) {
                        effectivePlayers.add(substitute.element)
                        // Update formation
                        currentFormation[nonPlayingPlayer.elementType] = 
                            (currentFormation[nonPlayingPlayer.elementType] ?: 0) - 1
                        currentFormation[players.find { it.id == substitute.element }!!.elementType] = 
                            (currentFormation[players.find { it.id == substitute.element }!!.elementType] ?: 0) + 1
                    } else {
                        // No valid substitute, keep original player
                        effectivePlayers.add(nonPlayingPick.element)
                    }
                }
            }
        } else {
            // No one to substitute, return all starting XI
            return startingXI.map { it.element }
        }
        
        return effectivePlayers
    }
    
    /**
     * Counts how many players are in play and to start considering auto-subs
     */
    fun countPlayersStatus(
        picks: List<Pick>,
        players: List<Player>,
        fixtures: List<Fixture>
    ): Pair<Int, Int> { // Pair<InPlay, ToStart>
        val effectiveXI = getEffectivePlayingXI(picks, players, fixtures)
        
        var inPlay = 0
        var toStart = 0
        
        effectiveXI.forEach { playerId ->
            val player = players.find { it.id == playerId }
            if (player != null) {
                val fixture = fixtures.find {
                    it.teamH == player.team || it.teamA == player.team
                }
                
                val isFinished = fixture?.finished == true || fixture?.finishedProvisional == true

                when {
                    fixture?.started == true && !isFinished -> inPlay++
                    fixture?.started == false -> toStart++
                    // Finished games don't count
                }
            }
        }
        
        return Pair(inPlay, toStart)
    }
    
    private fun getCurrentFormation(
        playersWithPicks: List<Pair<Pick, Player>>
    ): MutableMap<Int, Int> {
        val formation = mutableMapOf<Int, Int>()
        playersWithPicks.forEach { (_, player) ->
            formation[player.elementType] = (formation[player.elementType] ?: 0) + 1
        }
        return formation
    }
    
    private fun findEligibleSubstitute(
        nonPlayingPlayer: Player,
        currentFormation: MutableMap<Int, Int>,
        bench: List<Pick>,
        players: List<Player>,
        alreadySubbed: List<Int>
    ): Pick? {
        // Can only sub out if we still meet minimum requirements
        val currentCount = currentFormation[nonPlayingPlayer.elementType] ?: 0
        val minRequired = when (nonPlayingPlayer.elementType) {
            1 -> MIN_GOALKEEPERS
            2 -> MIN_DEFENDERS
            3 -> MIN_MIDFIELDERS
            4 -> MIN_FORWARDS
            else -> 0
        }
        
        // Can't substitute if removing this player breaks formation rules
        if (currentCount <= minRequired) {
            return null
        }
        
        // Find first bench player not already subbed in
        bench.forEach { benchPick ->
            if (!alreadySubbed.contains(benchPick.element)) {
                val benchPlayer = players.find { it.id == benchPick.element }
                if (benchPlayer != null) {
                    // Check if this player can fit in formation
                    // Any outfield player can replace any outfield player as long as minimums are met
                    // Goalkeeper can only replace goalkeeper
                    if (nonPlayingPlayer.elementType == 1) {
                        // Only GK can replace GK
                        if (benchPlayer.elementType == 1) {
                            return benchPick
                        }
                    } else {
                        // Outfield player
                        if (benchPlayer.elementType != 1) { // Not a goalkeeper
                            // Check if formation would still be valid
                            val newFormation = currentFormation.toMutableMap()
                            newFormation[nonPlayingPlayer.elementType] = currentCount - 1
                            newFormation[benchPlayer.elementType] = (newFormation[benchPlayer.elementType] ?: 0) + 1
                            
                            if (isValidFormation(newFormation)) {
                                return benchPick
                            }
                        }
                    }
                }
            }
        }
        
        return null
    }
    
    private fun isValidFormation(formation: Map<Int, Int>): Boolean {
        val gk = formation[1] ?: 0
        val def = formation[2] ?: 0
        val mid = formation[3] ?: 0
        val fwd = formation[4] ?: 0
        
        return gk >= MIN_GOALKEEPERS &&
               def >= MIN_DEFENDERS &&
               mid >= MIN_MIDFIELDERS &&
               fwd >= MIN_FORWARDS &&
               (gk + def + mid + fwd) == 11
    }
}

