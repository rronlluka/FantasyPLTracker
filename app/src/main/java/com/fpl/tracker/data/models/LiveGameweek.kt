package com.fpl.tracker.data.models

import com.google.gson.annotations.SerializedName

data class LiveGameweek(
    val elements: List<LiveElement>
)

data class LiveElement(
    val id: Int,
    val stats: LiveStats,
    val explain: List<Explain>?
)

data class LiveStats(
    val minutes: Int,
    @SerializedName("goals_scored") val goalsScored: Int,
    val assists: Int,
    @SerializedName("clean_sheets") val cleanSheets: Int,
    @SerializedName("goals_conceded") val goalsConceded: Int,
    @SerializedName("own_goals") val ownGoals: Int,
    @SerializedName("penalties_saved") val penaltiesSaved: Int,
    @SerializedName("penalties_missed") val penaltiesMissed: Int,
    @SerializedName("yellow_cards") val yellowCards: Int,
    @SerializedName("red_cards") val redCards: Int,
    val saves: Int,
    val bonus: Int,
    val bps: Int,
    val influence: String,
    val creativity: String,
    val threat: String,
    @SerializedName("ict_index") val ictIndex: String,
    @SerializedName("total_points") val totalPoints: Int,
    @SerializedName("in_dreamteam") val inDreamteam: Boolean
)

data class Explain(
    val fixture: Int,
    val stats: List<StatDetail>
)

data class StatDetail(
    val identifier: String,
    val points: Int,
    val value: Int
)

