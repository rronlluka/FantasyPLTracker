package com.fpl.tracker.data.repository

import android.util.Log
import com.fpl.tracker.data.api.BackendDiagnostics
import com.fpl.tracker.data.api.BackendRetrofitInstance
import com.fpl.tracker.data.api.BackendApiService
import com.fpl.tracker.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FPLRepository
 *
 * All data flows through your backend (caching + pre-computed stats).
 * The Android app does not call the public FPL API directly.
 *
 * The key new method is [getLeaguePlayerStats] — this replaces the old
 * per-player N-API-calls approach with a single backend call that serves
 * pre-cached data for the whole league.
 */
class FPLRepository {
    private val TAG = "FPLRepository"

    private suspend fun <T> request(
        requestName: String,
        call: suspend BackendApiService.() -> retrofit2.Response<T>
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            val response = BackendRetrofitInstance.api.call()
            if (response.isSuccessful && response.body() != null) {
                BackendDiagnostics.recordSuccess()
                return@withContext Result.success(response.body()!!)
            }
            val message = "$requestName failed: HTTP ${response.code()}"
            BackendDiagnostics.recordFailure(message)
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Log.e(TAG, "Backend $requestName failed: ${e.message}")
            BackendDiagnostics.recordFailure("$requestName failed: ${e.message}")
            Result.failure(e)
        }
    }

    // ── Bootstrap ─────────────────────────────────────────────────────────────
    suspend fun getBootstrapData(): Result<BootstrapData> = request("bootstrap") { getBootstrapData() }

    // ── Manager data ──────────────────────────────────────────────────────────
    suspend fun getManagerData(managerId: Long): Result<ManagerData> = request("manager data") { getManagerData(managerId) }

    // ── Manager history ───────────────────────────────────────────────────────
    suspend fun getManagerHistory(managerId: Long): Result<ManagerHistory> = request("manager history") { getManagerHistory(managerId) }

    // ── Manager picks ─────────────────────────────────────────────────────────
    suspend fun getManagerPicks(managerId: Long, eventId: Int): Result<ManagerPicks> = request("manager picks") { getManagerPicks(managerId, eventId) }

    // ── Live gameweek ─────────────────────────────────────────────────────────
    suspend fun getLiveGameweek(eventId: Int): Result<LiveGameweek> = request("live gameweek") { getLiveGameweek(eventId) }

    // ── League standings ──────────────────────────────────────────────────────
    suspend fun getLeagueStandings(
        leagueId: Long,
        page: Int = 1,
        eventId: Int? = null
    ): Result<LeagueStandings> = request("league standings") { getLeagueStandings(leagueId, page, eventId) }

    // ── Fixtures ──────────────────────────────────────────────────────────────
    suspend fun getFixturesByEvent(eventId: Int): Result<List<Fixture>> = request("fixtures") { getFixturesByEvent(eventId) }

    // ── Player detail ─────────────────────────────────────────────────────────
    suspend fun getPlayerDetail(elementId: Int): Result<PlayerDetailResponse> = request("player detail") { getPlayerDetail(elementId) }

    // ── Manager transfers ─────────────────────────────────────────────────────
    suspend fun getManagerTransfers(managerId: Long): Result<List<ManagerTransfer>> = request("manager transfers") { getManagerTransfers(managerId) }

    // ── League player stats (NEW — replaces calculateLeaguePlayerStats) ───────
    //
    // Single backend call that returns pre-computed starts/bench/captain stats.
    // The backend fetches all managers' picks for the league ONCE per GW and
    // caches them — so this is always fast, regardless of league size.
    //
    suspend fun getLeaguePlayerStats(
        leagueId: Long,
        gameweek: Int,
        playerId: Int
    ): Result<LeaguePlayerStats> = request("league player stats") {
        getLeaguePlayerStats(leagueId, gameweek, playerId)
    }.map { it.toLeaguePlayerStats() }

    suspend fun getBackendHealth(): Result<BackendHealthResponse> {
        val result = request("backend health") { getHealth() }
        result.onSuccess {
            BackendDiagnostics.recordHealth(
                if (it.ok) "Healthy at ${it.ts ?: "unknown"}" else "Unhealthy response"
            )
        }.onFailure {
            BackendDiagnostics.recordHealth("Health check failed")
        }
        return result
    }
}
