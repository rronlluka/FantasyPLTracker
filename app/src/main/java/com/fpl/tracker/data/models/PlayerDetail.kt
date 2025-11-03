package com.fpl.tracker.data.models

import com.google.gson.annotations.SerializedName

data class PlayerDetailResponse(
    val fixtures: List<PlayerFixture>,
    val history: List<PlayerHistory>,
    @SerializedName("history_past") val historyPast: List<PlayerHistoryPast>?
)

data class PlayerFixture(
    val id: Int,
    val code: Int,
    @SerializedName("team_h") val teamH: Int,
    @SerializedName("team_a") val teamA: Int,
    @SerializedName("team_h_score") val teamHScore: Int?,
    @SerializedName("team_a_score") val teamAScore: Int?,
    val event: Int?,
    val finished: Boolean,
    val minutes: Int,
    @SerializedName("provisional_start_time") val provisionalStartTime: Boolean,
    @SerializedName("kickoff_time") val kickoffTime: String?,
    @SerializedName("event_name") val eventName: String?,
    @SerializedName("is_home") val isHome: Boolean,
    val difficulty: Int
)

data class PlayerHistory(
    val element: Int,
    val fixture: Int,
    @SerializedName("opponent_team") val opponentTeam: Int,
    @SerializedName("total_points") val totalPoints: Int,
    @SerializedName("was_home") val wasHome: Boolean,
    @SerializedName("kickoff_time") val kickoffTime: String?,
    @SerializedName("team_h_score") val teamHScore: Int?,
    @SerializedName("team_a_score") val teamAScore: Int?,
    val round: Int,
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
    val value: Int,
    @SerializedName("transfers_balance") val transfersBalance: Int,
    val selected: Int,
    @SerializedName("transfers_in") val transfersIn: Int,
    @SerializedName("transfers_out") val transfersOut: Int
)

data class PlayerHistoryPast(
    @SerializedName("season_name") val seasonName: String,
    @SerializedName("element_code") val elementCode: Int,
    @SerializedName("start_cost") val startCost: Int,
    @SerializedName("end_cost") val endCost: Int,
    @SerializedName("total_points") val totalPoints: Int,
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
    @SerializedName("ict_index") val ictIndex: String
)

// League-specific player stats
data class LeaguePlayerStats(
    val playerId: Int,
    val startsCount: Int,
    val benchCount: Int,
    val captainCount: Int,
    val viceCaptainCount: Int,
    val startsPercentage: Double,
    val ownedPercentage: Double,
    val captainedBy: List<String>, // List of team names who captained
    val startedBy: List<String>,   // List of team names who started
    val benchedBy: List<String>    // List of team names who benched
)

