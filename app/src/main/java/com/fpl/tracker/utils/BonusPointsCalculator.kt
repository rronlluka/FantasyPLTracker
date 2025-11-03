package com.fpl.tracker.utils

import com.fpl.tracker.data.models.LiveElement

object BonusPointsCalculator {
    
    /**
     * Calculate provisional bonus points based on BPS rankings for a single fixture
     * Only for players in live matches (not finished)
     */
    fun calculateProvisionalBonus(
        liveElements: List<LiveElement>,
        fixtureId: Int
    ): Map<Int, Int> {
        if (liveElements.isEmpty()) return emptyMap()
        
        val bonusMap = mutableMapOf<Int, Int>()
        
        // Sort players by BPS descending
        val sortedByBps = liveElements
            .filter { it.stats.bps > 0 }
            .sortedByDescending { it.stats.bps }
        
        if (sortedByBps.isEmpty()) return emptyMap()
        
        // Group by BPS value to handle ties
        val bpsGroups = sortedByBps.groupBy { it.stats.bps }
        val sortedBpsValues = bpsGroups.keys.sortedDescending()
        
        when {
            sortedBpsValues.isEmpty() -> return emptyMap()
            
            // Case 1: Only one BPS value (all tied for first)
            sortedBpsValues.size == 1 -> {
                val players = bpsGroups[sortedBpsValues[0]]!!
                when (players.size) {
                    1 -> bonusMap[players[0].id] = 3
                    2 -> {
                        bonusMap[players[0].id] = 3
                        bonusMap[players[1].id] = 3
                    }
                    else -> {
                        // 3+ tied for first, all get 3
                        players.take(3).forEach { bonusMap[it.id] = 3 }
                    }
                }
            }
            
            // Case 2: Two different BPS values
            sortedBpsValues.size == 2 -> {
                val firstPlace = bpsGroups[sortedBpsValues[0]]!!
                val secondPlace = bpsGroups[sortedBpsValues[1]]!!
                
                when (firstPlace.size) {
                    1 -> {
                        // One player in first
                        bonusMap[firstPlace[0].id] = 3
                        
                        when (secondPlace.size) {
                            1 -> bonusMap[secondPlace[0].id] = 2
                            else -> {
                                // Multiple tied for second, both get 2
                                secondPlace.take(2).forEach { bonusMap[it.id] = 2 }
                            }
                        }
                    }
                    2 -> {
                        // Two tied for first, both get 3
                        bonusMap[firstPlace[0].id] = 3
                        bonusMap[firstPlace[1].id] = 3
                        // Second place gets 1
                        bonusMap[secondPlace[0].id] = 1
                    }
                    else -> {
                        // 3+ tied for first, top 3 get 3 points each
                        firstPlace.take(3).forEach { bonusMap[it.id] = 3 }
                    }
                }
            }
            
            // Case 3: Three or more different BPS values
            else -> {
                val firstPlace = bpsGroups[sortedBpsValues[0]]!!
                val secondPlace = bpsGroups[sortedBpsValues[1]]!!
                val thirdPlace = if (sortedBpsValues.size >= 3) bpsGroups[sortedBpsValues[2]]!! else emptyList()
                
                when (firstPlace.size) {
                    1 -> {
                        // One in first place
                        bonusMap[firstPlace[0].id] = 3
                        
                        when (secondPlace.size) {
                            1 -> {
                                // One in second place
                                bonusMap[secondPlace[0].id] = 2
                                // One or more in third
                                if (thirdPlace.isNotEmpty()) {
                                    bonusMap[thirdPlace[0].id] = 1
                                }
                            }
                            2 -> {
                                // Two tied for second, both get 2, third gets 0
                                bonusMap[secondPlace[0].id] = 2
                                bonusMap[secondPlace[1].id] = 2
                            }
                            else -> {
                                // 3+ tied for second, top 2 get 2 points each
                                secondPlace.take(2).forEach { bonusMap[it.id] = 2 }
                            }
                        }
                    }
                    2 -> {
                        // Two tied for first, both get 3
                        bonusMap[firstPlace[0].id] = 3
                        bonusMap[firstPlace[1].id] = 3
                        // Second place gets 1
                        bonusMap[secondPlace[0].id] = 1
                    }
                    else -> {
                        // 3+ tied for first, all get 3
                        firstPlace.take(3).forEach { bonusMap[it.id] = 3 }
                    }
                }
            }
        }
        
        return bonusMap
    }
    
    /**
     * Calculate provisional bonus for all live fixtures
     */
    fun calculateAllProvisionalBonus(
        liveElements: List<LiveElement>
    ): Map<Int, Int> {
        // For simplicity, calculate bonus across all live matches
        // In reality, bonus is per-fixture, but we'll do a simplified version
        val allBonus = mutableMapOf<Int, Int>()
        
        val playersByBps = liveElements
            .filter { it.stats.bps > 0 && it.stats.totalPoints >= 0 }
            .sortedByDescending { it.stats.bps }
        
        if (playersByBps.isEmpty()) return emptyMap()
        
        val bpsGroups = playersByBps.groupBy { it.stats.bps }
        val sortedBps = bpsGroups.keys.sortedDescending()
        
        // Assign bonus based on BPS ranking
        var currentRank = 0
        var bonusAwarded = 0
        
        for (bps in sortedBps) {
            val playersWithBps = bpsGroups[bps]!!
            
            for (player in playersWithBps) {
                currentRank++
                
                val bonusPoints = when {
                    bonusAwarded < 1 -> {
                        // Top BPS get 3 points
                        if (currentRank <= playersWithBps.size && sortedBps.indexOf(bps) == 0) {
                            bonusAwarded++
                            3
                        } else 0
                    }
                    bonusAwarded < 2 -> {
                        // Second tier gets 2 points
                        if (sortedBps.indexOf(bps) == 1 || (sortedBps.indexOf(bps) == 0 && playersWithBps.size >= 2)) {
                            bonusAwarded++
                            if (sortedBps.indexOf(bps) == 0 && playersWithBps.size == 2) 3 else 2
                        } else 0
                    }
                    bonusAwarded < 3 -> {
                        // Third tier gets 1 point
                        1.also { bonusAwarded++ }
                    }
                    else -> 0
                }
                
                if (bonusPoints > 0) {
                    allBonus[player.id] = bonusPoints
                }
            }
        }
        
        return allBonus
    }
}

