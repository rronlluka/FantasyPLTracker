package com.fpl.tracker.data.api

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

data class BackendDiagnosticsState(
    val baseUrl: String = "",
    val lastError: String? = null,
    val lastSuccessAt: String? = null,
    val lastHealthCheckAt: String? = null,
    val lastHealthStatus: String? = null
)

object BackendDiagnostics {
    private val _state = MutableStateFlow(BackendDiagnosticsState())
    val state: StateFlow<BackendDiagnosticsState> = _state.asStateFlow()

    fun updateBaseUrl(baseUrl: String) {
        _state.value = _state.value.copy(baseUrl = baseUrl)
    }

    fun recordSuccess() {
        _state.value = _state.value.copy(
            lastSuccessAt = Instant.now().toString(),
            lastError = null
        )
    }

    fun recordFailure(message: String) {
        _state.value = _state.value.copy(lastError = message)
    }

    fun recordHealth(status: String) {
        _state.value = _state.value.copy(
            lastHealthCheckAt = Instant.now().toString(),
            lastHealthStatus = status
        )
    }
}
