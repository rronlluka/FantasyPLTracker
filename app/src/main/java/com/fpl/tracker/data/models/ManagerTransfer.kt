package com.fpl.tracker.data.models

import com.google.gson.annotations.SerializedName

data class ManagerTransfer(
    val entry: Int,
    val event: Int,
    @SerializedName("element_in") val elementIn: Int,
    @SerializedName("element_in_cost") val elementInCost: Int,
    @SerializedName("element_out") val elementOut: Int,
    @SerializedName("element_out_cost") val elementOutCost: Int,
    val time: String
)
