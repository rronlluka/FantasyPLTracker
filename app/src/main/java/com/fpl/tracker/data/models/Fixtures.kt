package com.fpl.tracker.data.models

import com.google.gson.annotations.SerializedName

data class Fixture(
    val id: Int,
    val code: Int,
    val event: Int?,
    @SerializedName("finished") val finished: Boolean,
    @SerializedName("finished_provisional") val finishedProvisional: Boolean,
    @SerializedName("kickoff_time") val kickoffTime: String?,
    val minutes: Int,
    @SerializedName("provisional_start_time") val provisionalStartTime: Boolean,
    val started: Boolean?,
    @SerializedName("team_a") val teamA: Int,
    @SerializedName("team_a_score") val teamAScore: Int?,
    @SerializedName("team_h") val teamH: Int,
    @SerializedName("team_h_score") val teamHScore: Int?,
    @SerializedName("team_a_difficulty") val teamADifficulty: Int,
    @SerializedName("team_h_difficulty") val teamHDifficulty: Int,
    val stats: List<FixtureStat>?
)

data class FixtureStat(
    val identifier: String,
    val a: List<FixtureStatValue>,
    val h: List<FixtureStatValue>
)

data class FixtureStatValue(
    val value: Int,
    val element: Int
)

