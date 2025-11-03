package com.fpl.tracker.data.models

import com.google.gson.annotations.SerializedName

data class ManagerPicks(
    @SerializedName("active_chip") val activeChip: String?,
    @SerializedName("automatic_subs") val automaticSubs: List<AutomaticSub>?,
    @SerializedName("entry_history") val entryHistory: EntryHistory,
    val picks: List<Pick>
)

data class AutomaticSub(
    val entry: Int,
    val element_in: Int,
    val element_out: Int,
    val event: Int
)

data class EntryHistory(
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

data class Pick(
    val element: Int,
    val position: Int,
    @SerializedName("multiplier") val multiplier: Int,
    @SerializedName("is_captain") val isCaptain: Boolean,
    @SerializedName("is_vice_captain") val isViceCaptain: Boolean
)

