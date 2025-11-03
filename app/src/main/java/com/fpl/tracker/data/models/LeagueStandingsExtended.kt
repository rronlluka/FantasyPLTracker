package com.fpl.tracker.data.models

import com.google.gson.annotations.SerializedName

// Extended standings entry with live match info
data class StandingEntryExtended(
    val standingEntry: StandingEntry,
    val playersInPlay: Int = 0,
    val playersToStart: Int = 0,
    val captainName: String = "",
    val monthlyPoints: Int = 0,
    val rankChange: RankChange = RankChange.NO_CHANGE
)

enum class RankChange {
    UP, DOWN, NO_CHANGE
}

