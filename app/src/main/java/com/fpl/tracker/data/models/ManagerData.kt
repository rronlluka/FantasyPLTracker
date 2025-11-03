package com.fpl.tracker.data.models

import com.google.gson.annotations.SerializedName

data class ManagerData(
    val id: Int,
    @SerializedName("player_first_name") val playerFirstName: String,
    @SerializedName("player_last_name") val playerLastName: String,
    @SerializedName("player_region_name") val playerRegionName: String,
    @SerializedName("summary_overall_points") val summaryOverallPoints: Int,
    @SerializedName("summary_overall_rank") val summaryOverallRank: Int,
    @SerializedName("summary_event_points") val summaryEventPoints: Int,
    @SerializedName("summary_event_rank") val summaryEventRank: Int?,
    @SerializedName("current_event") val currentEvent: Int,
    val name: String,
    @SerializedName("favourite_team") val favouriteTeam: Int?,
    @SerializedName("started_event") val startedEvent: Int,
    val leagues: Leagues
)

data class Leagues(
    val classic: List<LeagueInfo>,
    val h2h: List<LeagueInfo>?
)

data class LeagueInfo(
    val id: Int,
    val name: String,
    @SerializedName("short_name") val shortName: String?,
    val created: String,
    @SerializedName("entry_rank") val entryRank: Int?,
    @SerializedName("entry_last_rank") val entryLastRank: Int?,
    @SerializedName("entry_can_leave") val entryCanLeave: Boolean,
    @SerializedName("entry_can_admin") val entryCanAdmin: Boolean,
    @SerializedName("entry_can_invite") val entryCanInvite: Boolean
)

