package com.fpl.tracker.data.models

import com.google.gson.annotations.SerializedName

data class LeagueStandings(
    val league: LeagueDetails,
    val standings: StandingsData
)

data class LeagueDetails(
    val id: Int,
    val name: String,
    val created: String,
    @SerializedName("start_event") val startEvent: Int,
    @SerializedName("code_privacy") val codePrivacy: String
)

data class StandingsData(
    @SerializedName("has_next") val hasNext: Boolean,
    val page: Int,
    val results: List<StandingEntry>
)

data class StandingEntry(
    val id: Int,
    @SerializedName("event_total") val eventTotal: Int,
    @SerializedName("player_name") val playerName: String,
    val rank: Int,
    @SerializedName("last_rank") val lastRank: Int,
    @SerializedName("rank_sort") val rankSort: Int,
    val total: Int,
    val entry: Int,
    @SerializedName("entry_name") val entryName: String
)

