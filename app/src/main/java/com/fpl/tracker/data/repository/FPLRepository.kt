package com.fpl.tracker.data.repository

import android.util.Log
import com.fpl.tracker.data.api.BackendRetrofitInstance
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
    private val backendApi = BackendRetrofitInstance.api
    private val TAG = "FPLRepository"

    // ── Bootstrap ─────────────────────────────────────────────────────────────
    suspend fun getBootstrapData(): Result<BootstrapData> = withContext(Dispatchers.IO) {
        try {
            val response = backendApi.getBootstrapData()
            if (response.isSuccessful && response.body() != null) {
                return@withContext Result.success(response.body()!!)
            }
            Result.failure(Exception("Failed to fetch bootstrap: ${response.code()}"))
        } catch (e: Exception) {
            Log.e(TAG, "Backend bootstrap failed: ${e.message}")
            Result.failure(e)
        }
    }

    // ── Manager data ──────────────────────────────────────────────────────────
    suspend fun getManagerData(managerId: Long): Result<ManagerData> = withContext(Dispatchers.IO) {
        try {
            val response = backendApi.getManagerData(managerId)
            if (response.isSuccessful && response.body() != null)
                return@withContext Result.success(response.body()!!)
            Result.failure(Exception("Failed to fetch manager data: ${response.code()}"))
        } catch (e: Exception) {
            Log.e(TAG, "Backend manager data failed: ${e.message}")
            Result.failure(e)
        }
    }

    // ── Manager history ───────────────────────────────────────────────────────
    suspend fun getManagerHistory(managerId: Long): Result<ManagerHistory> = withContext(Dispatchers.IO) {
        try {
            val response = backendApi.getManagerHistory(managerId)
            if (response.isSuccessful && response.body() != null)
                return@withContext Result.success(response.body()!!)
            Result.failure(Exception("Failed to fetch manager history: ${response.code()}"))
        } catch (e: Exception) {
            Log.e(TAG, "Backend manager history failed: ${e.message}")
            Result.failure(e)
        }
    }

    // ── Manager picks ─────────────────────────────────────────────────────────
    suspend fun getManagerPicks(managerId: Long, eventId: Int): Result<ManagerPicks> = withContext(Dispatchers.IO) {
        try {
            val response = backendApi.getManagerPicks(managerId, eventId)
            if (response.isSuccessful && response.body() != null)
                return@withContext Result.success(response.body()!!)
            Result.failure(Exception("Failed to fetch manager picks: ${response.code()}"))
        } catch (e: Exception) {
            Log.e(TAG, "Backend manager picks failed: ${e.message}")
            Result.failure(e)
        }
    }

    // ── Live gameweek ─────────────────────────────────────────────────────────
    suspend fun getLiveGameweek(eventId: Int): Result<LiveGameweek> = withContext(Dispatchers.IO) {
        try {
            val response = backendApi.getLiveGameweek(eventId)
            if (response.isSuccessful && response.body() != null)
                return@withContext Result.success(response.body()!!)
            Result.failure(Exception("Failed to fetch live gameweek: ${response.code()}"))
        } catch (e: Exception) {
            Log.e(TAG, "Backend live gameweek failed: ${e.message}")
            Result.failure(e)
        }
    }

    // ── League standings ──────────────────────────────────────────────────────
    suspend fun getLeagueStandings(
        leagueId: Long,
        page: Int = 1,
        eventId: Int? = null
    ): Result<LeagueStandings> = withContext(Dispatchers.IO) {
        try {
            val response = backendApi.getLeagueStandings(leagueId, page, eventId)
            if (response.isSuccessful && response.body() != null)
                return@withContext Result.success(response.body()!!)
            Result.failure(Exception("Failed to fetch league standings: ${response.code()}"))
        } catch (e: Exception) {
            Log.e(TAG, "Backend league standings failed: ${e.message}")
            Result.failure(e)
        }
    }

    // ── Fixtures ──────────────────────────────────────────────────────────────
    suspend fun getFixturesByEvent(eventId: Int): Result<List<Fixture>> = withContext(Dispatchers.IO) {
        try {
            val response = backendApi.getFixturesByEvent(eventId)
            if (response.isSuccessful && response.body() != null)
                return@withContext Result.success(response.body()!!)
            Result.failure(Exception("Failed to fetch fixtures: ${response.code()}"))
        } catch (e: Exception) {
            Log.e(TAG, "Backend fixtures failed: ${e.message}")
            Result.failure(e)
        }
    }

    // ── Player detail ─────────────────────────────────────────────────────────
    suspend fun getPlayerDetail(elementId: Int): Result<PlayerDetailResponse> = withContext(Dispatchers.IO) {
        try {
            val response = backendApi.getPlayerDetail(elementId)
            if (response.isSuccessful && response.body() != null)
                return@withContext Result.success(response.body()!!)
            Result.failure(Exception("Failed to fetch player detail: ${response.code()}"))
        } catch (e: Exception) {
            Log.e(TAG, "Backend player detail failed: ${e.message}")
            Result.failure(e)
        }
    }

    // ── Manager transfers ─────────────────────────────────────────────────────
    suspend fun getManagerTransfers(managerId: Long): Result<List<ManagerTransfer>> = withContext(Dispatchers.IO) {
        try {
            val response = backendApi.getManagerTransfers(managerId)
            if (response.isSuccessful && response.body() != null)
                return@withContext Result.success(response.body()!!)
            Result.failure(Exception("Failed to fetch manager transfers: ${response.code()}"))
        } catch (e: Exception) {
            Log.e(TAG, "Backend manager transfers failed: ${e.message}")
            Result.failure(e)
        }
    }

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
    ): Result<LeaguePlayerStats> = withContext(Dispatchers.IO) {
        try {
            val response = backendApi.getLeaguePlayerStats(leagueId, gameweek, playerId)
            if (response.isSuccessful && response.body() != null) {
                return@withContext Result.success(response.body()!!.toLeaguePlayerStats())
            }
            Result.failure(Exception("Backend returned ${response.code()} for player stats"))
        } catch (e: Exception) {
            Log.e(TAG, "getLeaguePlayerStats failed: ${e.message}")
            Result.failure(e)
        }
    }
}
