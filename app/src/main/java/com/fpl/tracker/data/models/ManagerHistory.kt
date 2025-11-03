package com.fpl.tracker.data.models

import com.google.gson.annotations.SerializedName

data class ManagerHistory(
    val current: List<CurrentGameweek>,
    val past: List<PastSeason>?,
    val chips: List<ChipUsage>?
)

data class CurrentGameweek(
    val event: Int,
    val points: Int,
    @SerializedName("total_points") val totalPoints: Int,
    val rank: Int?,
    @SerializedName("rank_sort") val rankSort: Int?,
    @SerializedName("overall_rank") val overallRank: Int,
    val bank: Int,
    val value: Int,
    @SerializedName("event_transfers") val eventTransfers: Int,
    @SerializedName("event_transfers_cost") val eventTransfersCost: Int,
    @SerializedName("points_on_bench") val pointsOnBench: Int
)

data class PastSeason(
    @SerializedName("season_name") val seasonName: String,
    @SerializedName("total_points") val totalPoints: Int,
    val rank: Int
)

data class ChipUsage(
    val name: String,
    val time: String,
    val event: Int
)

