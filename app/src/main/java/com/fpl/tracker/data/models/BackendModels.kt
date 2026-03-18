package com.fpl.tracker.data.models

data class BackendHealthResponse(
    val ok: Boolean,
    val ts: String? = null,
    val uptimeSeconds: Long? = null,
    val adminEnabled: Boolean? = null
)
