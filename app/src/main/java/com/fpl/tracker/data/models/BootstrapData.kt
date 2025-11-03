package com.fpl.tracker.data.models

import com.google.gson.annotations.SerializedName

data class BootstrapData(
    val events: List<Event>,
    val teams: List<Team>,
    val elements: List<Player>,
    @SerializedName("element_types") val elementTypes: List<ElementType>
)

data class Event(
    val id: Int,
    val name: String,
    @SerializedName("deadline_time") val deadlineTime: String,
    @SerializedName("average_entry_score") val averageEntryScore: Int?,
    @SerializedName("finished") val finished: Boolean,
    @SerializedName("data_checked") val dataChecked: Boolean,
    @SerializedName("highest_scoring_entry") val highestScoringEntry: Int?,
    @SerializedName("highest_score") val highestScore: Int?,
    @SerializedName("is_previous") val isPrevious: Boolean,
    @SerializedName("is_current") val isCurrent: Boolean,
    @SerializedName("is_next") val isNext: Boolean
)

data class Team(
    val id: Int,
    val name: String,
    @SerializedName("short_name") val shortName: String,
    val code: Int,
    val strength: Int
)

data class Player(
    val id: Int,
    @SerializedName("web_name") val webName: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("second_name") val secondName: String,
    val team: Int,
    @SerializedName("element_type") val elementType: Int,
    @SerializedName("now_cost") val nowCost: Int,
    @SerializedName("total_points") val totalPoints: Int,
    @SerializedName("event_points") val eventPoints: Int,
    @SerializedName("points_per_game") val pointsPerGame: String,
    val form: String,
    @SerializedName("selected_by_percent") val selectedByPercent: String,
    val status: String,
    val news: String?,
    @SerializedName("photo") val photo: String
)

data class ElementType(
    val id: Int,
    @SerializedName("plural_name") val pluralName: String,
    @SerializedName("plural_name_short") val pluralNameShort: String,
    @SerializedName("singular_name") val singularName: String,
    @SerializedName("singular_name_short") val singularNameShort: String
)

